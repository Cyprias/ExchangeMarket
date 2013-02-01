package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingOrder;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class BuyOrderCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.BUY_ORDER))
			list.add("/%s buyorder - Create a buy order.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IOException, InvalidConfigurationException,
		SQLException {
		if (!Plugin.checkPermission(sender, Perm.BUY_ORDER)) {
			return false;
		}
		Player player = (Player) sender;
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1) {
			ChatUtils.send(sender, "Cannot use ExchangeMarket while in creative mode.");
			return true;
		}

		if (args.length <= 0 || args.length >= 4) {
			getCommands(sender, cmd);
			return true;
		}

		ItemStack stock = Plugin.getItemStack(args[0]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(sender, "Unknown item: " + args[0]);
			return true;
		}

		int amount = 0;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 1) {
			if (Plugin.isInt(args[1]) && Integer.parseInt(args[1]) >= amount) {
				amount = Integer.parseInt(args[1]);
			} else {
				// ExchangeMarket.sendMessage(sender, F("invalidAmount",
				// args[2]));
				ChatUtils.error(sender, "Invalid amount: " + args[1]);
				return true;
			}
		}

		// Logger.debug("amount1: " + amount);

		double price = 0;
		// plugin.sendMessage(sender, "amount: " + amount);

		if (args.length > 2) {

			if (args[2].substring(args[2].length() - 1, args[2].length()).equalsIgnoreCase("e")) {
				price = Double.parseDouble(args[2].substring(0, args[2].length() - 1));
			} else {

				if (Plugin.isDouble(args[2])) {
					price = Double.parseDouble(args[2]);
				} else {
					// ExchangeMarket.sendMessage(sender, F("invalidPrice",
					// args[3]));
					ChatUtils.error(sender, "Invalid price: " + args[2]);
					return true;
				}
				price = price / amount;

			}
			if (price <= 0) {
				ChatUtils.error(sender, "Invalid price: " + args[2]);
				return true;
			}
		}

		stock.setAmount(amount);
		Order preOrder = new Order(Order.BUY_ORDER, false, sender.getName(), stock, price);

		if (price <= 0) {// price still zero, user never input it. lets find
							// their last price.

			Double lastPrice = Plugin.database.getLastPrice(preOrder);
			if (lastPrice > 0) {
				price = lastPrice;
				preOrder.setPrice(price);
			} else {
				ChatUtils.error(sender, "Invalid price: " + 0);
				return true;
			}

		} else if (price < Config.getDouble("properties.min-order-price")) {
			ChatUtils.error(sender, "§7Your price is too low.");
			return true;
		}
		// Logger.debug( "price1: " + price);

		Double accountBalance = Econ.getBalance(sender.getName());
		// Logger.debug( "accountBalance: " + accountBalance);
		if (accountBalance <= (price * amount)) {

			ChatUtils.send(sender, "§7Your account ($§f" + accountBalance + "§7) does not have enough funds to supply this buy order.");
			return true;
		}

		int pl = Config.getInt("properties.price-decmial-places");
		
		// Try to find matching sell orders to trade to first.

		if (Config.getBoolean("properties.match-opposing-orders-before-posting") && Econ.getBalance(sender.getName()) > 0){
			
			List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER);
			Order o;
			if (!Config.getBoolean("properties.trade-to-yourself"))
				for (int i = (orders.size() - 1); i >= 0; i--) {
					o = orders.get(i); 
					if (sender.getName().equalsIgnoreCase(o.getPlayer()))
						orders.remove(o);
				}
			
			Logger.debug("Checking for matching sell orders. " + orders.size());
			if (orders.size() > 0){
				int playerCanFit = Plugin.getFitAmount(stock, player.getInventory());
				
				for (int i=0; i<orders.size();i++){
					if (amount <= 0)
						break;
					
					stock.setAmount(1);
					if (!InventoryUtil.fits(stock, player.getInventory()))
						break;
					
					o = orders.get(i);
					
					Logger.debug("Checking matching order price " + (o.getPrice() > price));
					
					if (o.getPrice() > price)
						continue;
					
					int canTrade = amount;
					if (!o.isInfinite())
						canTrade  = Math.min(o.getAmount(), amount);
					
					canTrade = (int) Math.floor(Math.min(canTrade, Econ.getBalance(sender.getName()) / o.getPrice()));
					
					canTrade = Math.min(canTrade, playerCanFit);
					if (canTrade <= 0)
						break;
					
					int traded = canTrade;//(canBuy - leftover);
					playerCanFit -= traded;
					
					double spend = (traded*o.getPrice());
					
					//pendingOrder po = new pendingOrder(o.getId(), traded);
					
					traded = o.giveAmount(player, traded);
					
					spend = traded * o.getPrice();
					//Econ.withdrawPlayer(player.getName(), spend);
					//Econ.depositPlayer(o.getPlayer(), spend);
					o.depositPlayer(spend, player);
					
					
					if (!o.isInfinite()) 
						o.notifyPlayerOfTransaction(traded);
						

					o.insertTransaction(sender, traded);
					
					ChatUtils.send(
							sender,
							String.format("§7Bought §f%s§7x§f%s §7for $§f%s §7($§f%s§7e).", Plugin.getItemName(stock), traded, Plugin.Round(spend, pl),
								Plugin.Round(o.getPrice(), pl)));
					
					amount -= traded;
				}
				
			}
			Plugin.database.cleanEmpties();
		}
		
		if (amount <= 0)
			return true;
		
		// Add the order to the DB.
		Order matchingOrder = Plugin.database.findMatchingOrder(preOrder);
		if (matchingOrder != null) {
			// ChatUtils.send(sender, "Found matching order " +
			// matchingOrder.getId());

			// int mAmount = matchingOrder.getAmount();
			if (matchingOrder.increaseAmount(amount)) {

				ChatUtils.send(sender, String.format("§7Increased your existing buy order #§f%s §7of §f%s §7to §f%s§7.", matchingOrder.getId(),
					Plugin.getItemName(stock), matchingOrder.getAmount())); // amount

				EconomyResponse r = Econ.withdrawPlayer(sender.getName(), price * amount);

				if (r.transactionSuccess()) {
					ChatUtils.send(sender, String.format("$§f%s §7has been withdrawnfrom your account, you now have $§f%s§7.", r.amount, r.balance));
				} else {
					ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
				}

			}
		} else {

			if (Plugin.database.insert(preOrder)) {

				int id = Plugin.database.getLastId();

				
				ChatUtils.send(sender, String.format("§7Created buy order #§f%s §7for §f%s§7x§f%s §7@ §f%s §7(§f%s§7e)", id, Plugin.getItemName(stock),
					preOrder.getAmount(), Plugin.Round(preOrder.getPrice() * preOrder.getAmount(), pl), Plugin.Round(preOrder.getPrice(), pl)));

				EconomyResponse r = Econ.withdrawPlayer(sender.getName(), price * amount);

				if (r.transactionSuccess()) {
					ChatUtils.send(sender, String.format("$§f%s §7has been withdrawnfrom your account, you now have $§f%s§7.", r.amount, r.balance));
				} else {
					ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
				}

			}
		}

		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.BUY_ORDER, "/%s buyorder <item> [amount] [price]", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
