package com.cyprias.exchangemarket.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mindrot.jbcrypt.BCrypt;

import com.cyprias.Utils.InventoryUtil;
import com.cyprias.Utils.MaterialUtil;
import com.cyprias.exchangemarket.Config;
import com.cyprias.exchangemarket.Database;
import com.cyprias.exchangemarket.Database.itemStats;
import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.ItemDb;

public class MySQL {

	public MySQL(Database database) {
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(Config.sqlURL, Config.sqlUsername, Config.sqlPassword);
	}

	public static boolean tableExists(String tableName) throws SQLException {
		boolean exists = false;
		Connection con = getConnection();

		PreparedStatement statement = con.prepareStatement("show tables like '" + tableName + "'");
		ResultSet result = statement.executeQuery();

		result.last();
		if (result.getRow() != 0) {

			exists = true;
		}

		// //////
		result.close();
		statement.close();
		con.close();

		return exists;
	}

	private static String F(String string, Object... args) {
		return ExchangeMarket.F(string, args);
	}

	private static String L(String string) {
		return ExchangeMarket.L(string);
	}

	public static void init() throws SQLException {
		createTables();
	}

	public static void createTables() throws SQLException {
		Connection con = getConnection();

		if (tableExists(Config.sqlPrefix + "Orders") == false) {
			con.prepareStatement(
				"CREATE TABLE "
					+ Config.sqlPrefix
					+ "Orders (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `infinite` BOOLEAN NOT NULL DEFAULT '0' , `player` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NULL, `price` DOUBLE NOT NULL, `amount` INT NOT NULL) ENGINE = InnoDB")
				.executeUpdate();
		}

		if (tableExists(Config.sqlPrefix + "Transactions") == false) {
			con.prepareStatement(
				"CREATE TABLE "
					+ Config.sqlPrefix
					+ "Transactions"
					+ " (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `buyer` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NOT NULL, `amount` INT NOT NULL, `price` DOUBLE NOT NULL, `seller` VARCHAR(32) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE = InnoDB")
				.executeUpdate();
		}

		if (tableExists(Config.sqlPrefix + "Passwords") == false) {
			con.prepareStatement(
				"CREATE TABLE `"
					+ Config.sqlPrefix
					+ "Passwords` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `username` VARCHAR(32) NOT NULL, `hash` VARCHAR(64) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE (`username`))")
				.executeUpdate();
		} else if (tableFieldExists(Config.sqlPrefix + "Passwords", "salt") == true) {
			con.prepareStatement("ALTER TABLE `" + Config.sqlPrefix + "Passwords` DROP `salt`").executeUpdate();

		}

		if (tableExists(Config.sqlPrefix + "Mailbox") == false) {
			con.prepareStatement(
				"CREATE TABLE `"
					+ Config.sqlPrefix
					+ "Mailbox"
					+ "` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `player` VARCHAR(32) NOT NULL, `itemId` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchant` VARCHAR(16) NULL, `amount` INT NOT NULL, `time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE = InnoDB")
				.executeUpdate();
		}

		if (tableFieldExists(Config.sqlPrefix + "Orders", "exchanged") == true) {
			migrateExchangedToMailbox();
			con.prepareStatement("ALTER TABLE `" + Config.sqlPrefix + "Orders` DROP `exchanged`").executeUpdate();
		}

		con.close();
	}

	public static boolean tableFieldExists(String table, String field) throws SQLException {
		boolean found = false;
		Connection con = getConnection();

		PreparedStatement statement = con.prepareStatement("SELECT * FROM " + table + ";");

		ResultSet result = statement.executeQuery();
		ResultSetMetaData rsMetaData = result.getMetaData();
		int numberOfColumns = rsMetaData.getColumnCount();

		String columnName;
		for (int i = 1; i < numberOfColumns + 1; i++) {
			columnName = rsMetaData.getColumnName(i);
			if (columnName.equalsIgnoreCase(field)) {
				found = true;
				break;
			}
		}

		con.close();

		return found;
	}

	private static void migrateExchangedToMailbox() throws SQLException {
		Connection con = getConnection();

		String query = "SELECT * FROM `" + Config.sqlPrefix + "Orders" + "` WHERE `type` =2 AND `exchanged` >0";

		PreparedStatement statement = con.prepareStatement(query);

		ResultSet result = statement.executeQuery();

		String playerName, itemEnchant;
		int itemID, exchanged;
		short itemDur;
		while (result.next()) {
			playerName = result.getString(4);
			itemID = result.getInt(5);
			itemDur = result.getShort(6);
			itemEnchant = result.getString(7);
			// amount = result.getInt(9);
			exchanged = result.getInt(10);
			insertIntoMailbox(playerName, itemID, itemDur, itemEnchant, exchanged);

		}

		result.close();
		statement.close();

		con.close();
	}

	private static int insertIntoMailbox(String player, int itemID, int itemDur, String itemEnchant, int amount) throws SQLException {
		int success = 0;
		PreparedStatement statement = null;
		Connection con = getConnection();

		String query = "UPDATE `" + Config.sqlPrefix + "Mailbox"
			+ "` SET `amount` = `amount` + ?, `time` = CURRENT_TIMESTAMP WHERE `player` = ? AND `itemId` = ? AND `itemDur` = ? AND `itemEnchant` = ?";

		statement = con.prepareStatement(query);
		statement.setInt(1, amount);

		statement.setString(2, player);
		statement.setInt(3, itemID);
		statement.setInt(4, itemDur);
		if (itemEnchant == null) {
			statement.setObject(5, null);
		} else {
			statement.setString(5, itemEnchant);
		}

		success = statement.executeUpdate();
		statement.close();

		if (success == 0) {
			query = "INSERT INTO `" + Config.sqlPrefix + "Mailbox"
				+ "` (`id`, `player`, `itemId`, `itemDur`, `itemEnchant`, `amount`, `time`) VALUES (NULL, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);";

			statement = con.prepareStatement(query);
			statement.setString(1, player);
			statement.setInt(2, itemID);
			statement.setInt(3, itemDur);
			if (itemEnchant == null) {
				statement.setObject(4, null);
			} else {
				statement.setString(4, itemEnchant);
			}
			statement.setInt(5, amount);

			success = statement.executeUpdate();
			statement.close();
		}

		con.close();
		return success;
	}

	public static void cleanMailbox() throws SQLException {
		Connection con = getConnection();
		con.prepareStatement("DELETE FROM " + Config.sqlPrefix + "Mailbox WHERE `amount` <= 0").executeUpdate();
		con.close();
	}

	public static int insertOrder(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount)
		throws SQLException {
		Connection con = getConnection();

		if (increaseOrderAmount(type, infinite, player, itemID, itemDur, itemEnchants, price, amount) == true)
			return 2;

		int updateSuccessful = 0;
		// price = plugin.Round(price, Config.priceRounding);

		PreparedStatement statement;

		String query = "INSERT INTO " + Config.sqlPrefix
			+ "Orders (`id`, `type`, `infinite`, `player`, `itemID`, `itemDur`, `itemEnchants`, `price`, `amount`) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?);";
		statement = con.prepareStatement(query);
		statement.setInt(1, type);
		statement.setBoolean(2, infinite);
		statement.setString(3, player);
		statement.setInt(4, itemID);
		statement.setInt(5, itemDur);
		statement.setString(6, itemEnchants);
		statement.setDouble(7, price); // plugin.Round(Config.priceRounding
		statement.setInt(8, amount);

		updateSuccessful = statement.executeUpdate();
		statement.close();
		return updateSuccessful;
	}

	public static boolean increaseOrderAmount(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount)
		throws SQLException {
		int success;
		String query;

		if (itemEnchants != null) {
			query = "UPDATE "
				+ Config.sqlPrefix
				+ "Orders"
				+ " SET `amount` = `amount` + ? WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` = ? AND `price` = ?;";
			success = executeUpdate(query, amount, type, infinite, player, itemID, itemDur, itemEnchants, price);
		} else {
			query = "UPDATE "
				+ Config.sqlPrefix
				+ "Orders"
				+ " SET `amount` = `amount` + ? WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL  AND `price` = ?;";

			success = executeUpdate(query, amount, type, infinite, player, itemID, itemDur, price);
		}

		if (success > 0)
			return true;

		return false;
	}

	public static int executeUpdate(String query, Object... args) throws SQLException {
		Connection con = getConnection();
		int sucessful = 0;

		PreparedStatement statement = con.prepareStatement(query);
		int i = 0;
		for (Object a : args) {
			i += 1;
			statement.setObject(i, a);
		}
		sucessful = statement.executeUpdate();
		con.close();
		return sucessful;
	}

	public static queryReturn executeQuery(String query, Object... args) throws SQLException {
		Connection con = getConnection();

		queryReturn myreturn = null;// = new queryReturn();

		PreparedStatement statement = con.prepareStatement(query);

		int i = 0;
		for (Object a : args) {
			i += 1;
			// plugin.info("executeQuery "+i+": " + a);
			statement.setObject(i, a);
		}
		ResultSet result = statement.executeQuery();
		myreturn = new queryReturn(con, statement, result);

		return myreturn;
	}

	public static int removeOrder(CommandSender sender, int orderID) throws SQLException {
		Connection con = getConnection();
		int success = 0;

		String query = "DELETE FROM " + Config.sqlPrefix + "Orders WHERE `id` = ?";

		PreparedStatement statement = con.prepareStatement(query);

		statement.setInt(1, orderID);

		success = statement.executeUpdate();

		statement.close();

		con.close();
		return success;
	}

	public static int cancelOrder(CommandSender sender, int orderID) throws SQLException {
		int updateSuccessful = 0;

		Player player = (Player) sender;

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `id` = ?";

		Connection con = getConnection();
		PreparedStatement statement = con.prepareStatement(SQL);

		statement.setInt(1, orderID);

		ResultSet result = statement.executeQuery();

		int type, itemID, amount;
		short itemDur;
		double price;
		String itemName, trader;
		Boolean infinite;
		String itemEnchants;

		while (result.next()) {
			// patron = result.getString(1);

			trader = result.getString(4);

			if (trader.equalsIgnoreCase(sender.getName())) {

				type = result.getInt(2);
				infinite = result.getBoolean(3);
				itemID = result.getInt(5);
				itemDur = result.getShort(6);
				itemEnchants = result.getString(7);
				price = result.getDouble(8);
				amount = result.getInt(9);

				// exchanged = result.getInt(10);
				itemName = ItemDb.getItemName(itemID, itemDur);
				if (itemEnchants != null)
					itemName += "-" + itemEnchants;

				// plugin.sendMessage(sender, TypeToString(type) +
				// "] itemName: " + itemName + "x" + amount + " @ $" +
				// price);

				if (infinite == false) {
					if (type == 1) {// Sale, return items.
						int gave = ExchangeMarket.giveItemToPlayer(player, itemID, itemDur, itemEnchants, amount);
						if (gave > 0) {
							// plugin.sendMessage(sender,
							// F("collectedItem", itemName, i));
							decreaseInt(Config.sqlPrefix + "Orders", orderID, "amount", gave);

							ExchangeMarket.sendMessage(sender, F("returnedYourItem", itemName, gave));
							updateSuccessful = 1;
							break;
						}

					} else if (type == 2) {// Buy, return money.
						double money = price * amount;

						ExchangeMarket.payPlayer(sender.getName(), money);
						ExchangeMarket.sendMessage(sender, F("refundedYourMoney", ExchangeMarket.Round(money, Config.priceRounding)));

						updateSuccessful = removeRow(Config.sqlPrefix + "Orders", orderID);

						if (updateSuccessful > 0) {

							ExchangeMarket.sendMessage(sender, F("canceledOrder", Database.TypeToString(type, infinite), orderID, itemName, amount));
						}
					}

				}

			}
		}

		con.close();

		if (updateSuccessful == 0)
			ExchangeMarket.sendMessage(sender, F("cannotCancelOrder", orderID));

		return updateSuccessful;
	}

	public static int removeRow(String table, int rowID) throws SQLException {
		int sucessful = 0;

		String query = "DELETE FROM " + table + " WHERE `id` = ?";
		Connection con = getConnection();
		PreparedStatement statement = con.prepareStatement(query);
		statement.setInt(1, rowID);

		sucessful = statement.executeUpdate();
		con.close();

		return sucessful;
	}

	public static int decreaseInt(String table, int rowID, String column, int amount) throws SQLException {
		int sucessful = 0;
		Connection con = getConnection();
		String query = "UPDATE " + table + " SET " + column + "=" + column + " - ? WHERE `id` = ?;";

		PreparedStatement statement = con.prepareStatement(query);
		statement.setInt(1, amount);

		statement.setInt(2, rowID);

		sucessful = statement.executeUpdate();
		statement.close();
		con.close();
		return sucessful;
	}

	public static class queryReturn {
		Connection con;
		PreparedStatement statement;
		public ResultSet result;

		public queryReturn(Connection con, PreparedStatement statement, ResultSet result) {
			this.con = con;
			this.statement = statement;
			this.result = result;
		}

		public void close() throws SQLException {
			this.result.close();
			this.statement.close();
			this.con.close();
		}

	}

	public static int cancelOrders(CommandSender sender, int cType, int ciID, short ciDur, String itemEnchants, int cAmount, Boolean dryrun)
		throws SQLException {
		int changes = 0;
		String sortBy = "price DESC";
		if (cType == 2) {
			sortBy = "price ASC";
		}

		String query;

		queryReturn qReturn;
		if (itemEnchants == null) {
			query = "SELECT * FROM "
				+ Config.sqlPrefix
				+ "Orders WHERE `type` = ? AND `infinite` = 0 AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL AND `amount` > 0 ORDER BY "
				+ sortBy;

			qReturn = executeQuery(query, cType, sender.getName(), ciID, ciDur);
		} else {
			query = "SELECT * FROM "
				+ Config.sqlPrefix
				+ "Orders WHERE `type` = ? AND `infinite` = 0 AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` like ? AND `amount` > 0 ORDER BY "
				+ sortBy;

			qReturn = executeQuery(query, cType, sender.getName(), ciID, ciDur, itemEnchants);
		}

		Player player = (Player) sender;
		Boolean hasOrders = false;

		String itemName = ItemDb.getItemName(ciID, ciDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		int id, amount;
		Boolean infinite;
		Double price;
		int canBuy;

		while (qReturn.result.next()) {
			if (cAmount <= 0)
				break;
			hasOrders = true;
			// patron = result.getString(1);

			id = qReturn.result.getInt(1);
			infinite = qReturn.result.getBoolean(3);
			// trader = qReturn.result.getString(4);
			price = qReturn.result.getDouble(8);
			amount = qReturn.result.getInt(9);
			// enchants = qReturn.result.getString(7);

			canBuy = Math.min(amount, cAmount);

			if (canBuy > 0) {
				String preview = "";
				if (dryrun == true)
					preview = L("preview");

				if (cType == 1) {// Sale, return items.
					// ItemStack is = new ItemStack(ciID, 1);
					// is.setDurability(ciDur);

					// is.setAmount(canBuy);

					int gave;
					if (dryrun == true) {
						ExchangeMarket.sendMessage(sender, preview + F("returnedYourItem", itemName, canBuy));
						break;
					} else {
						gave = ExchangeMarket.giveItemToPlayer(player, ciID, ciDur, itemEnchants, canBuy);
						decreaseInt(Config.sqlPrefix + "Orders", id, "amount", gave);

						ExchangeMarket.sendMessage(sender, preview + F("returnedYourItem", itemName, gave));
						break;
					}

				} else if (cType == 2) {// Buy, return money.
					double money = price * canBuy;
					if (dryrun == false) {
						ExchangeMarket.payPlayer(sender.getName(), money);
						if (infinite == false) {
							decreaseInt(Config.sqlPrefix + "Orders", id, "amount", canBuy);
						}
					}

					ExchangeMarket.sendMessage(sender, preview + F("refundedYourMoney", ExchangeMarket.Round(money, Config.priceRounding)));

				}

				cAmount -= canBuy;

				changes += 1;
			}

		}

		qReturn.close();

		if (hasOrders == false)
			ExchangeMarket.sendMessage(sender, L("noActiveOrders"));

		return changes;
	}

	public static int processBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun)
		throws SQLException {
		int success = 0;
		int beforeAmount = buyAmount;

		buyAmount = checkSellOrders(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, dryrun, false);
		// plugin.info("buyAmountB: " + buyAmount);

		if (buyAmount != beforeAmount) {
			cleanSellOrders();
			return 1;
		}

		return success;
	}

	public static int checkSellOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun,
		Boolean silentFail) throws SQLException {

		String query;
		queryReturn qReturn;

		/*
		 * if (buyPrice == -1) { buyPrice = 999999; }
		 */

		if (itemEnchants == null) {
			query = "SELECT * FROM "
				+ Config.sqlPrefix
				+ "Orders WHERE `type` = 1 AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL AND `price` <= ? AND `amount` > 0 AND `player` NOT LIKE ? ORDER BY `price` ASC, `amount` ASC;";
			qReturn = executeQuery(query, itemID, itemDur, buyPrice, sender.getName());
		} else {
			query = "SELECT * FROM "
				+ Config.sqlPrefix
				+ "Orders WHERE `type` = 1 AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` like ? AND `price` <= ? AND `amount` > 0 AND `player` NOT LIKE ? ORDER BY `price` ASC, `amount` ASC;";

			qReturn = executeQuery(query, itemID, itemDur, itemEnchants, buyPrice, sender.getName());
		}
		String itemName = ItemDb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;

		int initialAmount = buyAmount;
		double price;
		int id, amount;
		double senderBalance;
		String trader;
		int canBuy;
		ItemStack itemStack;
		Boolean infinite;
		String enchants;
		int tAmount = 0;
		double tPrice = 0;
		Player player = (Player) sender;

		int totalFit = Database.getFitAmount(new ItemStack(itemID, 1), 64 * 9 * 4, player);

		String preview = "";
		if (dryrun == true)
			preview = L("preview");
		while (qReturn.result.next()) {

			amount = qReturn.result.getInt(9);
			if (amount <= 0)
				continue;

			id = qReturn.result.getInt(1);
			infinite = qReturn.result.getBoolean(3);
			trader = qReturn.result.getString(4);
			price = qReturn.result.getDouble(8);
			enchants = qReturn.result.getString(7);

			// plugin.info("processBuyOrder id: " + id + ", price: " + price
			// + ", amount: " + amount);

			senderBalance = ExchangeMarket.getBalance(sender.getName());
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

				if (enchants != null)
					itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));

				if (canBuy > totalFit) {
					canBuy = totalFit;

				}
				totalFit -= canBuy;

				if (canBuy <= 0)
					break;

				itemStack.setAmount(canBuy);

				int bought = 0;

				if (dryrun == false) {
					int gave = ExchangeMarket.giveItemToPlayer(player, itemID, itemDur, itemEnchants, canBuy);
					if (gave > 0) {
						bought = gave;

						ExchangeMarket.debtPlayer(sender.getName(), bought * price);

						if (infinite == false)
							ExchangeMarket.payPlayer(trader, bought * price);

						if (infinite == false) {
							decreaseInt(Config.sqlPrefix + "Orders", id, "amount", bought);

						}

						ExchangeMarket.notifySellerOfExchange(trader, itemID, itemDur, itemEnchants, bought, price, sender.getName(), dryrun);// buy

						if (Config.logTransactionsToDB == true)
							insertTransaction(1, sender.getName(), itemID, itemDur, enchants, bought, price, trader);
					}
				} else {
					bought = canBuy;
				}

				ExchangeMarket.info("canBuy: " + canBuy + ", bought: " + bought);
				if (bought > 0) {
					tAmount += bought;
					tPrice += price * bought;

					// if (dryrun == true) {
					ExchangeMarket.sendMessage(
						sender,
						F("foundItem", itemName, bought, ExchangeMarket.Round(price * bought, Config.priceRounding),
							ExchangeMarket.Round(price, Config.priceRounding), trader));
					buyAmount -= bought;
				}
			}

		}
		qReturn.close();

		if (tAmount > 0) {
			ExchangeMarket.sendMessage(
				sender,
				preview
					+ F("buyingItemsTotal", itemName, tAmount, ExchangeMarket.Round(tPrice, Config.priceRounding),
						ExchangeMarket.Round(tPrice / tAmount, Config.priceRounding)));

		} else if (silentFail == false) {
			if (initialAmount == buyAmount) {
				if (buyPrice > 0) {
					ExchangeMarket.sendMessage(
						sender,
						F("noSellersForBuyPrice", itemName, buyAmount, ExchangeMarket.Round(buyPrice * buyAmount, Config.priceRounding),
							ExchangeMarket.Round(buyPrice, Config.priceRounding)));
				} else {
					ExchangeMarket.sendMessage(sender, F("noSellersForBuy", itemName, buyAmount));
				}
			}

		}

		return buyAmount;
	}

	public static int cleanSellOrders() throws SQLException {
		String SQL = "DELETE FROM " + Config.sqlPrefix + "Orders WHERE `type` = 1 AND `amount` = 0;";
		int success = 0;
		Connection con = getConnection();
		PreparedStatement statement = con.prepareStatement(SQL);
		success = statement.executeUpdate();
		statement.close();
		return success;
	}

	public static int insertTransaction(int type, String buyer, int itemID, int itemDur, String itemEnchants, int amount, double price, String seller)
		throws SQLException {
		int reply = 0;
		String query = "INSERT INTO "
			+ Config.sqlPrefix
			+ "Transactions"
			+ " (`id`, `type`, `buyer`, `itemID`, `itemDur`, `itemEnchants`, `amount`, `price`, `seller`, `timestamp`) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);";

		Connection con = getConnection();
		PreparedStatement statement = null;

		statement = con.prepareStatement(query);
		statement.setInt(1, type);
		statement.setString(2, buyer);
		statement.setInt(3, itemID);
		statement.setInt(4, itemDur);
		if (itemEnchants == null)
			itemEnchants = "";
		statement.setString(5, itemEnchants);
		statement.setInt(6, amount);
		statement.setDouble(7, price);
		statement.setString(8, seller);

		reply = statement.executeUpdate();
		statement.close();

		con.close();

		return reply;
	}

	public static int processSellOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int sellAmount, double sellPrice, Boolean dryrun)
		throws SQLException {
		int success = 0;

		// plugin.info("sellAmountA: " + sellAmount);
		int beforeAmount = sellAmount;
		sellAmount = checkBuyOrders(sender, itemID, itemDur, itemEnchants, sellAmount, sellPrice, dryrun, false);

		if (dryrun == false)
			cleanSellOrders();

		if (beforeAmount != sellAmount)
			success = 1;

		return success;
	}

	public static int checkBuyOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int sellAmount, double sellPrice, Boolean dryrun,
		Boolean silentFail) throws SQLException {
		String query;
		queryReturn qReturn;

		if (itemEnchants == null) {
			query = "SELECT * FROM "
				+ Config.sqlPrefix
				+ "Orders WHERE `type` = 2 AND `itemID` = ? AND `itemDur` = ? AND `price` >= ? AND `amount` > 0 AND `player` NOT LIKE ? AND `itemEnchants` IS NULL ORDER BY `price` DESC, `amount` ASC";

			qReturn = executeQuery(query, itemID, itemDur, sellPrice, sender.getName());
		} else {
			query = "SELECT * FROM "
				+ Config.sqlPrefix
				+ "Orders WHERE `type` = 2 AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` like ? AND `price` >= ? AND `amount` > 0 AND `player` NOT LIKE ? ORDER BY `price` DESC, `amount` ASC";

			qReturn = executeQuery(query, itemID, itemDur, itemEnchants, sellPrice, sender.getName());
		}

		String itemName = ItemDb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		double price;
		int id, amount;
		String trader, enchants;
		Boolean infinite;
		Player player = (Player) sender;
		int initialAmount = sellAmount;
		while (qReturn.result.next()) {

			amount = qReturn.result.getInt(9);
			if (amount <= 0)
				continue;

			id = qReturn.result.getInt(1);
			infinite = qReturn.result.getBoolean(3);
			trader = qReturn.result.getString(4);
			price = qReturn.result.getDouble(8);
			enchants = qReturn.result.getString(7);

			if (infinite == true) {
				amount = sellAmount;
			} else {
				amount = Math.min(amount, sellAmount);
			}

			// plugin.info("amount: " + amount);

			// plugin.info("checkBuyOrders: " + id);

			String preview = "";
			if (dryrun == true)
				preview = L("preview");

			if (dryrun == true || Database.removeItemFromPlayer(player, ItemDb.getItemStack(itemID, itemDur, itemEnchants, amount)) == true) {
				sellAmount -= amount;

				if (dryrun == false) {
					ExchangeMarket.payPlayer(sender.getName(), amount * price);

					ExchangeMarket.sendMessage(sender, preview + F("withdrewItem", itemName, amount));
				}

				ExchangeMarket.notifySellerOfExchange(sender.getName(), itemID, itemDur, itemEnchants, amount, price, trader, dryrun);

				if (dryrun == false) {
					if (infinite == false) {
						decreaseInt(Config.sqlPrefix + "Orders", id, "amount", amount);
						// increaseInt(Config.sqlPrefix + "Orders", id,
						// "exchanged", amount);
						// Mailbox Todo

						insertIntoMailbox(trader, itemID, itemDur, enchants, amount);

					}

					ExchangeMarket.notifyBuyerOfExchange(trader, itemID, itemDur, amount, price, sender.getName(), dryrun);

					if (Config.logTransactionsToDB == true)
						insertTransaction(2, sender.getName(), itemID, itemDur, enchants, amount, price, trader);

				}

				// plugin.info("sellAmount: " + sellAmount);
			} else {
				ExchangeMarket.info("Could not remove " + itemName + "x" + amount + " from inv.");
			}
			if (sellAmount <= 0)
				break;

		}

		if (silentFail == false) {
			if (initialAmount == sellAmount) {
				if (sellPrice > 0) {

					ExchangeMarket.sendMessage(
						sender,
						F("noBuyersForSellPrice", itemName, sellAmount, ExchangeMarket.Round(sellPrice * sellAmount, Config.priceRounding),
							ExchangeMarket.Round(sellPrice, Config.priceRounding)));
				} else {
					ExchangeMarket.sendMessage(sender, F("noBuyersForSell", itemName, sellAmount));
				}
			}
		}

		return sellAmount;
	}

	public static void checkPendingBuys(CommandSender sender) throws SQLException {

		Connection con = getConnection();
		// String SQL = "SELECT * FROM " + Config.sqlPrefix +
		// "Orders WHERE `type` = 2 AND `player` LIKE ? AND `exchanged` > 0 ORDER BY `exchanged` DESC";
		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Mailbox WHERE `player` LIKE ? ORDER BY `amount` DESC";

		PreparedStatement statement = con.prepareStatement(SQL);
		statement.setString(1, sender.getName());

		ResultSet result = statement.executeQuery();

		int itemID, exchanged;
		short itemDur;
		String itemName, itemEnchants;
		while (result.next()) {

			itemID = result.getInt(3);
			itemDur = result.getShort(4);
			itemEnchants = result.getString(5);
			// amount = result.getInt(9);
			exchanged = result.getInt(6);

			itemName = ItemDb.getItemName(itemID, itemDur);

			if (itemEnchants != null)
				itemName += "-" + itemEnchants;

			// plugin.sendMessage(sender, id + ": "+itemName+"x"+exchanged);
			if (exchanged > 0)
				sender.sendMessage(ExchangeMarket.chatPrefix + F("collectPending", itemName, exchanged));

		}

		result.close();
		statement.close();

		con.close();
	}

	public static double getTradersLastPrice(int type, String trader, int itemID, short itemDur, String itemEnchants) throws SQLException {
		double price = 0;

		String query;
		queryReturn qReturn;

		if (itemEnchants == null) {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Orders WHERE `type` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ?  AND `itemEnchants` IS NULL ORDER BY `id` DESC LIMIT 0 , 1";
			qReturn = executeQuery(query, type, trader, itemID, itemDur);
		} else {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Orders WHERE `type` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ?  AND `itemEnchants` = ? ORDER BY `id` DESC LIMIT 0 , 1";
			qReturn = executeQuery(query, type, trader, itemID, itemDur, itemEnchants);
		}

		while (qReturn.result.next()) {
			price = qReturn.result.getDouble(8);
			break;
		}
		qReturn.close();

		return price;
	}

	public static double getUsersLastPrice(int type, String trader, int itemID, short itemDur, String itemEnchants) throws SQLException {
		double price = 0;

		String query;
		queryReturn qReturn;

		if (itemEnchants == null) {
			query = "SELECT * FROM "
				+ Config.sqlPrefix
				+ "Transactions WHERE `type` = ? AND `seller` LIKE ? AND `itemID` = ? AND `itemDur` = ?  AND `itemEnchants` IS NULL ORDER BY `id` DESC LIMIT 0 , 1";
			qReturn = executeQuery(query, type, trader, itemID, itemDur);
		} else {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Transactions WHERE `type` = ? AND `seller` LIKE ? AND `itemID` = ? AND `itemDur` = ?  AND `itemEnchants` = ? ORDER BY `id` DESC LIMIT 0 , 1";
			qReturn = executeQuery(query, type, trader, itemID, itemDur, itemEnchants);
		}

		while (qReturn.result.next()) {
			price = qReturn.result.getDouble(8);
			break;
		}
		qReturn.close();

		return price;
	}

	public static void postBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun)
		throws SQLException {
		int success = 0;

		String itemName = ItemDb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;

		if (buyAmount > 0) {

			/*
			 * if (buyPrice == -1) { buyPrice = getTradersLastPrice(2,
			 * sender.getName(), itemID, itemDur, itemEnchants);
			 * 
			 * if (buyPrice <= 0) { plugin.sendMessage(sender,
			 * L("mustSupplyAPrice")); return; } }
			 */

			if (ExchangeMarket.getBalance(sender.getName()) < (buyAmount * buyPrice)) {
				ExchangeMarket.sendMessage(
					sender,
					F("buyNotEnoughFunds", ExchangeMarket.Round(buyPrice * buyAmount, Config.priceRounding),
						ExchangeMarket.Round(buyPrice, Config.priceRounding)));
				ExchangeMarket.sendMessage(sender, L("failedToCreateOrder"));
				return;
			}

			buyAmount = checkSellOrders(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, false, true);

			if (buyAmount == 0)
				return;

			if (dryrun == false)
				success = insertOrder(2, false, sender.getName(), itemID, itemDur, itemEnchants, buyPrice, buyAmount);

			if (dryrun == true || success > 0) {
				String preview = "";
				if (dryrun == true)
					preview = L("preview");

				// createdBuyOrderEnchant

				ExchangeMarket.sendMessage(
					sender,
					F("createdBuyOrder", itemName, buyAmount, ExchangeMarket.Round(buyPrice * buyAmount, Config.priceRounding),
						ExchangeMarket.Round(buyPrice, Config.priceRounding)));

				if (dryrun == false) {
					ExchangeMarket.sendMessage(sender, preview + F("withdrewMoney", ExchangeMarket.Round(buyPrice * buyAmount, Config.priceRounding)));
					ExchangeMarket.debtPlayer(sender.getName(), buyAmount * buyPrice);

					if (success == 1)
						ExchangeMarket.announceNewOrder(2, sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice);
				}

			}
		}

		if (success == 0) {
			ExchangeMarket.sendMessage(sender, L("failedToCreateOrder"));
		}

	}

	public static int collectPendingBuys(CommandSender sender) throws SQLException {
		int success = 0;

		// String SQL = "SELECT * FROM " + Config.sqlPrefix +
		// "Orders WHERE `type` = 2 AND `player` LIKE ? AND `exchanged` > 0 ORDER BY `exchanged` DESC";
		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Mailbox WHERE `player` LIKE ? ORDER BY `amount` DESC";

		Player player = (Player) sender;

		int count = 0;
		Connection con = getConnection();
		PreparedStatement statement = con.prepareStatement(SQL);
		statement.setString(1, sender.getName());

		ResultSet result = statement.executeQuery();

		int id, itemID, amount;
		short itemDur;
		String itemName;
		String itemEnchants;
		while (result.next()) {
			count += 1;
			// id = result.getInt(1);
			// type = result.getInt(2);
			// infinite = result.getBoolean(3);

			// price = result.getDouble(8);
			// exchanged = result.getInt(10);

			id = result.getInt(1);
			itemID = result.getInt(3);
			itemDur = result.getShort(4);
			amount = result.getInt(6);

			itemEnchants = result.getString(5);

			itemName = ItemDb.getItemName(itemID, itemDur);
			if (itemEnchants != null)
				itemName += "-" + itemEnchants;

			// plugin.sendMessage(sender, id + ": "+itemName+"x"+exchanged);
			if (amount > 0) {
				int gave = ExchangeMarket.giveItemToPlayer(player, itemID, itemDur, itemEnchants, amount);
				if (gave > 0) {
					ExchangeMarket.sendMessage(sender, F("collectedItem", itemName, gave));
					decreaseInt(Config.sqlPrefix + "Mailbox", id, "amount", gave);
					break;
				}
			}

		}

		result.close();
		statement.close();
		con.close();

		cleanMailbox();

		if (count == 0) {
			ExchangeMarket.sendMessage(sender, L("nothingToCollect"));
		}

		return success;
	}

	public static int listOrders(CommandSender sender, int getType, int page) throws SQLException {
		String query;
		int rows = 0;
		if (getType > 0) {
			query = "SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0 AND `type` = ?";
			rows = getResultCount(query, getType);
		} else {
			query = "SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0";
			rows = getResultCount(query);
		}

		int maxPages = (int) Math.ceil((double) rows / Config.transactionsPerPage);

		if (page <= 0) {
			page = maxPages;
		}

		ExchangeMarket.sendMessage(sender, F("transactionPage", page, maxPages));

		int updateSuccessful = 0;

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0 ORDER BY id ASC LIMIT " + (Config.transactionsPerPage * (page - 1))
			+ ", " + Config.transactionsPerPage;

		if (getType > 0) {
			SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0 AND `type` = ? ORDER BY id ASC LIMIT "
				+ (Config.transactionsPerPage * (page - 1)) + ", " + Config.transactionsPerPage;
		}

		int count = 0;
		Connection con = getConnection();
		PreparedStatement statement = con.prepareStatement(SQL);

		if (getType > 0) {
			statement.setInt(1, getType);
		}

		ResultSet result = statement.executeQuery();

		short itemDur;
		int type, itemID, amount, id;
		Boolean infinite;
		double price;
		String itemName, trader, itemEnchants;
		while (result.next()) {
			count += 1;
			// patron = result.getString(1);
			id = result.getInt(1);
			type = result.getInt(2);
			infinite = result.getBoolean(3);
			trader = result.getString(4);
			itemID = result.getInt(5);
			itemDur = result.getShort(6);
			itemEnchants = result.getString(7);
			price = result.getDouble(8);
			amount = result.getInt(9);
			itemName = ItemDb.getItemName(itemID, itemDur);
			if (itemEnchants != null)
				itemName += "-" + itemEnchants;

			// plugin.info("id: " + id + ", type: " + type + ", infinite: "
			// + infinite + ", itemName: " + itemName + ", amount: " +
			// amount + ", price: " + price);

			String typeString = ChatColor.RED + Database.TypeToString(type, infinite);
			if (type == 2)
				typeString = ChatColor.GREEN + Database.TypeToString(type, infinite);

			sender.sendMessage(F("playerOrder", typeString, id, itemName, amount, ExchangeMarket.Round(amount * price, Config.priceRounding),
				ExchangeMarket.Round(price, Config.priceRounding), Database.ColourName(sender, trader)));

		}

		result.close();
		statement.close();

		if (count == 0) {
			ExchangeMarket.sendMessage(sender, L("noActiveList"));
		}

		return updateSuccessful;
	}

	public static int getResultCount(String query, Object... args) throws SQLException {
		queryReturn qReturn = executeQuery(query, args);
		int rows = 0;
		qReturn.result.first();
		rows = qReturn.result.getInt(1);

		qReturn.close();

		return rows;
	}

	public static int listPlayerOrders(CommandSender sender, String trader, int page) throws SQLException {
		int rows = getResultCount("SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `player` LIKE ?", sender.getName());

		int maxPages = (int) Math.ceil((double) rows / Config.transactionsPerPage);

		if (page <= 0) {
			page = maxPages;
		}

		ExchangeMarket.sendMessage(sender, F("transactionPage", page, maxPages));

		int updateSuccessful = 0;

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `player` LIKE ? ORDER BY id ASC LIMIT " + (Config.transactionsPerPage * (page - 1))
			+ ", " + Config.transactionsPerPage;// `amount`
		Connection con = getConnection(); // >
		// 0
		int count = 0;
		PreparedStatement statement = con.prepareStatement(SQL);
		statement.setString(1, trader);

		ResultSet result = statement.executeQuery();

		short itemDur;
		int id, type, itemID, amount;
		double price;
		String itemName, itemEnchants;
		Boolean infinite;
		while (result.next()) {
			count += 1;
			// patron = result.getString(1);
			id = result.getInt(1);
			type = result.getInt(2);
			infinite = result.getBoolean(3);
			itemID = result.getInt(5);
			itemDur = result.getShort(6);
			itemEnchants = result.getString(7);
			price = result.getDouble(8);
			amount = result.getInt(9);
			// exchanged = result.getInt(10);
			itemName = ItemDb.getItemName(itemID, itemDur);
			if (itemEnchants != null)
				itemName += "-" + itemEnchants;

			// .sendMessage(sender, F("playerOrder",
			// TypeToString(type,infinite), id, itemName,amount, price) +
			// toCollect);
			String typeString = ChatColor.RED + Database.TypeToString(type, infinite);
			if (type == 2)
				typeString = ChatColor.GREEN + Database.TypeToString(type, infinite);

			sender.sendMessage(F("playerOrder", typeString, id, itemName, amount, ExchangeMarket.Round(amount * price, Config.priceRounding),
				ExchangeMarket.Round(price, Config.priceRounding), Database.ColourName(sender, trader)));

		}

		result.close();
		statement.close();

		if (count == 0) {
			ExchangeMarket.sendMessage(sender, L("noActiveOrders"));
		}

		con.close();

		return updateSuccessful;
	}

	public static void searchOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int page) throws SQLException {
		int rows = 0;

		if (itemEnchants != null) {
			rows = getResultCount("SELECT COUNT(*) FROM " + Config.sqlPrefix
				+ "Orders WHERE `itemID` = ? AND `itemEnchants` = ? AND `itemDur` = ? AND amount > 0", itemID, itemDur, itemEnchants);
		} else {
			rows = getResultCount("SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `itemID` = ? AND `itemDur` = ? AND amount > 0", itemID, itemDur);
		}

		String itemName = ItemDb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;

		ExchangeMarket.sendMessage(sender, F("resultsForItem", rows, itemName));
		if (rows <= 0)
			return;

		int maxPages = (int) Math.ceil((double) rows / Config.transactionsPerPage);

		if (page <= 0) {
			page = maxPages;
		}

		ExchangeMarket.sendMessage(sender, F("transactionPage", page, maxPages));

		queryReturn qReturn;
		String query;
		if (itemEnchants != null) {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` = ? AND amount > 0 ORDER BY `price` ASC, `amount` ASC LIMIT "
				+ (Config.transactionsPerPage * (page - 1)) + ", " + Config.transactionsPerPage;
			qReturn = executeQuery(query, itemID, itemDur, itemEnchants);
		} else {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Orders WHERE `itemID` = ? AND `itemDur` = ? AND amount > 0 ORDER BY `price` ASC, `amount` ASC LIMIT "
				+ (Config.transactionsPerPage * (page - 1)) + ", " + Config.transactionsPerPage;
			qReturn = executeQuery(query, itemID, itemDur);
		}

		/**/

		int results = 0;
		int id, type, amount;
		Boolean infinite;
		String trader;
		Double price;

		while (qReturn.result.next()) {
			results += 1;
			id = qReturn.result.getInt(1);
			type = qReturn.result.getInt(2);
			infinite = qReturn.result.getBoolean(3);
			trader = qReturn.result.getString(4);
			itemEnchants = qReturn.result.getString(7);
			price = qReturn.result.getDouble(8);
			amount = qReturn.result.getInt(9);
			itemName = ItemDb.getItemName(itemID, itemDur);
			if (itemEnchants != null)
				itemName += "-" + itemEnchants;

			String typeString = ChatColor.RED + Database.TypeToString(type, infinite);
			if (type == 2)
				typeString = ChatColor.GREEN + Database.TypeToString(type, infinite);

			sender.sendMessage(F("playerOrder", typeString, id, itemName, amount, ExchangeMarket.Round(amount * price, Config.priceRounding),
				ExchangeMarket.Round(price, Config.priceRounding), Database.ColourName(sender, trader)));

		}

		if (results == 0)
			ExchangeMarket.sendMessage(sender, L("noActiveList"));

		qReturn.close();
	}

	public static void postSellOrder(CommandSender sender, ItemStack stock, double sellPrice, Boolean dryrun) throws SQLException {

		int success = 0;
		String itemName = ItemDb.getItemName(stock, true);

		String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(stock);
		
		int amount = stock.getAmount();

		Player player = (Player) sender;

		if (amount > 0) {
			// ItemStack itemStack = new ItemStack(itemID, 1);
			// itemStack.setDurability(itemDur);
			// itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(itemEnchants));

			if (InventoryUtil.getAmount(stock, player.getInventory()) <= 0) {
				ExchangeMarket.sendMessage(sender, F("noItemInInventory", itemName));
				ExchangeMarket.sendMessage(sender, L("failedToCreateOrder"));
				return;
			}

			amount = Math.min(amount, InventoryUtil.getAmount(stock, player.getInventory()));
			stock.setAmount(amount);

			// int success = plugin.processSellOrder(sender,
			// stock.getTypeId(), stock.getDurability(), amount, price, dryrun);

			amount = checkBuyOrders(sender, stock.getTypeId(), stock.getDurability(), itemEnchants, amount, sellPrice, false, true);
			ExchangeMarket.info("C " + amount);
			if (amount == 0)
				return;

			if (dryrun == false)
				success = insertOrder(1, false, sender.getName(), stock.getTypeId(), stock.getDurability(), itemEnchants, sellPrice, amount);

			if (success > 0 || dryrun == true) {
				String preview = "";
				if (dryrun == true)
					preview = L("preview");

				ExchangeMarket.sendMessage(sender, preview + F("withdrewItem", itemName, amount));

				ExchangeMarket.sendMessage(
					sender,
					preview
						+ F("createdSellOrder", itemName, amount, ExchangeMarket.Round(amount * sellPrice, Config.priceRounding),
							ExchangeMarket.Round(sellPrice, Config.priceRounding)));
				// plugin.debtPlayer(sender.getName(), sellPrice * sellPrice);
				ExchangeMarket.info("E " + amount);

				if (dryrun == false) {
					InventoryUtil.remove(stock, player.getInventory());
					if (success == 1)
						ExchangeMarket.announceNewOrder(1, sender, stock.getTypeId(), stock.getDurability(), itemEnchants, amount, sellPrice);
				}
			}

		}
		if (success == 0)
			ExchangeMarket.sendMessage(sender, L("failedToCreateOrder"));

	}

	/*
	 * ABC
	 * 
	 * v
	 */
	public static void setPassword(CommandSender sender, String password) throws SQLException {
		String hash = BCrypt.hashpw(password, BCrypt.gensalt());
		String table = Config.sqlPrefix + "Passwords";

		Connection con = getConnection();
		PreparedStatement statement = null;
		int success = 0;

		String query = "UPDATE `" + table + "` SET `hash` = ?, `timestamp` = CURRENT_TIMESTAMP WHERE `username` = ? ;";

		statement = con.prepareStatement(query);
		statement.setString(1, hash);
		statement.setString(2, sender.getName());

		success = statement.executeUpdate();
		statement.close();

		if (success == 0) {
			query = "INSERT INTO `" + table + "` (`id`, `username`, `hash`, `timestamp`) VALUES (NULL, ?, ?, CURRENT_TIMESTAMP);";
			statement = con.prepareStatement(query);
			statement.setString(1, sender.getName());
			statement.setString(2, hash);

			success = statement.executeUpdate();
			statement.close();
			con.close();
		}

		if (success > 0) {
			ExchangeMarket.sendMessage(sender, L("savePassSuccessful"));
		} else {
			ExchangeMarket.sendMessage(sender, L("savePassFailed"));
		}

		con.close();

	}

	public static itemStats getItemStats(int itemID, int itemDur, String itemEnchants, int getType) throws SQLException {
		itemStats myReturn = new itemStats();

		String query = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `amount` > 0";
		queryReturn qReturn;

		if (itemEnchants != null && getType > 0) {
			query += " AND `itemEnchants` like ?";
			query += " AND `type` like ?";
			qReturn = executeQuery(query, itemID, itemDur, itemEnchants, getType);
		} else if (itemEnchants != null) {
			query += " AND `itemEnchants` like ?";
			qReturn = executeQuery(query, itemID, itemDur, itemEnchants);
		} else if (getType > 0) {
			query += " AND `type` like ?";
			qReturn = executeQuery(query, itemID, itemDur, getType);
		} else {
			qReturn = executeQuery(query, itemID, itemDur);
		}

		int i = 0, amount;
		double price, aPrice;
		double totalPrice = 0;
		double totalAmount = 0;
		List<Double> prices = new ArrayList<Double>();
		List<Integer> amounts = new ArrayList<Integer>();

		while (qReturn.result.next()) {
			itemID = qReturn.result.getInt(5);
			itemDur = qReturn.result.getInt(6);
			price = qReturn.result.getDouble(8);
			amount = qReturn.result.getInt(9);

			aPrice = price * amount;// / amount;

			totalPrice += aPrice;
			totalAmount += amount;

			// prices[i] = aPrice;

			prices.add(price);
			amounts.add(amount);
			i += 1;

		}

		qReturn.result.close();
		qReturn.statement.close();
		qReturn.con.close();

		myReturn.total = i;
		myReturn.totalAmount = totalAmount;

		double avgPrice = totalPrice / totalAmount;

		myReturn.avgPrice = avgPrice;

		myReturn.mean = 0;
		myReturn.median = 0;
		myReturn.mode = 0;

		if (prices.size() > 0) {
			double[] dPrices = new double[prices.size()];

			for (int i1 = 0; i1 < prices.size(); i1++)
				dPrices[i1] = prices.get(i1);

			myReturn.median = Database.median(dPrices);
			myReturn.mode = Database.mode(dPrices);
		}
		if (amounts.size() > 0) {
			double[] dAmounts = new double[amounts.size()];

			for (int i1 = 0; i1 < amounts.size(); i1++)
				dAmounts[i1] = amounts.get(i1);

			myReturn.amean = Database.mean(dAmounts);
			myReturn.amedian = Database.median(dAmounts);
			myReturn.amode = Database.mode(dAmounts);
		}
		qReturn.close();

		return myReturn;
	}

	public static void listPlayerTransactions(CommandSender sender, int page) throws SQLException {
		String query = "SELECT COUNT(*) FROM " + Config.sqlPrefix + "Transactions" + " WHERE `seller` LIKE ?";

		Connection con = getConnection();
		int rows = 0;
		PreparedStatement statement = con.prepareStatement(query);
		statement.setString(1, sender.getName());
		ResultSet result = statement.executeQuery();
		while (result.next()) {
			rows = (result.getInt(1) - 1);
		}

		// int Config.transactionsPerPage = Config.transactionsPerPage;
		int maxPages = (int) Math.floor(rows / Config.transactionsPerPage);

		if (page < 0) {
			page = maxPages;
		} else {
			page -= 1;
		}

		ExchangeMarket.sendMessage(sender, F("transactionPage", page + 1, maxPages + 1));

		query = "SELECT * FROM " + Config.sqlPrefix + "Transactions" + " WHERE `seller` LIKE ? LIMIT " + (Config.transactionsPerPage * page) + ", "
			+ Config.transactionsPerPage;

		boolean found = false;

		statement = con.prepareStatement(query);
		statement.setString(1, sender.getName());

		// statement.setString(3, sender.getName());

		// statement.setDouble(3, buyPrice);

		result = statement.executeQuery();

		double price;
		short itemDur;
		int type, itemID, amount;
		String buyer, itemEnchants, itemName;
		Timestamp timestamp;

		while (result.next()) {
			found = true;

			type = result.getInt(2);
			buyer = result.getString(3);
			itemID = result.getInt(4);
			itemDur = result.getShort(5);
			itemEnchants = result.getString(6);
			amount = result.getInt(7);
			price = result.getDouble(8);
			timestamp = result.getTimestamp(10);

			itemName = ItemDb.getItemName(itemID, itemDur);
			if (itemEnchants != null && !itemEnchants.equalsIgnoreCase(""))
				itemName += "-" + itemEnchants;

			String date = new SimpleDateFormat("MM/dd/yy").format(timestamp);

			if (type == 1) {
				sender.sendMessage(F("transactionMsgSold", itemName, amount, buyer, ExchangeMarket.Round(price * amount, Config.priceRounding), date));
				// timestamp.toString()

			} else {
				sender.sendMessage(F("transactionMsgBought", itemName, amount, buyer, ExchangeMarket.Round(price * amount, Config.priceRounding), date));

			}
		}
		// }

		if (found == false)
			ExchangeMarket.sendMessage(sender, L("noTransactions"));

	}
}
