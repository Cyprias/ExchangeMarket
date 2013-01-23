package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
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
		ChatUtils.sendCommandHelp(sender, Perm.CONFIRM, "/%s confirm", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	// static HashMap<String, List<pendingOrder>> pendingOrders = new
	// HashMap<String, List<pendingOrder>>();
	public static class pendingOrder {
		public pendingOrder(int oId, int amount) {
			// this.player = player;
			this.orderId = oId;
			this.amount = amount;
		}

		// private Player player;
		private int orderId;
		private int amount;

	}

	public static HashMap<String, pendingTranasction> pendingTransactions = new HashMap<String, pendingTranasction>();
	static HashMap<String, Boolean> expiredTransactions = new HashMap<String, Boolean>();

	public static class pendingTranasction {
		public List<pendingOrder> pendingOrders;
		private Player player;
		private int transactionType;

		public pendingTranasction(Player player, List<pendingOrder> pendingOrders, int transactionType) {
			this.pendingOrders = pendingOrders;
			this.player = player;
			this.transactionType = transactionType;
		}

	}

	/*
	 * private void removeOrderFromOthers(CommandSender sender, int oID) throws
	 * SQLException { pendingOrder po; List<pendingOrder> pending; Order order;
	 * for (String playerName : pendingTransactions.keySet()) { if
	 * (!sender.getName().equalsIgnoreCase(playerName)) { pending =
	 * pendingTransactions.get(playerName).pendingOrders; for (int i = 0; i <
	 * pending.size(); i++) { po = pending.get(i);
	 * 
	 * order = Plugin.database.getOrder(po.orderId); if (order.getId() == oID) {
	 * pendingTransactions.remove(playerName);
	 * expiredTransactions.put(playerName, true); Logger.info(sender.getName() +
	 * " has expired " + playerName + "'s previous esimite."); break; } } } } }
	 */

	@Override
	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException, IOException,
		InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.CONFIRM)) {
			return false;
		}

		Player player = (Player) sender;
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1) {
			ChatUtils.send(sender, "Cannot use ExchangeMarket while in creative mode.");
			return true;
		}

		if (args.length >= 1) {
			getCommands(sender, cmd);
			return true;
		}

		if (expiredTransactions.containsKey(sender.getName())) {
			expiredTransactions.remove(sender.getName());
			ChatUtils.send(sender, "§7Your previous estimite has expired, start over.");
			return true;
		}
		if (!pendingTransactions.containsKey(sender.getName())) {
			ChatUtils.send(sender, "§7You have no transaction to confirm.");
			return true;
		}

		pendingTranasction pT = pendingTransactions.get(sender.getName());

		List<pendingOrder> pending = pT.pendingOrders;
		pendingOrder po;
		// int traded;
		int totalTraded = 0;
		double spend, profit;
		ItemStack stock = null;
		double moneyTraded = 0;
		int traded = 0;
		Order order;
		int places = Config.getInt("properties.price-decmial-places");

		if (pT.transactionType == Order.SELL_ORDER) {// Buy command

			for (int i = 0; i < pending.size(); i++) {
				po = pending.get(i);

				order = Plugin.database.getOrder(po.orderId);

				if (order == null || !order.exists())
					break;

				stock = order.getItemStack();

				// removeOrderFromOthers(sender, po.order.getId());

				traded = po.amount;
				if (!order.isInfinite())
					traded = Math.min(order.getAmount(), traded);

				traded = Math.min(traded, Plugin.getFitAmount(stock, 64 * 36, pT.player.getInventory()));

				if (traded <= 0)
					break;

				stock.setAmount(traded);

				totalTraded += traded;

				//

				InventoryUtil.add(stock, pT.player.getInventory());

				spend = traded * order.getPrice();
				Econ.withdrawPlayer(pT.player.getName(), spend);
				Econ.depositPlayer(order.getPlayer(), spend);

				if (!order.isInfinite()) {
					order.reduceAmount(traded);
					order.notifyPlayerOfTransaction(traded);
				}

				order.insertTransaction(sender, traded);
				moneyTraded += spend;

				if (Config.getBoolean("properties.show-orderer-each-transaction"))
					ChatUtils.send(
						sender,
						String.format("§7Bought §f%s§7x§f%s §7for $§f%s §7($§f%s§7e).", Plugin.getItemName(stock), traded, Plugin.Round(spend, places),
							Plugin.Round(order.getPrice(), places)));

			}

			Plugin.database.cleanEmpties();

			if (moneyTraded > 0) {
				ChatUtils.send(sender,
					String.format("§7Spent $§f%s §7buying §f%s§7x§f%s§7.", Plugin.Round(moneyTraded, places), Plugin.getItemName(stock), totalTraded));

			} else {
				if (stock != null){
					stock.setAmount(1);
					if (!InventoryUtil.fits(stock, ((Player) sender).getInventory())) {
						ChatUtils.send(sender, "§7You have no bag space available.");
						return true;
					}
				}
				ChatUtils.send(sender, "§7Cannot confirm that order anymore, start over.");
				pendingTransactions.remove(sender.getName());

			}

			// pendingTransactions.remove(sender.getName());

		} else if (pT.transactionType == Order.BUY_ORDER) {// Sell command

			for (int i = 0; i < pending.size(); i++) {
				po = pending.get(i);

				order = Plugin.database.getOrder(po.orderId);
				if (order == null || !order.exists())
					break;

				stock = order.getItemStack();

				traded = po.amount;

				if (!order.isInfinite())
					traded = Math.min(order.getAmount(), traded);

				traded = Math.min(InventoryUtil.getAmount(stock, pT.player.getInventory()), traded);

				if (traded <= 0)
					break;

				stock.setAmount(traded);

				// totalTraded = +po.amount;

				InventoryUtil.remove(stock, pT.player.getInventory());

				if (!order.isInfinite())
					order.sendAmountToMailbox(traded);

				profit = traded * order.getPrice();
				Econ.depositPlayer(sender.getName(), profit);

				if (!order.isInfinite()) {
					order.reduceAmount(traded);
					order.notifyPlayerOfTransaction(traded);
				}

				order.insertTransaction(sender, traded);

				moneyTraded += profit;
				totalTraded += traded;

				if (Config.getBoolean("properties.show-orderer-each-transaction"))
					ChatUtils.send(
						sender,
						String.format("§7Sold §f%s§7x§f%s §7for $§f%s §7($§f%s§7e).", Plugin.getItemName(stock), traded, Plugin.Round(profit, places),
							Plugin.Round(order.getPrice(), places)));

				// po.order

			}
			if (moneyTraded > 0) {
				Plugin.database.cleanEmpties();
				ChatUtils.send(sender,
					String.format("§7Made $§f%s §7selling §f%s§7x§f%s§7.", Plugin.Round(moneyTraded, places), Plugin.getItemName(stock), totalTraded));

			} else if (stock != null && InventoryUtil.getAmount(stock, pT.player.getInventory()) == 0) {
				ChatUtils.send(sender, "§7You have no §f" + Plugin.getItemName(stock) + " §7to sell.");

			} else {
				ChatUtils.send(sender, "§7Cannot confirm that order anymore, start over.");
				pendingTransactions.remove(sender.getName());
			}

			// pendingTransactions.remove(sender.getName());

		}

		// Econ.withdrawPlayer(sender.getName(), spend);
		// Econ.depositPlayer(o.getPlayer(), spend);
		// o.reduceAmount(added);

		return true;
	}
}
