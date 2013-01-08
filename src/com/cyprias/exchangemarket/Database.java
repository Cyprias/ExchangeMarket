package com.cyprias.exchangemarket;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.cyprias.Utils.InventoryUtil;
import com.cyprias.Utils.MaterialUtil;
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
	
	final public static int sellOrder = 1;
	final public static int buyOrder = 2;

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
	
	public static boolean removeItemFromPlayer(Player player, ItemStack stock) {
		if (InventoryUtil.getAmount(stock, player.getInventory()) >= stock.getAmount()) {
			InventoryUtil.remove(stock, player.getInventory());
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
	public static void checkPendingBuys(CommandSender sender) throws SQLException {
		MySQL.checkPendingBuys(sender);
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
	


	public static int insertOrder(int type, Boolean infinite, String player, ItemStack stock, double price) throws SQLException {
		return MySQL.insertOrder(type, infinite, player, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock), price, stock.getAmount());
	}
	
	public static int removeOrder(CommandSender sender, int orderID) throws SQLException {
		return MySQL.removeOrder(sender, orderID);
	}
	
	public static int cancelOrder(CommandSender sender, int orderID) throws SQLException {
		return MySQL.cancelOrder(sender, orderID);
	}
	
	
	public static int cancelOrders(CommandSender sender, int cType, int ciID, short ciDur, String itemEnchants, int cAmount, Boolean dryrun) throws SQLException {
		return MySQL.cancelOrders(sender, cType, ciID, ciDur, itemEnchants, cAmount, dryrun);
	}
	
	public static int processBuyOrder(CommandSender sender, ItemStack stock, double buyPrice, Boolean dryrun) throws SQLException {
		return MySQL.processBuyOrder(sender, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock), stock.getAmount(), buyPrice, dryrun);
	}

	public static int processSellOrder(CommandSender sender, ItemStack stock, double price, Boolean dryrun) throws SQLException {
		return MySQL.processSellOrder(sender, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock), stock.getAmount(), price, dryrun);
	}

	public static double getTradersLastPrice(int type, String trader, ItemStack stock) throws SQLException {
		return MySQL.getTradersLastPrice(type, trader, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock));
	}
	
	public static void postBuyOrder(CommandSender sender, ItemStack stock, double price, Boolean dryrun) throws SQLException {
		MySQL.postBuyOrder(sender, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock), stock.getAmount(), price, dryrun);
	}
	public static int collectPendingBuys(CommandSender sender) throws SQLException {
		return MySQL.collectPendingBuys(sender);
	}
	public static int listOrders(CommandSender sender, int getType, int page) throws SQLException {
		return MySQL.listOrders(sender, getType, page);
	}

	
	public static int listPlayerOrders(CommandSender sender, String trader, int page) throws SQLException {
		return MySQL.listPlayerOrders(sender, trader, page);
	}
	public static void searchOrders(CommandSender sender, ItemStack stock, int page) throws SQLException {
		MySQL.searchOrders(sender, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock), page);
	}
	
	public static void postSellOrder(CommandSender sender, ItemStack stock, double sellPrice, Boolean dryrun) throws SQLException {
		MySQL.postSellOrder(sender, stock, sellPrice, dryrun);
	}
	
	public static void setPassword(CommandSender sender, String password) throws SQLException {
		MySQL.setPassword(sender, password);
	}
	
	public static itemStats getItemStats(ItemStack stock, int getType) throws SQLException {
		return MySQL.getItemStats(stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock), getType);
	}
	public static void listPlayerTransactions(CommandSender sender, int page) throws SQLException {
		MySQL.listPlayerTransactions(sender, page);
	}
	
	public static void init() throws SQLException{
		MySQL.init();
	}
}
