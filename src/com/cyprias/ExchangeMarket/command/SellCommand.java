package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingOrder;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingTranasction;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class SellCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.SELL))
			list.add("/%s sell - Buy items from sell orders.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.SELL, "/%s sell <item> [amount]", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.BUY)) {
			return false;
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
		Player player = (Player) sender;
		
		int intAmount = InventoryUtil.getAmount(stock, player.getInventory());

		if (intAmount == 0) {
			ChatUtils.error(sender, "§7You do not have any " + stock.getType());
			return true;
		}
		
		int amount = 0;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 1) {
			if (Plugin.isInt(args[1])) {
				amount = Integer.parseInt(args[1]);
			} else {
				// ExchangeMarket.sendMessage(sender, F("invalidAmount",
				// args[2]));
				ChatUtils.error(sender, "Invalid amount: " + args[1]);
				return true;
			}
		}
		
		

		double price = 0;
		// plugin.sendMessage(sender, "amount: " + amount);

		if (args.length > 2) {

			if (args[2].substring(args[2].length() - 1, args[2].length()).equalsIgnoreCase("e")) {
				price = Math.abs(Double.parseDouble(args[2].substring(0, args[2].length() - 1)));
			} else {

				if (Plugin.isDouble(args[2])) {
					price = Math.abs(Double.parseDouble(args[2]));
				} else {
					// ExchangeMarket.sendMessage(sender, F("invalidPrice",
					// args[3]));
					ChatUtils.error(sender, "Invalid price: " + args[2]);
					return true;
				}
				if (amount > 0)
					price = price / amount;

			}
		}

		if (amount <= 0){
			amount = intAmount;
		}else{
			amount = Math.min(amount, intAmount);
		}
		
		//int invAmount = InventoryUtil.getAmount(stock, player.getInventory());
		//amount = Math.min(amount, invAmount);
		if (ConfirmCommand.pendingTransactions.containsKey(sender.getName()))
			ConfirmCommand.pendingTransactions.remove(sender.getName());

		if (ConfirmCommand.expiredTransactions.containsKey(sender.getName())) 
			ConfirmCommand.expiredTransactions.remove(sender.getName());
		
		try {
			List<Order> orders = Plugin.database.search(stock, Order.BUY_ORDER);
			Order o;
			
			for (int i = (orders.size() - 1); i >= 0; i--) {
				o = orders.get(i); 
				if (sender.getName().equalsIgnoreCase(o.getPlayer()))
					orders.remove(o);
			}
			
			Logger.debug( "Orders: " + orders.size());




			if (orders.size() <= 0){
				ChatUtils.send(sender, String.format("§7There are §f%s §7buy orders for §f%s§7, try creating a sell order.", orders.size(),Plugin.getItemName(stock)) );
				return true;
			//}else{
			//	ChatUtils.send(sender, String.format("§7There are §f%s §7buy orders for §f%s§7.", orders.size(), stock.getType()) );
			}
			
			
			pendingTranasction pT = new ConfirmCommand.pendingTranasction(player, new ArrayList<pendingOrder>(), Order.BUY_ORDER);
			ConfirmCommand.pendingTransactions.put(sender.getName(), pT);

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
				moneyProfited += profit;

				pendingOrder po = new pendingOrder(o.getId(), traded);

				pending.add(po);

				Logger.debug(o.getId() + " x" + o.getAmount() + ", canTrade: " + canTrade + " (" + (canTrade * o.getPrice()) + ") traded: " + traded + ", player: " + o.getPlayer());

				// message = format.format(format, o.getItemType(), added,
				// Plugin.Round((added*o.getPrice()),dplaces),
				// Plugin.Round(o.getPrice(),dplaces));
				// ChatUtils.send(sender, "§a[Prevew] " + message);

				itemsTraded += traded;
				amount -= traded;

			}

			if (itemsTraded > 0) {

				ChatUtils.send(sender, String.format("§a[Estimate] §f%s§7x§f%s§7 will earn $§f%s§7, type §d/em confirm §7to confirm transaction.",
					Plugin.getItemName(stock), itemsTraded, Plugin.Round(moneyProfited, Config.getInt("properties.price-decmial-places"))));

			} else {

				ChatUtils.send(sender, "Failed to sell any items, try creating a sell order.");

			}

		} catch (SQLException e) {
			e.printStackTrace();
			ChatUtils.error(sender, "An error has occured: " + e.getLocalizedMessage());
			return true;
		}

		return true;
	}
}
