package com.cyprias.exchangemarket;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.cyprias.exchangemarket.databases.MySQL;

public class Database {
//	private JavaPlugin plugin;

	public Database(JavaPlugin plugin) {
//		this.plugin = plugin;
		new MySQL(this);
	}
	
	public static class itemStats {
		public int total;
		public double totalPrice;
		public double totalAmount;
		public double avgPrice;
		public double mean;
		public double median;
		public double mode;
		public double amean;
		public double amedian;
		public double amode;
	}
	
	public static String TypeToString(int type, boolean infinite) {
		if (infinite == true) {
			switch (type) {
			case 1:
				return "InfSell";
			case 2:
				return "InfBuy";
			}
		} else {
			switch (type) {
			case 1:
				return "Sell";
			case 2:
				return "Buy";
			}
		}

		return null;
	}

	public static int getFitAmount(ItemStack itemStack, int amount, Player player) {
		for (int i = amount; i > 0; i--) {
			itemStack.setAmount(i);
			if (InventoryUtil.fits(itemStack, player.getInventory()) == true) {
				return i;
			}
		}
		return 0;
	}
	public static boolean removeItemFromPlayer(Player player, int itemID, short itemDur, String itemEnchants, int amount) {
		ItemStack itemStack = new ItemStack(itemID, 1);
		itemStack.setDurability(itemDur);
		itemStack.setAmount(amount);

		if (itemEnchants != null && !itemEnchants.equalsIgnoreCase("")) {
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(itemEnchants));
		}

		if (InventoryUtil.getAmount(itemStack, player.getInventory()) >= amount) {
			InventoryUtil.remove(itemStack, player.getInventory());
			return true;
		}

		return false;
	}
	public static class checkPendingBuysTask implements Runnable {
		private CommandSender sender;

		public checkPendingBuysTask(CommandSender sender) {
			this.sender = sender;
		}

		@Override
		public void run() {
			try {
				checkPendingBuys(this.sender);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String ColourName(CommandSender sender, String name) {
		if (ExchangeMarket.pluginName.equalsIgnoreCase(name)) {
			return ChatColor.GOLD + name + ChatColor.RESET;

		}
		if (sender.getName().equalsIgnoreCase(name)) {
			return ChatColor.YELLOW + name + ChatColor.RESET;

		}

		return name;
	}
	
	public static double mean(double[] p) {
		double sum = 0; // sum of all the elements
		for (int i = 0; i < p.length; i++) {
			sum += p[i];
		}
		return sum / p.length;
	}// end method mean

	public static double median(double[] m) {
		int middle = m.length / 2;
		if (m.length % 2 == 1) {
			return m[middle];
		} else {
			return (m[middle - 1] + m[middle]) / 2.0;
		}
	}

	public static double mode(double[] prices) {
		double maxValue = 0, maxCount = 0;

		for (int i = 0; i < prices.length; ++i) {
			int count = 0;
			for (int j = 0; j < prices.length; ++j) {
				if (prices[j] == prices[i])
					++count;
			}
			if (count > maxCount) {
				maxCount = count;
				maxValue = prices[i];
			}
		}

		return maxValue;
	}
	
	public static void checkPendingBuys(CommandSender sender) throws SQLException {
		MySQL.checkPendingBuys(sender);
	}
	public static int insertOrder(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount) throws SQLException {
		return MySQL.insertOrder(type, infinite, player, itemID, itemDur, itemEnchants, price, amount);
	}
	
	public static int removeOrder(CommandSender sender, int orderID) throws SQLException {
		return MySQL.removeOrder(sender, orderID);
	}
	
	public static int cancelOrder(CommandSender sender, int orderID) throws SQLException {
		return MySQL.cancelOrder(sender, orderID);
	}
	
	public static int decreaseInt(String table, int rowID, String column, int amount) throws SQLException {
		return MySQL.decreaseInt(table, rowID, column, amount);
	}
	public static int removeRow(String table, int rowID) throws SQLException {
		return MySQL.removeRow(table, rowID);
	}
	
	public static int cancelOrders(CommandSender sender, int cType, int ciID, short ciDur, String itemEnchants, int cAmount, Boolean dryrun) throws SQLException {
		return MySQL.cancelOrders(sender, cType, ciID, ciDur, itemEnchants, cAmount, dryrun);
	}
	
	public static int processBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun) throws SQLException {
		return MySQL.processBuyOrder(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, dryrun);
	}
	
	public static int cleanSellOrders() throws SQLException {
		return MySQL.cleanSellOrders();
	}
	
	public static int insertTransaction(int type, String buyer, int itemID, int itemDur, String itemEnchants, int amount, double price, String seller) throws SQLException {
		return MySQL.insertTransaction(type, buyer, itemID, itemDur, itemEnchants, amount, price, seller);
	}
	
	public static int checkSellOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun,
		Boolean silentFail) throws SQLException {
		return MySQL.checkSellOrders(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, dryrun, silentFail);
	}
	
	public static int processSellOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int amount, double price, Boolean dryrun) throws SQLException {
		return MySQL.processSellOrder(sender, itemID, itemDur, itemEnchants, amount, price, dryrun);
	}
	
	public static int checkBuyOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int amount, double price, Boolean dryrun,
		Boolean silentFail) throws SQLException {
		return MySQL.checkBuyOrders(sender, itemID, itemDur, itemEnchants, amount, price, dryrun, silentFail);
	}
	
	public static double getTradersLastPrice(int type, String trader, int itemID, short itemDur, String itemEnchants) throws SQLException {
		return MySQL.getTradersLastPrice(type, trader, itemID, itemDur, itemEnchants);
	}
	
	public static void postBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int amount, double price, Boolean dryrun) throws SQLException {
		MySQL.postBuyOrder(sender, itemID, itemDur, itemEnchants, amount, price, dryrun);
	}
	public static int collectPendingBuys(CommandSender sender) throws SQLException {
		return MySQL.collectPendingBuys(sender);
	}
	public static int listOrders(CommandSender sender, int getType, int page) throws SQLException {
		return MySQL.listOrders(sender, getType, page);
	}
	public static int getResultCount(String query, Object... args) throws SQLException {
		return MySQL.getResultCount(query, args);
	}
	
	public static int listPlayerOrders(CommandSender sender, String trader, int page) throws SQLException {
		return MySQL.listPlayerOrders(sender, trader, page);
	}
	public static void searchOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int page) throws SQLException {
		MySQL.searchOrders(sender, itemID, itemDur, itemEnchants, page);
	}
	
	public static void postSellOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int sellAmount, double sellPrice, Boolean dryrun) throws SQLException {
		MySQL.postSellOrder(sender, itemID, itemDur, itemEnchants, sellAmount, sellPrice, dryrun);
	}
	
	public static void setPassword(CommandSender sender, String password) throws SQLException {
		MySQL.setPassword(sender, password);
	}
	
	public static itemStats getItemStats(int itemID, int itemDur, String itemEnchants, int getType) throws SQLException {
		return MySQL.getItemStats(itemID, itemDur, itemEnchants, getType);
	}
	public static void listPlayerTransactions(CommandSender sender, int page) throws SQLException {
		MySQL.listPlayerTransactions(sender, page);
	}
	public static double getUsersLastPrice(int type, String user, int itemID, short itemDur, String itemEnchants) throws SQLException {
		return MySQL.getUsersLastPrice(type, user, itemID, itemDur, itemEnchants);
	}
}
