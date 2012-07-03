package com.cyprias.exchangemarket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.MaterialUtil;

public class Database {
	private ExchangeMarket plugin;

	/*
	 * Order types 1: Sell 2: Buy 3: Infinite Sell 4: Infinite Buy
	 */

	public Database(ExchangeMarket plugin) {
		this.plugin = plugin;

		if (testDBConnection()) {
			setupMysql();
		} else {
			plugin.info("Failed to connect to database, disabling plugin...");
			plugin.getPluginLoader().disablePlugin(plugin);
		}
	}

	public boolean testDBConnection() {
		try {
			Connection con = DriverManager.getConnection(Config.sqlURL, Config.sqlUsername, Config.sqlPassword);

			con.close();
			return true;
		} catch (SQLException e) {
		}
		return false;
	}

	public static Connection getSQLConnection() {
		try {
			return DriverManager.getConnection(Config.sqlURL, Config.sqlUsername, Config.sqlPassword);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void setupMysql() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = getSQLConnection();

			String query;

			PreparedStatement statement = con.prepareStatement("show tables like '%" + Config.sqlPrefix + "Orders%'");
			ResultSet result = statement.executeQuery();

			/**/

			result.last();
			if (result.getRow() == 0) {

				query = "CREATE TABLE "
					+ Config.sqlPrefix
					+ "Orders (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `infinite` BOOLEAN NOT NULL DEFAULT '0' , `player` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NULL, `price` DOUBLE NOT NULL, `amount` INT NOT NULL, `exchanged` INT NOT NULL DEFAULT '0')";

				statement = con.prepareStatement(query);
				statement.executeUpdate();
			}

			// //////
			result.close();
			statement.close();
			con.close();

		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean removeItemFromPlayer(Player player, int itemID, short itemDur, int amount, String enchants) {
		ItemStack itemStack = new ItemStack(itemID, 1);
		itemStack.setDurability(itemDur);
		itemStack.setAmount(amount);

		if (enchants != null && !enchants.equalsIgnoreCase("")) {
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));
		}

		if (InventoryUtil.getAmount(itemStack, player.getInventory()) >= amount) {
			InventoryUtil.remove(itemStack, player.getInventory());
			return true;
		}

		return false;
	}

	public boolean removeItemFromPlayer(Player player, int itemID, short itemDur, int amount) {
		return removeItemFromPlayer(player, itemID, itemDur, amount, null);
	}

	public boolean giveItemToPlayer(Player player, int itemID, short itemDur, int amount, String enchants) {
		ItemStack itemStack = new ItemStack(itemID, 1);
		itemStack.setDurability(itemDur);
		itemStack.setAmount(amount);

		if (enchants != null && !enchants.equalsIgnoreCase("")) {
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));
		}

		if (InventoryUtil.fits(itemStack, player.getInventory())) {
			InventoryUtil.add(itemStack, player.getInventory());
			return true;
		}
		return false;
	}

	public boolean giveItemToPlayer(Player player, int itemID, short itemDur, int amount) {
		return giveItemToPlayer(player, itemID, itemDur, amount, null);
	}

	public int checkBuyOrders(CommandSender sender, int itemID, short itemDur, int sellAmount, double sellPrice, Connection con) {
		String query = "SELECT * FROM " + Config.sqlPrefix
			+ "Orders WHERE `type` = 2 AND `itemID` = ? AND `itemDur` = ? AND `price` >= ? AND `amount` > 0  AND `player` NOT LIKE ? ORDER BY `price` DESC";
		int updateSuccessful = 0;
		String itemName = plugin.itemdb.getItemName(itemID, itemDur);

		Player player = (Player) sender;

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, itemID);
			statement.setInt(2, itemDur);

			statement.setDouble(3, sellPrice);

			statement.setString(4, sender.getName());

			ResultSet result = statement.executeQuery();

			double price;
			int id, amount;
			double senderBalance;
			String trader;
			int canSell = 0;
			Boolean infinite;
			while (result.next()) {
				// patron = result.getString(1);

				id = result.getInt(1);
				infinite = result.getBoolean(3);
				trader = result.getString(4);
				price = result.getDouble(8);
				amount = result.getInt(9);

				// plugin.info("processBuyOrder id: " + id + ", price: " + price
				// + ", amount: " + amount);

				if (infinite == true) {
					amount = sellAmount;
				} else {
					amount = Math.min(amount, sellAmount);
				}

				// plugin.info("amount: " + amount);

				if (removeItemFromPlayer(player, itemID, itemDur, amount) == true) {
					sellAmount -= amount;

					plugin.payPlayer(sender.getName(), amount * price);

					// plugin.sendMessage(sender, "Sold " + itemName + "x" +
					// amount + " for $" + (amount*price) + " ($" +price+"e)");
					if (infinite == true) {
						plugin.notifySellerOfExchange(sender.getName(), itemID, itemDur, amount, price, plugin.pluginName);
					} else {
						plugin.notifySellerOfExchange(sender.getName(), itemID, itemDur, amount, price, trader);
					}

					if (infinite == false) {
						decreaseInt(Config.sqlPrefix + "Orders", id, "amount", amount, con);
						increaseInt(Config.sqlPrefix + "Orders", id, "exchanged", amount, con);
						// plugin.notifyBuyerOfExchange(trader, itemID, itemDur,
						// amount, price, trader);

						if (infinite == true) {
							plugin.notifyBuyerOfExchange(trader, itemID, itemDur, amount, price, plugin.pluginName);
						} else {
							plugin.notifyBuyerOfExchange(trader, itemID, itemDur, amount, price, sender.getName());
						}

					}

					// plugin.info("sellAmount: " + sellAmount);
				} else {
					plugin.info("Could not remove " + itemName + "x" + amount + " from inv.");
				}
				if (sellAmount <= 0)
					break;

			}

			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sellAmount;
	}

	public void processSellOrder(CommandSender sender, int itemID, short itemDur, int sellAmount, double sellPrice) {
		Connection con = getSQLConnection();

		// plugin.info("sellAmountA: " + sellAmount);
		sellAmount = checkBuyOrders(sender, itemID, itemDur, sellAmount, sellPrice, con);

		// plugin.info("sellAmountB: " + sellAmount);

		String itemName = plugin.itemdb.getItemName(itemID, itemDur);
		Player player = (Player) sender;

		if (sellAmount > 0) {

			ItemStack itemStack = new ItemStack(itemID, 1);
			itemStack.setDurability(itemDur);

			sellAmount = Math.min(sellAmount, InventoryUtil.getAmount(itemStack, player.getInventory()));
			// plugin.info("sellAmountC: " + sellAmount);
			itemStack.setAmount(sellAmount);

			int success = insertOrder(1, false, sender.getName(), itemID, itemDur, null, sellPrice, sellAmount, con);
			if (success > 0) {
				plugin.sendMessage(sender, F("createdSellOrder", itemName, sellAmount, plugin.Round(sellAmount * sellPrice, Config.priceRounding), plugin.Round(sellPrice,Config.priceRounding)));

				// plugin.debtPlayer(sender.getName(), sellPrice * sellPrice);

				InventoryUtil.remove(itemStack, player.getInventory());

			}

		}

		cleanSellOrders(con);

	}

	public int cleanBuyOrders() {
		Connection con = getSQLConnection();
		int value = cleanBuyOrders(con);
		closeSQLConnection(con);
		return value;
	}

	public int cleanBuyOrders(Connection con) {
		String SQL = "DELETE FROM " + Config.sqlPrefix + "Orders WHERE `type` = 2 AND `amount` <= 0 AND `exchanged` <= 0;";
		int success = 0;

		try {
			PreparedStatement statement = con.prepareStatement(SQL);

			success = statement.executeUpdate();

			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// plugin.info("cleanBuyOrders: " + success);

		return success;
	}

	public int cleanSellOrders(Connection con) {
		String SQL = "DELETE FROM " + Config.sqlPrefix + "Orders WHERE `type` = 1 AND `amount` = 0;";
		int success = 0;

		try {
			PreparedStatement statement = con.prepareStatement(SQL);

			success = statement.executeUpdate();

			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// plugin.info("cleanSellOrders: " + success);

		return success;
	}

	public int cleanSellOrders() {
		Connection con = getSQLConnection();
		int value = cleanSellOrders(con);
		closeSQLConnection(con);
		return value;
	}

	public int checkSellOrders(CommandSender sender, int itemID, short itemDur, int buyAmount, double buyPrice, Connection con) {

		String query = "SELECT * FROM " + Config.sqlPrefix
			+ "Orders WHERE `type` = 1 AND `itemID` = ? AND `itemDur` = ? AND `price` <= ? AND `amount` > 0 AND `player` NOT LIKE ? ORDER BY `price` ASC";

		int updateSuccessful = 0;
		String itemName = plugin.itemdb.getItemName(itemID, itemDur);

		Player player = (Player) sender;
		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, itemID);
			statement.setInt(2, itemDur);

			statement.setDouble(3, buyPrice);

			statement.setString(4, sender.getName());

			ResultSet result = statement.executeQuery();

			double price;
			int id, amount;
			double senderBalance;
			String trader;
			int canBuy;
			ItemStack itemStack;
			Boolean infinite;
			while (result.next()) {
				// patron = result.getString(1);

				id = result.getInt(1);
				infinite = result.getBoolean(3);
				trader = result.getString(4);
				price = result.getDouble(8);
				amount = result.getInt(9);

				//plugin.info("processBuyOrder id: " + id + ", price: " + price + ", amount: " + amount);

				senderBalance = plugin.getBalance(sender.getName());
				// plugin.info("senderBalance:" + senderBalance);

				canBuy = (int) Math.floor(senderBalance / price);
				canBuy = Math.min(canBuy, buyAmount);
				if (infinite == false)
					canBuy = Math.min(canBuy, amount);

				// plugin.info("canBuy:" + canBuy);

				/**/

				if (canBuy > 0) {
					itemStack = new ItemStack(itemID, 1);
					itemStack.setDurability(itemDur);
					itemStack.setAmount(canBuy);

					if (InventoryUtil.fits(itemStack, player.getInventory())) {

						plugin.debtPlayer(sender.getName(), canBuy * price);
						if (infinite == false)
							plugin.payPlayer(trader, canBuy * price);

						InventoryUtil.add(itemStack, player.getInventory());

						// setInt(Config.sqlPrefix+"Orders", id, "amount",
						// amount-canBuy);

						if (infinite == false) {
							decreaseInt(Config.sqlPrefix + "Orders", id, "amount", canBuy);
							increaseInt(Config.sqlPrefix + "Orders", id, "exchanged", canBuy);
						}

						if (infinite == true) {
							plugin.notifySellerOfExchange(sender.getName(), itemID, itemDur, canBuy, price, plugin.pluginName);
						} else {
							plugin.notifySellerOfExchange(sender.getName(), itemID, itemDur, canBuy, price, trader);// buy
																													// when
																													// sale
																													// exists
						}

						if (infinite == false)
							plugin.sendMessage(sender, F("buyingItem", itemName, canBuy, plugin.Round(price * canBuy, Config.priceRounding), plugin.Round(price,Config.priceRounding), trader));

						buyAmount -= canBuy;
					}

				}
			}

			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return buyAmount;
	}

	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}

	public int checkPlayerSellOrders(CommandSender sender, int itemID, short itemDur, int buyAmount, double buyPrice, Connection con) {

		String query = "SELECT * FROM " + Config.sqlPrefix
			+ "Orders WHERE `type` = 1 AND `infinite` = 0 AND `itemID` = ? AND `itemDur` = ? AND `amount` > 0 AND `player` LIKE ? ORDER BY `price` ASC";//DESC

		int updateSuccessful = 0;
		String itemName = plugin.itemdb.getItemName(itemID, itemDur);

		Player player = (Player) sender;
		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, itemID);
			statement.setInt(2, itemDur);

			statement.setString(3, sender.getName());

			// statement.setDouble(3, buyPrice);

			ResultSet result = statement.executeQuery();

			double price;
			int id, amount;
			double senderBalance;
			String trader;
			int canBuy;
			ItemStack itemStack;
			Boolean infinite;
			while (result.next()) {
				// patron = result.getString(1);

				id = result.getInt(1);
				infinite = result.getBoolean(3);
				trader = result.getString(4);
				price = result.getDouble(8);
				amount = result.getInt(9);

				//plugin.info("processBuyOrder id: " + id + ", price: " + price + ", amount: " + amount);

				senderBalance = plugin.getBalance(sender.getName());
				// plugin.info("senderBalance:" + senderBalance);

				// canBuy = (int) Math.floor(senderBalance / price);
				canBuy = Math.min(amount, buyAmount);

				// if (infinite == false)
				// canBuy = Math.min(canBuy, amount);

				/**/

				if (canBuy > 0) {
					itemStack = new ItemStack(itemID, 1);
					itemStack.setDurability(itemDur);
					itemStack.setAmount(canBuy);

					if (InventoryUtil.fits(itemStack, player.getInventory())) {
						InventoryUtil.add(itemStack, player.getInventory());

						if (infinite == false) {
							decreaseInt(Config.sqlPrefix + "Orders", id, "amount", canBuy);
						}

						if (infinite == false)
							plugin.sendMessage(sender, F("buyFromSelf", itemName, canBuy, plugin.Round(price * canBuy, Config.priceRounding), plugin.Round(price,Config.priceRounding)));

						buyAmount -= canBuy;
					}

				}
			}

			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return buyAmount;
	}

	public int processBuyOrder(CommandSender sender, int itemID, short itemDur, int buyAmount, double buyPrice, Connection con) {
		int updateSuccessful = 0;

		// plugin.info("buyAmountA: " + buyAmount);
		if (Config.cancelSelfSalesWhenBuying == true)
			buyAmount = checkPlayerSellOrders(sender, itemID, itemDur, buyAmount, buyPrice, con);

		buyAmount = checkSellOrders(sender, itemID, itemDur, buyAmount, buyPrice, con);
		// plugin.info("buyAmountB: " + buyAmount);

		String itemName = plugin.itemdb.getItemName(itemID, itemDur);

		if (buyAmount > 0) {

			if (plugin.getBalance(sender.getName()) < (buyAmount * buyPrice)) {
				plugin.sendMessage(sender, L("buyNotEnoughFunds"));
				return 0;
			}

			updateSuccessful = insertOrder(2, false, sender.getName(), itemID, itemDur, null, buyPrice, buyAmount, con);
			if (updateSuccessful > 0) {
				plugin.sendMessage(sender, F("createdBuyOrder", itemName, buyAmount, plugin.Round(buyPrice * buyAmount, Config.priceRounding), plugin.Round(buyPrice,Config.priceRounding)));

				plugin.debtPlayer(sender.getName(), buyAmount * buyPrice);

			}

		}

		cleanSellOrders(con);
		return updateSuccessful;
	}

	public int processBuyOrder(CommandSender sender, int itemID, short itemDur, int buyAmount, double buyPrice) {
		Connection con = getSQLConnection();
		int value = processBuyOrder(sender, itemID, itemDur, buyAmount, buyPrice, con);
		closeSQLConnection(con);
		return value;
	}

	public int insertOrder(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount, Connection con) {
		int updateSuccessful = 0;
		String query = "SELECT *  FROM " + Config.sqlPrefix
			+ "Orders WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL  AND `price` = ?;";
		int id = 0;
		PreparedStatement statement;
		if (itemEnchants == null) {

			try {
				statement = con.prepareStatement(query);
				statement.setInt(1, type);
				statement.setBoolean(2, infinite);
				statement.setString(3, player);
				statement.setInt(4, itemID);
				statement.setInt(5, itemDur);
				statement.setDouble(6, price);

				ResultSet result = statement.executeQuery();

				while (result.next()) {
					id = result.getInt(1);
					break;
				}
				if (id > 0) {
					// plugin.info("Order already in table, changing amount...");

					increaseInt(Config.sqlPrefix + "Orders", id, "amount", amount, con);

				}
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		if (id > 0) {
			return 2;
		}

		query = "INSERT INTO "
			+ Config.sqlPrefix
			+ "Orders (`id`, `type`, `infinite`, `player`, `itemID`, `itemDur`, `itemEnchants`, `price`, `amount`, `exchanged`) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, 0);";
		try {
			statement = con.prepareStatement(query);
			statement.setInt(1, type);
			statement.setBoolean(2, infinite);
			statement.setString(3, player);
			statement.setInt(4, itemID);
			statement.setInt(5, itemDur);
			statement.setString(6, itemEnchants);
			statement.setDouble(7, price);
			statement.setInt(8, amount);

			updateSuccessful = statement.executeUpdate();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return updateSuccessful;
	}

	public int insertOrder(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount) {
		Connection con = getSQLConnection();
		int value = insertOrder(type, infinite, player, itemID, itemDur, itemEnchants, price, amount, con);
		closeSQLConnection(con);
		return value;
	}

	public void closeSQLConnection(Connection con) {
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String TypeToString(int type, boolean infinite) {
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

	public String TypeToString(int type) {
		return TypeToString(type, false);
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

	public static class itemStats {
		int total;
		double totalPrice;
		double totalAmount;
		double avgPrice;
		double mean;
		double median;
		double mode;
		double amean;
		double amedian;
		double amode;
	}

	// String SQL = "UPDATE " + tblAllegiance + " SET XP=XP+" + amount +
	// " WHERE `player` LIKE ?" + " AND `patron` LIKE ?";

	public int removeRow(String table, int rowID, Connection con) {
		int sucessful = 0;

		String query = "DELETE FROM " + table + " WHERE `id` = ?";

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, rowID);

			sucessful = statement.executeUpdate();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sucessful;
	}

	public int removeRow(String table, int rowID) {
		Connection con = getSQLConnection();
		int sucessful = removeRow(table, rowID);
		closeSQLConnection(con);
		return sucessful;
	}

	public int increaseInt(String table, int rowID, String column, int amount, Connection con) {
		int sucessful = 0;

		String query = "UPDATE " + table + " SET " + column + "=" + column + " + ? WHERE `id` = ?;";

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, amount);

			statement.setInt(2, rowID);

			sucessful = statement.executeUpdate();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sucessful;
	}

	public int increaseInt(String table, int rowID, String column, int amount) {
		Connection con = getSQLConnection();
		int sucessful = increaseInt(table, rowID, column, amount, con);
		closeSQLConnection(con);
		return sucessful;
	}

	public int decreaseInt(String table, int rowID, String column, int amount, Connection con) {
		int sucessful = 0;

		String query = "UPDATE " + table + " SET " + column + "=" + column + " - ? WHERE `id` = ?;";

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, amount);

			statement.setInt(2, rowID);

			sucessful = statement.executeUpdate();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sucessful;
	}

	public int decreaseInt(String table, int rowID, String column, int amount) {
		Connection con = getSQLConnection();
		int sucessful = decreaseInt(table, rowID, column, amount, con);
		closeSQLConnection(con);
		return sucessful;
	}

	public int setInt(String table, int rowID, String column, int value, Connection con) {
		int sucessful = 0;

		String query = "UPDATE " + table + " SET " + column + " = ? WHERE `id` = ?;";

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, value);

			statement.setInt(2, rowID);

			sucessful = statement.executeUpdate();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sucessful;
	}

	public int setInt(String table, int rowID, String column, int value) {
		Connection con = getSQLConnection();
		int sucessful = setInt(table, rowID, column, value, con);
		closeSQLConnection(con);
		return sucessful;
	}

	public itemStats getItemStats(int itemID, int itemDur, int getType) {
		itemStats myReturn = new itemStats();

		Connection con = getSQLConnection();
		String query = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL AND `amount` > 0";

		if (getType > 0) {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL AND `amount` > 0 AND `type` = ?;";
		}

		try {
			int i = 0, type, amount;
			double price, aPrice;
			String itemName;
			double totalPrice = 0;
			double totalAmount = 0;
			List<Double> prices = new ArrayList<Double>();
			List<Integer> amounts = new ArrayList<Integer>();

			PreparedStatement statement = con.prepareStatement(query);

			statement.setInt(1, itemID);
			statement.setInt(2, itemDur);

			if (getType > 0) {
				statement.setInt(3, getType);
			}

			ResultSet result = statement.executeQuery();

			while (result.next()) {
				// patron = result.getString(1);

				type = result.getInt(2);
				itemID = result.getInt(5);
				itemDur = result.getInt(6);
				price = result.getDouble(8);
				amount = result.getInt(9);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				aPrice = price * amount;// / amount;

				totalPrice += aPrice;
				totalAmount += amount;

				// prices[i] = aPrice;

				prices.add(price);
				amounts.add(amount);
				i += 1;

			}

			result.close();
			statement.close();
			myReturn.total = i;
			myReturn.totalAmount = totalAmount;

			double avgPrice = totalPrice / totalAmount;
			// plugin.info("totalAmount: "+totalAmount );
			// plugin.info("totalPrice: "+totalPrice );
			// plugin.info("i: "+i );
			// plugin.info("avgPrice: "+avgPrice );

			myReturn.avgPrice = avgPrice;

			myReturn.mean = 0;
			myReturn.median = 0;
			myReturn.mode = 0;

			if (prices.size() > 0) {
				double[] dPrices = new double[prices.size()];

				for (int i1 = 0; i1 < prices.size(); i1++)
					dPrices[i1] = prices.get(i1);

				// myReturn.mean = mean(dPrices);
				myReturn.median = median(dPrices);
				myReturn.mode = mode(dPrices);
			}
			if (amounts.size() > 0) {
				double[] dAmounts = new double[amounts.size()];

				for (int i1 = 0; i1 < amounts.size(); i1++)
					dAmounts[i1] = amounts.get(i1);

				myReturn.amean = mean(dAmounts);
				myReturn.amedian = median(dAmounts);
				myReturn.amode = mode(dAmounts);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		closeSQLConnection(con);

		return myReturn;
	}

	public int collectPenderingBuys(CommandSender sender) {
		Connection con = getSQLConnection();
		int value = collectPenderingBuys(sender, con);
		closeSQLConnection(con);
		return value;
	}

	public int collectPenderingBuys(CommandSender sender, Connection con) {
		int success = 0;

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `type` = 2 AND `player` LIKE ? AND `exchanged` > 0 ORDER BY `exchanged` DESC";

		Player player = (Player) sender;

		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);
			statement.setString(1, sender.getName());

			ResultSet result = statement.executeQuery();

			int id, type, itemID, mount, exchanged;
			short itemDur;
			double price;
			String itemName;
			Boolean infinite;
			String toCollect;
			while (result.next()) {
				count += 1;
				// patron = result.getString(1);
				id = result.getInt(1);
				type = result.getInt(2);
				infinite = result.getBoolean(3);
				itemID = result.getInt(5);
				itemDur = result.getShort(6);
				price = result.getDouble(8);
				// amount = result.getInt(9);
				exchanged = result.getInt(10);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				// plugin.sendMessage(sender, id + ": "+itemName+"x"+exchanged);
				if (exchanged > 0) {
					for (int i = exchanged; i >= 0; i--) {
						if (plugin.database.giveItemToPlayer(player, itemID, itemDur, i) == true) {
							plugin.sendMessage(sender, F("collectedItem", itemName, i));
							plugin.database.decreaseInt(Config.sqlPrefix + "Orders", id, "exchanged", i);
							break;
						}
					}
				}

			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		cleanBuyOrders();

		if (count == 0) {
			plugin.sendMessage(sender, L("nothingToCollect"));
		}

		return success;
	}

	public int listPlayerOrders(CommandSender sender, String trader) {
		int updateSuccessful = 0;

		Connection con = getSQLConnection();

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `player` LIKE ? ORDER BY id ASC;";// `amount`
																											// >
																											// 0
		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);
			statement.setString(1, trader);

			ResultSet result = statement.executeQuery();

			int id, type, itemID, itemDur, amount, exchanged;
			double price;
			String itemName;
			Boolean infinite;
			String toCollect;
			while (result.next()) {
				count += 1;
				// patron = result.getString(1);
				id = result.getInt(1);
				type = result.getInt(2);
				infinite = result.getBoolean(3);
				itemID = result.getInt(5);
				itemDur = result.getInt(6);
				price = result.getDouble(8);
				amount = result.getInt(9);
				exchanged = result.getInt(10);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				if (type == 2 && exchanged > 0) {

					toCollect = " " + F("toCollect", exchanged);
				} else {
					toCollect = "";
				}

				// .sendMessage(sender, F("playerOrder",
				// TypeToString(type,infinite), id, itemName,amount, price) +
				// toCollect);

				sender.sendMessage(F("playerOrder", TypeToString(type, infinite), id, itemName, amount, plugin.Round(amount * price, Config.priceRounding), plugin.Round(price,Config.priceRounding)) + toCollect);

			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (count == 0) {
			plugin.sendMessage(sender, L("noActiveOrders"));
		}

		closeSQLConnection(con);

		return updateSuccessful;
	}

	public int cancelOrder(CommandSender sender, int orderID, Connection con) {
		int updateSuccessful = 0;

		Player player = (Player) sender;

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `id` = ?";

		try {
			PreparedStatement statement = con.prepareStatement(SQL);

			statement.setInt(1, orderID);

			ResultSet result = statement.executeQuery();

			int type, itemID, amount, exchanged;
			short itemDur;
			double price;
			String itemName, trader;
			Boolean infinite;
			String enchants;

			while (result.next()) {
				// patron = result.getString(1);

				trader = result.getString(4);

				if (trader.equalsIgnoreCase(sender.getName())) {

					type = result.getInt(2);
					infinite = result.getBoolean(3);
					itemID = result.getInt(5);
					itemDur = result.getShort(6);
					enchants = result.getString(7);
					price = result.getDouble(8);
					amount = result.getInt(9);

					exchanged = result.getInt(10);
					itemName = plugin.itemdb.getItemName(itemID, itemDur);

					// plugin.sendMessage(sender, TypeToString(type) +
					// "] itemName: " + itemName + "x" + amount + " @ $" +
					// price);

					if (infinite == false) {
						if (type == 1) {// Sale, return items.
							ItemStack is = new ItemStack(itemID, 1);
							is.setDurability(itemDur);
							is.setAmount(amount);

							if (enchants != null && !enchants.equalsIgnoreCase("")) {
								is.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));
							}

							if (!InventoryUtil.fits(is, player.getInventory())) {
								plugin.sendMessage(sender, F("notEnoughInvSpace", itemName, amount));

								return 0;
							}

							InventoryUtil.add(is, player.getInventory());

							plugin.sendMessage(sender, F("returnedYourItem", itemName, amount));

						} else if (type == 2) {// Buy, return money.
							double money = price * amount;

							plugin.payPlayer(sender.getName(), money);
							plugin.sendMessage(sender, F("refundedYourMoney", money));

						}

					}
					updateSuccessful = removeRow(Config.sqlPrefix + "Orders", orderID, con);

					if (updateSuccessful > 0) {
						plugin.sendMessage(sender, F("canceledOrder", TypeToString(type, infinite), orderID, itemName, amount));
					}

					// plugin.info("cancelOrder updateSuccessful: " +
					// updateSuccessful);

				}
			}

			if (updateSuccessful == 0)
				plugin.sendMessage(sender, F("cannotCancelOrder", orderID));

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return updateSuccessful;
	}

	public int cancelOrder(CommandSender sender, int orderID) {
		Connection con = getSQLConnection();
		int value = cancelOrder(sender, orderID, con);
		closeSQLConnection(con);
		return value;
	}

	public int listOrders(CommandSender sender, Connection con) {
		int updateSuccessful = 0;

		String SQL = "SELECT *" + " FROM " + Config.sqlPrefix + "Orders ORDER BY id ASC;";
		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);

			ResultSet result = statement.executeQuery();

			int type, itemID, itemDur, amount, id;
			Boolean infinite;
			double price;
			String itemName;
			while (result.next()) {
				count += 1;
				// patron = result.getString(1);
				id = result.getInt(1);
				type = result.getInt(2);
				infinite = result.getBoolean(3);
				itemID = result.getInt(5);
				itemDur = result.getInt(6);
				price = result.getDouble(8);
				amount = result.getInt(9);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				sender.sendMessage(F("playerOrder", TypeToString(type, infinite), id, itemName, amount, price));

				// plugin.sendMessage(sender, );

			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (count == 0) {
			plugin.sendMessage(sender, L("noActiveList"));
		}

		return updateSuccessful;
	}

	public int listOrders(CommandSender sender) {
		Connection con = getSQLConnection();
		int value = listOrders(sender, con);
		closeSQLConnection(con);
		return value;
	}
}
