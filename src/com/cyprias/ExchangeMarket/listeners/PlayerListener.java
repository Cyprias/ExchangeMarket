package com.cyprias.ExchangeMarket.listeners;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Signs;
import com.cyprias.ExchangeMarket.Breeze.BlockUtil;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.Breeze.PriceUtil;
import com.cyprias.ExchangeMarket.command.BuyCommand;
import com.cyprias.ExchangeMarket.command.Command;
import com.cyprias.ExchangeMarket.command.CommandManager;
import com.cyprias.ExchangeMarket.command.ConfirmCommand;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingOrder;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingTranasction;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;
import com.cyprias.ExchangeMarket.database.Parcel;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;

public class PlayerListener implements Listener {

	static public void unregisterEvents(JavaPlugin instance) {
		PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
		PlayerJoinEvent.getHandlerList().unregister(instance);
		PluginEnableEvent.getHandlerList().unregister(instance);
		PlayerInteractEvent.getHandlerList().unregister(instance);

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoinEvent(PlayerJoinEvent event) throws SQLException, IOException, InvalidConfigurationException {
		List<Parcel> packages = Plugin.database.getPackages(event.getPlayer());

		if (packages.size() <= 0)
			return;

		ChatUtils.notify(event.getPlayer(), String.format("§7You have §f%s §7packages to collect.", packages.size()));

		ItemStack stock;
		for (Parcel parcel : packages) {
			stock = Plugin.getItemStack(parcel.getItemId(), parcel.getItemDur(), parcel.getItemEnchant());
			ChatUtils.sendSpam(event.getPlayer(), String.format("§f%s§7x§f%s", Plugin.getItemName(stock), parcel.getAmount()));
		}

	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String msg = event.getMessage();
		String command = msg.split(" ")[0].replace("/", "");

		if (Plugin.aliases.containsKey(command.toLowerCase())) {
			event.setMessage(msg.replaceFirst("/" + command, "/" + Plugin.aliases.get(command.toLowerCase())));
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public static void onInteract(PlayerInteractEvent event) throws IllegalArgumentException, SQLException, IOException, InvalidConfigurationException {
		Player player = event.getPlayer();
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1)
			return;

		Block block = event.getClickedBlock();

		if (block == null) {
			return;
		}

		Action action = event.getAction();

		if (!BlockUtil.isSign(block) || player.getItemInHand().getType() == Material.SIGN) { // Blocking
																								// accidental
																								// sign
																								// edition
			return;
		}

		Sign sign = (Sign) block.getState();

		if (!Signs.isValid(sign)) {
			return;
		}

		if (!Plugin.checkPermission(player, Perm.USE_EXCHANGE_SIGN)) {
			return;
		}

		Logger.debug("action " + action);

		String[] line = sign.getLines();

		ItemStack stock = MaterialUtil.getItem(line[Signs.ITEM_LINE]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(player, "Unknown item: " + line[Signs.ITEM_LINE]);
			return;
		}

		int amount = Integer.parseInt(line[Signs.QUANTITY_LINE]);
		// Logger.debug("amount " + amount);

		String formattedPrice = Signs.formatPriceLine(line[Signs.PRICE_LINE]);

		int dplaces = Config.getInt("properties.price-decmial-places");

		// ////////////////////////////////////////////////////
		if (action == RIGHT_CLICK_BLOCK) {
			if (Econ.getBalance(player.getName()) <= 0) {
				ChatUtils.send(player, String.format("§7You have no money in your account."));
				return;
			}

			double buyPrice = PriceUtil.getBuyPrice(formattedPrice);
			Logger.debug("buyPrice " + buyPrice);
			if (buyPrice <= 0) {
				ChatUtils.send(player, "§7That exchange does not have a buy price.");
				return;
			}

			List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER);

			Order o;

			if (!Config.getBoolean("properties.trade-to-yourself"))
				for (int i = (orders.size() - 1); i >= 0; i--) {
					o = orders.get(i);
					if (player.getName().equalsIgnoreCase(o.getPlayer()))
						orders.remove(o);
				}

			if (orders.size() <= 0) {
				ChatUtils.send(player, String.format("§7There are no sell orders for §f%s§7.", Plugin.getItemName(stock)));
				return;
			}

			int playerCanFit = Plugin.getFitAmount(stock, 64 * 36, player.getInventory());
			double moneySpent = 0;
			int itemsTraded = 0;

			pendingTranasction pT = new ConfirmCommand.pendingTranasction(player, new ArrayList<pendingOrder>(), Order.SELL_ORDER);
			ConfirmCommand.pendingTransactions.put(player.getName(), pT);
			List<pendingOrder> pending = pT.pendingOrders;

			for (int i = 0; i < orders.size(); i++) {
				if (amount <= 0)
					break;

				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory()))
					break;

				o = orders.get(i);

				int canTrade = amount;
				if (!o.isInfinite())
					canTrade = Math.min(o.getAmount(), amount);

				canTrade = (int) Math.floor(Math.min(canTrade, Econ.getBalance(player.getName()) / o.getPrice()));

				canTrade = Math.min(canTrade, playerCanFit);

				if (canTrade <= 0)
					break;

				int traded = canTrade;// (canBuy - leftover);
				playerCanFit -= traded;

				double spend = (traded * o.getPrice());

				if (spend > buyPrice)
					continue;

				Logger.debug("traded: " + traded);
				Logger.debug("spend: " + spend);

				moneySpent += spend;

				pendingOrder po = new pendingOrder(o.getId(), traded);

				pending.add(po);

				Logger.debug(o.getId() + " x" + o.getAmount() + ", canTrade: " + canTrade + " (" + (canTrade * o.getPrice()) + ") traded: " + traded
					+ ", player: " + o.getPlayer());

				itemsTraded += traded;
				amount -= traded;

			}

			if (moneySpent > 0) {
				ChatUtils.send(player, String.format("§a[Estimate] §f%s§7x§f%s§7 will cost $§f%s§7, type §d/em confirm §7to confirm transaction.",
					Plugin.getItemName(stock), itemsTraded, Plugin.Round(moneySpent, dplaces)));
			} else {
				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory())) {
					ChatUtils.send(player, "You have no bag space available.");
				} else {
					ChatUtils.send(
						player,
						String.format("§7There are no sell orders for §f%s§7x§f%s §7at $§f%s§7.", Plugin.getItemName(stock), amount,
							Plugin.Round(buyPrice, dplaces)));
					return;
				}

			}

			// ///////////////////////////////////////////////////////////////////////
		} else if (action == LEFT_CLICK_BLOCK) {
			double sellPrice = PriceUtil.getSellPrice(formattedPrice);
			Logger.debug("sellPrice " + sellPrice);
			if (sellPrice <= 0) {
				ChatUtils.send(player, "§7fThat exchange does not have a sell price.");
				return;
			}

			List<Order> orders = Plugin.database.search(stock, Order.BUY_ORDER);
			Order o;

			if (!Config.getBoolean("properties.trade-to-yourself"))
				for (int i = (orders.size() - 1); i >= 0; i--) {
					o = orders.get(i);
					if (player.getName().equalsIgnoreCase(o.getPlayer()))
						orders.remove(o);
				}

			if (orders.size() <= 0) {
				ChatUtils.send(player, String.format("§7There are no buy orders for §f%s§7.", Plugin.getItemName(stock)));
				return;
			}

			pendingTranasction pT = new ConfirmCommand.pendingTranasction(player, new ArrayList<pendingOrder>(), Order.BUY_ORDER);
			ConfirmCommand.pendingTransactions.put(player.getName(), pT);

			List<pendingOrder> pending = pT.pendingOrders; // ConfirmCommand.pendingOrders.get(sender.getName());

			double moneyProfited = 0.0;
			int itemsTraded = 0;
			for (int i = (orders.size() - 1); i >= 0; i--) {
				if (amount <= 0)
					break;

				o = orders.get(i);

				int canTrade = amount;
				if (!o.isInfinite())
					canTrade = Math.min(o.getAmount(), amount);

				Logger.debug("sell " + i + ", id: " + o.getId() + ", price: " + o.getPrice() + ", canTrade: " + canTrade);
				if (canTrade <= 0)
					break;

				int traded = canTrade;// (canBuy - leftover);

				double profit = (traded * o.getPrice());
				
				//Logger.debug("traded: " + traded);
				//Logger.debug("profit: " + profit);
				//Logger.debug("sellPrice: " + sellPrice);
				
				if (profit < sellPrice)
					continue;
				
				moneyProfited += profit;

				pendingOrder po = new pendingOrder(o.getId(), traded);

				pending.add(po);

				Logger.debug(o.getId() + " x" + o.getAmount() + ", canTrade: " + canTrade + " (" + (canTrade * o.getPrice()) + ") traded: " + traded
					+ ", player: " + o.getPlayer());

				// message = format.format(format, o.getItemType(), added,
				// Plugin.Round((added*o.getPrice()),dplaces),
				// Plugin.Round(o.getPrice(),dplaces));
				// ChatUtils.send(sender, "§a[Prevew] " + message);

				itemsTraded += traded;
				amount -= traded;

			}

			if (itemsTraded > 0) {

				ChatUtils.send(player, String.format("§a[Estimate] §f%s§7x§f%s§7 will earn $§f%s§7, type §d/em confirm §7to confirm transaction.",
					Plugin.getItemName(stock), itemsTraded, Plugin.Round(moneyProfited, Config.getInt("properties.price-decmial-places"))));

			} else {
				ChatUtils.send(player, String.format("§7There are no buy orders for §f%s§7x§f%s §7at $§f%s§7.", Plugin.getItemName(stock), amount,
					Plugin.Round(sellPrice, dplaces)));
				return;
			}

		}

	}
}
