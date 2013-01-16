package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class ConfirmCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.CONFIRM))
			list.add("/%s confirm - Confirm a pending transaction.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.BUY, "/%s confirm", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	// static HashMap<String, List<pendingOrder>> pendingOrders = new
	// HashMap<String, List<pendingOrder>>();
	public static class pendingOrder {
		public pendingOrder(Order o, int amount) {
			// this.player = player;
			this.order = o;
			this.amount = amount;
		}

		// private Player player;
		private Order order;
		private int amount;

	}

	static HashMap<String, pendingTranasction> pendingTransactions = new HashMap<String, pendingTranasction>();
	static HashMap<String, Boolean> expiredTransactions = new HashMap<String, Boolean>();

	
	public static class pendingTranasction {
		List<pendingOrder> pendingOrders;
		private Player player;
		private int transactionType;

		public pendingTranasction(Player player, List<pendingOrder> pendingOrders, int transactionType) {
			this.pendingOrders = pendingOrders;
			this.player = player;
			this.transactionType = transactionType;
		}

	}

	private void removeOrderFromOthers(CommandSender sender, int oID){
		pendingOrder po;
		List<pendingOrder> pending;
		for (String playerName : pendingTransactions.keySet()) {
			if (sender.getName().equalsIgnoreCase(playerName)){
				pending = pendingTransactions.get(playerName).pendingOrders;
				for (int i = 0; i < pending.size(); i++) {
					 po = pending.get(i);
					if (po.order.getId() == oID){
						pendingTransactions.remove(playerName);
						expiredTransactions.put(playerName, true);
						Logger.info(sender.getName() + " has expired " + playerName + "'s previous esimite.");
						break;
					}
				}
			}
		}
	}
		
	
	@Override
	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException {
		if (!Plugin.checkPermission(sender, Perm.CONFIRM)) {
			return false;
		}

		if (args.length >= 1) {
			getCommands(sender, cmd);
			return true;
		}
		
		if (expiredTransactions.containsKey(sender.getName())) {
			ChatUtils.send(sender, "Your previous estimite has expired, start over.");
			return true;
		}
		if (!pendingTransactions.containsKey(sender.getName())) {
			ChatUtils.send(sender, "You have no transaction to confirm.");
			return true;
		}

		pendingTranasction pT = pendingTransactions.get(sender.getName());

		if (pT.transactionType == Order.SELL_ORDER) {

			List<pendingOrder> pending = pT.pendingOrders;

			pendingOrder po;
			ItemStack stock = null;
			int added;
			double spend;
			double moneySpent = 0;
			int itemsBought = 0;
			for (int i = 0; i < pending.size(); i++) {
				po = pending.get(i);
				removeOrderFromOthers(sender, po.order.getId());
				
				stock = po.order.getItemStack();

				if (po.order.getOrderType() == Order.SELL_ORDER) {
					added = po.amount;
					itemsBought = +added;
					spend = added * po.order.getPrice();

					stock.setAmount(added);

					InventoryUtil.add(stock, pT.player.getInventory());

					Econ.withdrawPlayer(pT.player.getName(), spend);
					Econ.depositPlayer(po.order.getPlayer(), spend);
					
					
					moneySpent = +spend;

					po.order.reduceAmount(added);

					po.order.notifyPlayerOfTransaction(added);
				}

			}
			Plugin.database.cleanEmpties();

			if (moneySpent > 0) {
				ChatUtils.send(sender, String.format("§7Spent $§f%s §7buying §f%s §7items.",
					Plugin.Round(moneySpent, Config.getInt("properties.price-decmial-places")), itemsBought));
				
				
			} else {
				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, ((Player) sender).getInventory())) {
					ChatUtils.send(sender, "You have no bag space available.");
				} else {
					ChatUtils.send(sender, "Failed to buy any items, try creating a buy order.");
				}

			}

			pendingTransactions.remove(sender.getName());
		}

		// Econ.withdrawPlayer(sender.getName(), spend);
		// Econ.depositPlayer(o.getPlayer(), spend);
		// o.reduceAmount(added);

		return true;
	}

}
