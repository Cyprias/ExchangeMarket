package com.cyprias.exchangemarket;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mindrot.jbcrypt.BCrypt;

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
		// MySQL = new MySQL(this);

	}

	public boolean testDBConnection() {
		try {
			Connection con;
			if (Config.sqlURL.contains("mysql")) {
				con = DriverManager.getConnection(Config.sqlURL, Config.sqlUsername, Config.sqlPassword);
			} else {
				con = DriverManager.getConnection(Config.sqlURL);
			}

			con.close();
			return true;
		} catch (SQLException e) {
		}
		return false;
	}

	public static Connection getSQLConnection() {
		try {
			Connection con;
			if (Config.sqlURL.contains("mysql")) {
				con = DriverManager.getConnection(Config.sqlURL, Config.sqlUsername, Config.sqlPassword);
			} else {
				con = DriverManager.getConnection(Config.sqlURL);
			}

			return con;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public boolean tableExists(String tableName) {
		boolean exists = false;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = getSQLConnection();

			String query;

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

		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return exists;
	}

	public void setupMysql() {
		String query;

		Connection con = getSQLConnection();
		PreparedStatement statement = null;

		try {

			if (tableExists(Config.sqlPrefix + "Orders") == false) {
				query = "CREATE TABLE "
					+ Config.sqlPrefix
					+ "Orders (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `infinite` BOOLEAN NOT NULL DEFAULT '0' , `player` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NULL, `price` DOUBLE NOT NULL, `amount` INT NOT NULL)";
				statement = con.prepareStatement(query);
				statement.executeUpdate();
			}

			if (tableExists(Config.sqlPrefix + "Transactions") == false) {
				query = "CREATE TABLE "
					+ Config.sqlPrefix
					+ "Transactions"
					+ " (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `buyer` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NOT NULL, `amount` INT NOT NULL, `price` DOUBLE NOT NULL, `seller` VARCHAR(32) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";
				statement = con.prepareStatement(query);
				statement.executeUpdate();
			}

			if (tableExists(Config.sqlPrefix + "Passwords") == false) {

				query = "CREATE TABLE `"
					+ Config.sqlPrefix
					+ "Passwords` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `username` VARCHAR(32) NOT NULL, `hash` VARCHAR(64) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE (`username`))";
				statement = con.prepareStatement(query);
				statement.executeUpdate();
			} else if (tableFieldExists(Config.sqlPrefix + "Passwords", "salt") == true) {
				query = "ALTER TABLE `" + Config.sqlPrefix + "Passwords` DROP `salt`";
				statement = con.prepareStatement(query);
				statement.executeUpdate();
			}

			if (tableExists(Config.sqlPrefix + "Mailbox") == false) {
				query = "CREATE TABLE `"
					+ Config.sqlPrefix
					+ "Mailbox"
					+ "` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `player` VARCHAR(32) NOT NULL, `itemId` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchant` VARCHAR(16) NULL, `amount` INT NOT NULL, `time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE = InnoDB";
				statement = con.prepareStatement(query);
				statement.executeUpdate();
			}
			if (tableFieldExists(Config.sqlPrefix + "Orders", "exchanged") == true) {

				migrateExchangedToMailbox();
				/**/
				query = "ALTER TABLE `" + Config.sqlPrefix + "Orders` DROP `exchanged`";
				statement = con.prepareStatement(query);
				statement.executeUpdate();

			}

			// statement.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void cleanMailbox() {
		Connection con = getSQLConnection();
		String SQL = "DELETE FROM " + Config.sqlPrefix + "Mailbox WHERE `amount` <= 0";

		try {
			PreparedStatement statement = con.prepareStatement(SQL);
			statement.executeUpdate();
			statement.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeSQLConnection(con);
	}

	private void migrateExchangedToMailbox() {
		plugin.info("Migrating pending collection items to Mailbox table...");
		Connection con = getSQLConnection();

		String SQL = "SELECT * FROM `" + Config.sqlPrefix + "Orders" + "` WHERE `type` =2 AND `exchanged` >0";

		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);

			ResultSet result = statement.executeQuery();

			String playerName, itemEnchant;
			int itemID, exchanged;
			short itemDur;
			String itemName;
			while (result.next()) {
				count += 1;

				playerName = result.getString(4);
				itemID = result.getInt(5);
				itemDur = result.getShort(6);
				itemEnchant = result.getString(7);
				// amount = result.getInt(9);
				exchanged = result.getInt(10);
				itemName = ItemDb.getItemName(itemID, itemDur);

				insertIntoMailbox(playerName, itemID, itemDur, itemEnchant, exchanged);

			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		closeSQLConnection(con);
	}

	private int insertIntoMailbox(String player, int itemID, int itemDur, String itemEnchant, int amount) {
		int success = 0;
		PreparedStatement statement = null;
		Connection con = getSQLConnection();

		String query = "UPDATE `" + Config.sqlPrefix + "Mailbox"
			+ "` SET `amount` = `amount` + ?, `time` = CURRENT_TIMESTAMP WHERE `player` = ? AND `itemId` = ? AND `itemDur` = ? AND `itemEnchant` = ?";

		try {
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

			query = "INSERT INTO `SomeMCable` (`id`, `forumID`, `mcName`) VALUES (NULL, '39179' 'Cyprias')";

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

		} catch (SQLException e) {
			e.printStackTrace();
		}

		closeSQLConnection(con);
		return success;
	}

	public boolean tableFieldExists(String table, String field) {
		boolean found = false;
		Connection con = getSQLConnection();
		String query = "SELECT * FROM " + table + ";";

		try {
			PreparedStatement statement = con.prepareStatement(query);

			ResultSet result = statement.executeQuery();
			ResultSetMetaData rsMetaData = result.getMetaData();
			int numberOfColumns = rsMetaData.getColumnCount();

			String columnName;
			for (int i = 1; i < numberOfColumns + 1; i++) {
				columnName = rsMetaData.getColumnName(i);
				if (columnName.equalsIgnoreCase(field)) {
					// plugin.info("offline XP  world found.");
					found = true;
					break;
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closeSQLConnection(con);
		return found;
	}

	public void listPlayerTransactions(CommandSender sender, int page) {
		String query = "SELECT COUNT(*) FROM " + Config.sqlPrefix + "Transactions" + " WHERE `seller` LIKE ?";

		Connection con = getSQLConnection();
		int rows = 0;
		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setString(1, sender.getName());
			ResultSet result = statement.executeQuery();
			while (result.next()) {
				rows = (result.getInt(1) - 1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// int Config.transactionsPerPage = Config.transactionsPerPage;
		int maxPages = (int) Math.floor(rows / Config.transactionsPerPage);

		if (page < 0) {
			page = maxPages;
		} else {
			page -= 1;
		}

		plugin.sendMessage(sender, F("transactionPage", page + 1, maxPages + 1));

		query = "SELECT * FROM " + Config.sqlPrefix + "Transactions" + " WHERE `seller` LIKE ? LIMIT " + (Config.transactionsPerPage * page) + ", "
			+ Config.transactionsPerPage;

		boolean found = false;
		try {

			PreparedStatement statement = con.prepareStatement(query);
			statement.setString(1, sender.getName());

			// statement.setString(3, sender.getName());

			// statement.setDouble(3, buyPrice);

			ResultSet result = statement.executeQuery();

			double price;
			int type, itemID, itemDur, amount;
			String buyer, itemEnchants, seller;
			Timestamp timestamp;

			while (result.next()) {
				found = true;

				type = result.getInt(2);
				buyer = result.getString(3);
				itemID = result.getInt(4);
				itemDur = result.getInt(5);
				itemEnchants = result.getString(6);
				amount = result.getInt(7);
				price = result.getDouble(8);
				timestamp = result.getTimestamp(10);

				String itemName = plugin.itemdb.getItemName(itemID, itemDur);

				String date = new SimpleDateFormat("MM/dd/yy").format(timestamp);

				if (type == 1) {
					sender.sendMessage(F("transactionMsgSold", itemName, amount, buyer, plugin.Round(price * amount, Config.priceRounding), date));
					// timestamp.toString()

				} else {
					sender.sendMessage(F("transactionMsgBought", itemName, amount, buyer, plugin.Round(price * amount, Config.priceRounding), date));

				}
			}
			// }
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (found == false)
			plugin.sendMessage(sender, L("noTransactions"));

	}

	public void setPassword(CommandSender sender, String password) {
		String hash = BCrypt.hashpw(password, BCrypt.gensalt());
		String table = Config.sqlPrefix + "Passwords";

		Connection con = getSQLConnection();
		PreparedStatement statement = null;
		int success = 0;

		String query = "UPDATE `" + table + "` SET `hash` = ?, `timestamp` = CURRENT_TIMESTAMP WHERE `username` = ? ;";

		try {
			statement = con.prepareStatement(query);
			statement.setString(1, hash);
			statement.setString(2, sender.getName());

			success = statement.executeUpdate();
			statement.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (success == 0) {
			query = "INSERT INTO `" + table + "` (`id`, `username`, `hash`, `timestamp`) VALUES (NULL, ?, ?, CURRENT_TIMESTAMP);";
			try {
				statement = con.prepareStatement(query);
				statement.setString(1, sender.getName());
				statement.setString(2, hash);

				success = statement.executeUpdate();
				statement.close();
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (success > 0) {
			plugin.sendMessage(sender, L("savePassSuccessful"));
		} else {
			plugin.sendMessage(sender, L("savePassFailed"));
		}

		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public int insertTransaction(int type, String buyer, int itemID, int itemDur, String itemEnchants, int amount, double price, String seller) {
		int reply = 0;
		String query = "INSERT INTO "
			+ Config.sqlPrefix
			+ "Transactions"
			+ " (`id`, `type`, `buyer`, `itemID`, `itemDur`, `itemEnchants`, `amount`, `price`, `seller`, `timestamp`) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);";

		Connection con = getSQLConnection();
		PreparedStatement statement = null;

		try {
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
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return reply;
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

	public boolean giveItemToPlayer(Player player, int itemID, short itemDur, String enchants, int amount) {
		ItemStack itemStack = new ItemStack(itemID, 1);
		itemStack.setDurability(itemDur);
		itemStack.setAmount(amount);

		plugin.info("giveItemToPlayer1: enchants: " + enchants);

		if (enchants != null && !enchants.equalsIgnoreCase("")) {
			plugin.info("giveItemToPlayer2: enchants: " + enchants);
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));
		}

		if (InventoryUtil.fits(itemStack, player.getInventory())) {
			InventoryUtil.add(itemStack, player.getInventory());
			return true;
		}
		return false;
	}

	/*
	 * public boolean giveItemToPlayer(Player player, int itemID, short itemDur,
	 * int amount) { return giveItemToPlayer(player, itemID, itemDur, amount,
	 * null); }
	 */

	public int checkBuyOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int sellAmount, double sellPrice, Boolean dryrun,
		Connection con, Boolean silentFail) {
		String query = "SELECT * FROM "
			+ Config.sqlPrefix
			+ "Orders WHERE `type` = 2 AND `itemID` = ? AND `itemDur` = ? AND `price` >= ? AND `amount` > 0 AND `player` NOT LIKE ? AND `itemEnchants` like ? ORDER BY `price` DESC";
		// AND `player` NOT LIKE ?
		int updateSuccessful = 0;
		String itemName = plugin.itemdb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		
		Player player = (Player) sender;

		int found = 0;
		int initialAmount = sellAmount;

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, itemID);
			statement.setInt(2, itemDur);

			if (sellPrice == -1) {
				statement.setDouble(3, 0);
			} else {
				statement.setDouble(3, plugin.Round(sellPrice, Config.priceRounding));
			}

			statement.setString(4, sender.getName());

			if (itemEnchants == null) {
				statement.setObject(5, null);
			} else {
				statement.setString(5, itemEnchants);
			}

			ResultSet result = statement.executeQuery();

			double price;
			int id, amount;
			double senderBalance;
			String trader, enchants;
			int canSell = 0;
			Boolean infinite;
			while (result.next()) {
				amount = result.getInt(9);
				if (amount <= 0)
					continue;

				// patron = result.getString(1);
				found += 1;

				id = result.getInt(1);
				infinite = result.getBoolean(3);
				trader = result.getString(4);
				price = result.getDouble(8);
				enchants = result.getString(7);

				if (infinite == true) {
					amount = sellAmount;
				} else {
					amount = Math.min(amount, sellAmount);
				}

				// plugin.info("amount: " + amount);

				String preview = "";
				if (dryrun == true)
					preview = L("preview");

				if (dryrun == true || removeItemFromPlayer(player, itemID, itemDur, amount) == true) {
					sellAmount -= amount;

					if (dryrun == false) {
						plugin.payPlayer(sender.getName(), amount * price);

						plugin.sendMessage(sender, preview + F("withdrewItem", itemName, amount));
					}

					plugin.notifySellerOfExchange(sender.getName(), itemID, itemDur, amount, price, trader, dryrun);

					if (dryrun == false) {
						if (infinite == false) {
							decreaseInt(Config.sqlPrefix + "Orders", id, "amount", amount);
							// increaseInt(Config.sqlPrefix + "Orders", id,
							// "exchanged", amount);
							// Mailbox Todo

							insertIntoMailbox(trader, itemID, itemDur, enchants, amount);

						}

						plugin.notifyBuyerOfExchange(trader, itemID, itemDur, amount, price, sender.getName(), dryrun);

						if (Config.logTransactionsToDB == true)
							insertTransaction(2, sender.getName(), itemID, itemDur, enchants, amount, price, trader);

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
		if (silentFail == false) {
			
			if (found == 0) {
				// String itemName = plugin.itemdb.getItemName(itemID, itemDur);
				plugin.sendMessage(sender, F("noBuyersForSell", itemName));
				
				
			} else if (found > 0 && initialAmount == sellAmount) {
				plugin.sendMessage(sender, F("noBuyersForSellPrice", itemName, sellAmount, sellPrice * sellAmount, sellPrice));
			}
		}
		return sellAmount;
	}

	public void postSellOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int sellAmount, double sellPrice, Boolean dryrun) {
		Connection con = getSQLConnection();
		postSellOrder(sender, itemID, itemDur, itemEnchants, sellAmount, sellPrice, dryrun, con);
		closeSQLConnection(con);
	}

	public void postSellOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int sellAmount, double sellPrice, Boolean dryrun,
		Connection con) {
		int success = 0;
		String itemName = plugin.itemdb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		
		Player player = (Player) sender;

		if (sellAmount > 0) {

			if (sellPrice == -1) {

				sellPrice = getTradersLastPrice(sender.getName(), itemID, itemDur, 1);

				if (sellPrice <= 0) {
					plugin.sendMessage(sender, L("mustSupplyAPrice"));

					if (success == 0)
						plugin.sendMessage(sender, L("failedToCreateOrder"));

					return;
				}

			}

			ItemStack itemStack = new ItemStack(itemID, 1);
			itemStack.setDurability(itemDur);
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(itemEnchants));

			// itemEnchants 123333

			if (InventoryUtil.getAmount(itemStack, player.getInventory()) <= 0) {

				plugin.sendMessage(sender, F("noItemInInventory", itemName));
				plugin.sendMessage(sender, L("failedToCreateOrder"));
				return;
			}

			sellAmount = Math.min(sellAmount, InventoryUtil.getAmount(itemStack, player.getInventory()));
			itemStack.setAmount(sellAmount);

			// int success = plugin.database.processSellOrder(sender,
			// stock.getTypeId(), stock.getDurability(), amount, price, dryrun);

			sellAmount = checkBuyOrders(sender, itemID, itemDur, itemEnchants, sellAmount, sellPrice, false, con, true);

			if (sellAmount == 0)
				return;

			if (dryrun == false)
				success = insertOrder(1, false, sender.getName(), itemID, itemDur, itemEnchants, sellPrice, sellAmount);

			if (success > 0 || dryrun == true) {
				String preview = "";
				if (dryrun == true)
					preview = L("preview");

				plugin.sendMessage(sender, preview + F("withdrewItem", itemName, sellAmount));

				
				
				plugin.sendMessage(
					sender,
					preview
						+ F("createdSellOrder", itemName, sellAmount, plugin.Round(sellAmount * sellPrice, Config.priceRounding),
							plugin.Round(sellPrice, Config.priceRounding)));
				// plugin.debtPlayer(sender.getName(), sellPrice * sellPrice);
				if (dryrun == false) {
					InventoryUtil.remove(itemStack, player.getInventory());

					if (success == 1)
						plugin.announceNewOrder(1, sender, itemID, itemDur, itemEnchants, sellAmount, sellPrice);
				}
			}

		}
		if (success == 0)
			plugin.sendMessage(sender, L("failedToCreateOrder"));

	}

	public int processSellOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int sellAmount, double sellPrice, Boolean dryrun) {
		Connection con = getSQLConnection();
		int success = 0;

		// plugin.info("sellAmountA: " + sellAmount);
		int beforeAmount = sellAmount;
		sellAmount = checkBuyOrders(sender, itemID, itemDur, itemEnchants, sellAmount, sellPrice, dryrun, con, false);

		if (beforeAmount != sellAmount)
			return 1;
		// success = 1;

		// plugin.info("sellAmountB: " + sellAmount);

		if (dryrun == false)
			cleanSellOrders(con);

		return success;
	}

	public int cleanBuyOrders() {
		Connection con = getSQLConnection();
		int value = cleanBuyOrders(con);
		closeSQLConnection(con);
		return value;
	}

	public int cleanBuyOrders(Connection con) {
		String SQL = "DELETE FROM " + Config.sqlPrefix + "Orders WHERE `type` = 2 AND `amount` <= 0";
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

	public int getFitAmount(ItemStack itemStack, int amount, Player player) {
		// int amount = itemStack.getAmount();
		//

		for (int i = amount; i > 0; i--) {
			itemStack.setAmount(i);
			if (InventoryUtil.fits(itemStack, player.getInventory()) == true) {
				return i;
			}
		}

		return 0;
	}

	public int checkSellOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun,
		Connection con, Boolean silentFail) {

		String query = "SELECT * FROM "
			+ Config.sqlPrefix
			+ "Orders WHERE `type` = 1 AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` = ? AND `price` <= ? AND `amount` > 0 AND `player` NOT LIKE ? ORDER BY `price` ASC";

		int updateSuccessful = 0;
		String itemName = plugin.itemdb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		
		Player player = (Player) sender;
		int found = 0;
		int initialAmount = buyAmount;
		int tAmount = 0;
		double tPrice = 0;

		String preview = "";
		if (dryrun == true)
			preview = L("preview");

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, itemID);
			statement.setInt(2, itemDur);

			if (itemEnchants == null) {
				statement.setObject(3, null);
			} else {
				statement.setString(3, itemEnchants);
			}

			if (buyPrice == -1) {
				statement.setDouble(4, 9999);
			} else {
				statement.setDouble(4, plugin.Round(buyPrice, Config.priceRounding));
			}

			statement.setString(5, sender.getName());

			ResultSet result = statement.executeQuery();

			double price;
			int id, amount;
			double senderBalance;
			String trader;
			int canBuy;
			ItemStack itemStack;
			Boolean infinite;
			String enchants;
			while (result.next()) {
				amount = result.getInt(9);
				if (amount <= 0)
					continue;

				found += 1;
				// patron = result.getString(1);

				id = result.getInt(1);
				infinite = result.getBoolean(3);
				trader = result.getString(4);
				price = result.getDouble(8);
				enchants = result.getString(7);

				// plugin.info("processBuyOrder id: " + id + ", price: " + price
				// + ", amount: " + amount);

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
					// itemStack.setAmount(canBuy);

					canBuy = getFitAmount(itemStack, canBuy, player);

					if (canBuy < 0)
						break;

					itemStack.setAmount(canBuy);

					// plugin.info("getAmount: " + itemStack.getAmount());

					// if (dryrun == true || InventoryUtil.fits(itemStack,
					// player.getInventory())) {

					if (dryrun == false) {
						plugin.debtPlayer(sender.getName(), canBuy * price);
						// plugin.sendMessage(sender, preview +
						// F("withdrewMoney", plugin.Round(canBuy * price,
						// Config.priceRounding)));

						if (infinite == false)
							plugin.payPlayer(trader, canBuy * price);

						InventoryUtil.add(itemStack, player.getInventory());

						if (infinite == false) {
							decreaseInt(Config.sqlPrefix + "Orders", id, "amount", canBuy);
							// increaseInt(Config.sqlPrefix + "Orders", id,
							// "exchanged", canBuy);
							// Mailbox Todo?

						}

						plugin.notifySellerOfExchange(trader, itemID, itemDur, canBuy, price, sender.getName(), dryrun);// buy

						if (Config.logTransactionsToDB == true)
							insertTransaction(1, sender.getName(), itemID, itemDur, enchants, canBuy, price, trader);

					}

					tAmount += canBuy;
					tPrice += price * canBuy;

					// if (dryrun == true) {
					plugin
						.sendMessage(
							sender,
							F("foundItem", itemName, canBuy, plugin.Round(price * canBuy, Config.priceRounding), plugin.Round(price, Config.priceRounding),
								trader));
					// } else {
					// plugin.sendMessage(
					// sender,
					// F("buyingItem", itemName, canBuy, plugin.Round(price *
					// canBuy, Config.priceRounding),
					// plugin.Round(price, Config.priceRounding), trader));
					// }

					buyAmount -= canBuy;
					// }

				}
			}

			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (tAmount > 0) {
			plugin
				.sendMessage(
					sender,
					preview
						+ F("buyingItemsTotal", itemName, tAmount, plugin.Round(tPrice, Config.priceRounding),
							plugin.Round(tPrice / tAmount, Config.priceRounding)));

		} else if (silentFail == false) {
			if (itemEnchants != null)
				itemName += "-" + itemEnchants;
			
			if (found == 0) {
				// String itemName = plugin.itemdb.getItemName(itemID, itemDur);
				plugin.sendMessage(sender, F("noSellersForBuy", itemName));
			} else if (found > 0 && initialAmount == buyAmount) {
				plugin.sendMessage(sender, F("noSellersForBuyPrice", itemName, buyAmount, buyPrice * buyAmount, buyPrice));
			}

		}

		return buyAmount;
	}

	private static String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}

	public int cancelOrders(CommandSender sender, int cType, int ciID, short ciDur, String itemEnchants, int cAmount, Boolean dryrun) {
		int changes = 0;
		String sortBy = "price DESC";
		if (cType == 2) {
			sortBy = "price ASC";
		}

		String query = "SELECT * FROM "
			+ Config.sqlPrefix
			+ "Orders WHERE `type` = ? AND `infinite` = 0 AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` like ? AND `amount` > 0 ORDER BY "
			+ sortBy;

		Player player = (Player) sender;
		Connection con = getSQLConnection();

		String itemName = plugin.itemdb.getItemName(ciID, ciDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		
		Boolean hasOrders = false;

		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, cType);
			statement.setString(2, sender.getName());
			statement.setInt(3, ciID);
			statement.setInt(4, ciDur);
			
			if (itemEnchants != null){
				statement.setString(5, itemEnchants);
			}else{
				statement.setObject(5, null);
			}
			
			// statement.setString(3, sender.getName());

			// statement.setDouble(3, buyPrice);

			ResultSet result = statement.executeQuery();

			double price;
			int id, amount;
			double senderBalance;
			String trader;
			int canBuy;
			ItemStack itemStack;
			Boolean infinite;
			String enchants;

			while (result.next()) {
				if (cAmount <= 0)
					break;
				hasOrders = true;
				// patron = result.getString(1);

				id = result.getInt(1);
				infinite = result.getBoolean(3);
				trader = result.getString(4);
				price = result.getDouble(8);
				amount = result.getInt(9);
				enchants = result.getString(7);

				// plugin.info("processBuyOrder id: " + id + ", price: " + price
				// + ", amount: " + amount);

				canBuy = Math.min(amount, cAmount);

				if (canBuy > 0) {
					String preview = "";
					if (dryrun == true)
						preview = L("preview");

					if (cType == 1) {// Sale, return items.
						ItemStack is = new ItemStack(ciID, 1);
						is.setDurability(ciDur);
						is.setAmount(canBuy);

	
						for (int i = canBuy; i > 0; i--) {
							if (dryrun == true || plugin.database.giveItemToPlayer(player, ciID, ciDur, enchants, i) == true) {
								if (dryrun == false)
									plugin.database.decreaseInt(Config.sqlPrefix + "Orders", id, "amount", i);

								
								plugin.sendMessage(sender, preview + F("returnedYourItem", itemName, i));
								break;
							}
						}

					} else if (cType == 2) {// Buy, return money.
						double money = price * canBuy;
						if (dryrun == false) {
							plugin.payPlayer(sender.getName(), money);
							if (infinite == false) {
								decreaseInt(Config.sqlPrefix + "Orders", id, "amount", canBuy);
							}
						}

						plugin.sendMessage(sender, preview + F("refundedYourMoney", plugin.Round(money, Config.priceRounding)));

					}

					cAmount -= canBuy;

					changes += 1;
				}

			}

			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// clean sell orders.
		if (dryrun == false) {
			cleanSellOrders(con);
			cleanBuyOrders(con);
		}
		if (hasOrders == false)
			plugin.sendMessage(sender, L("noActiveOrders"));

		return changes;
	}

	public void postBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun) {
		Connection con = getSQLConnection();
		postBuyOrder(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, dryrun, con);
		closeSQLConnection(con);
	}

	public void postBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun,
		Connection con) {
		int success = 0;

		String itemName = plugin.itemdb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		
		if (buyAmount > 0) {

			if (buyPrice == -1) {
				buyPrice = getTradersLastPrice(sender.getName(), itemID, itemDur, 2);

				if (buyPrice <= 0) {
					plugin.sendMessage(sender, L("mustSupplyAPrice"));
					return;
				}
			}

			if (plugin.getBalance(sender.getName()) < (buyAmount * buyPrice)) {
				plugin.sendMessage(sender,
					F("buyNotEnoughFunds", plugin.Round(buyPrice * buyAmount, Config.priceRounding), plugin.Round(buyPrice, Config.priceRounding)));
				plugin.sendMessage(sender, L("failedToCreateOrder"));
				return;
			}

			buyAmount = checkSellOrders(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, false, con, true);

			if (buyAmount == 0)
				return;

			if (dryrun == false)
				success = insertOrder(2, false, sender.getName(), itemID, itemDur, itemEnchants, buyPrice, buyAmount);

			if (dryrun == true || success > 0) {
				String preview = "";
				if (dryrun == true)
					preview = L("preview");

				//createdBuyOrderEnchant

				plugin.sendMessage(
					sender,
					F("createdBuyOrder", itemName, buyAmount, plugin.Round(buyPrice * buyAmount, Config.priceRounding),
						plugin.Round(buyPrice, Config.priceRounding)));
				
				if (dryrun == false) {
					plugin.sendMessage(sender, preview + F("withdrewMoney", plugin.Round(buyPrice * buyAmount, Config.priceRounding)));
					plugin.debtPlayer(sender.getName(), buyAmount * buyPrice);

					if (success == 1)
						plugin.announceNewOrder(2, sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice);
				}

			}
		}

		if (success == 0) {
			plugin.sendMessage(sender, L("failedToCreateOrder"));
		}

	}

	public int processBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun,
		Connection con) {
		int success = 0;
		int beforeAmount = buyAmount;

		// plugin.info("buyAmountA: " + buyAmount);

		// if (Config.cancelSelfSalesWhenBuying == true)
		// buyAmount = checkPlayerSellOrders(sender, itemID, itemDur, buyAmount,
		// buyPrice, dryrun, con);

		buyAmount = checkSellOrders(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, dryrun, con, false);
		// plugin.info("buyAmountB: " + buyAmount);

		if (buyAmount != beforeAmount) {
			cleanSellOrders(con);
			return 1;
		}
		// success = 1;

		// success = postBuyOrder(sender, itemID, itemDur, buyAmount, buyPrice,
		// dryrun, con);

		return success;
	}

	public int processBuyOrder(CommandSender sender, int itemID, short itemDur, String itemEnchants, int buyAmount, double buyPrice, Boolean dryrun) {
		Connection con = getSQLConnection();
		int value = processBuyOrder(sender, itemID, itemDur, itemEnchants, buyAmount, buyPrice, dryrun, con);
		closeSQLConnection(con);
		return value;
	}

	public boolean increaseOrderAmount(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount) {
		boolean value = false;
		if (itemEnchants == null) {
			Connection con = getSQLConnection();

			// String query = "UPDATE " + table + " SET " + column + "=" +
			// column + " + ? WHERE `id` = ?;";
			String query = "UPDATE "
				+ Config.sqlPrefix
				+ "Orders"
				+ " SET `amount` = `amount` + ? WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL  AND `price` = ?;";

			PreparedStatement statement;
			try {
				statement = con.prepareStatement(query);
				statement.setInt(1, amount);

				statement.setInt(2, type);
				statement.setBoolean(3, infinite);
				statement.setString(4, player);
				statement.setInt(5, itemID);
				statement.setInt(6, itemDur);
				statement.setDouble(7, price);

				int sucessful = statement.executeUpdate();

				if (sucessful > 0)
					value = true;

				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			closeSQLConnection(con);
		}
		return value;
	}

	public int insertOrder(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount, Connection con) {

		if (increaseOrderAmount(type, infinite, player, itemID, itemDur, itemEnchants, price, amount) == true)
			return 2;

		int updateSuccessful = 0;
		// price = plugin.Round(price, Config.priceRounding);

		PreparedStatement statement;

		String query = "INSERT INTO " + Config.sqlPrefix
			+ "Orders (`id`, `type`, `infinite`, `player`, `itemID`, `itemDur`, `itemEnchants`, `price`, `amount`) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?);";
		try {
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

	public static void closeSQLConnection(Connection con) {
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

	public int increaseInt(String table, int rowID, String column, int amount) {
		Connection con = getSQLConnection();
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
		closeSQLConnection(con);
		return sucessful;
	}

	/**/
	public int decreaseInt(String table, int rowID, String column, int amount) {
		int sucessful = 0;
		Connection con = getSQLConnection();
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

	public itemStats getItemStats(int itemID, int itemDur, String itemEnchants, int getType) {
		itemStats myReturn = new itemStats();

		Connection con = getSQLConnection();
		String query = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` like ? AND `amount` > 0";

		if (getType > 0) {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` like ? AND `amount` > 0 AND `type` = ?;";
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

			if (itemEnchants == null) {
				statement.setObject(3, null);
			} else {
				statement.setString(3, itemEnchants);
			}

			if (getType > 0) {
				statement.setInt(4, getType);
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

	public static class checkPendingBuysTask implements Runnable {
		private CommandSender sender;

		public checkPendingBuysTask(CommandSender sender) {
			this.sender = sender;
		}

		@Override
		public void run() {
			checkPendingBuys(this.sender);
		}
	}

	public static void checkPendingBuys(CommandSender sender) {

		Connection con = getSQLConnection();
		// String SQL = "SELECT * FROM " + Config.sqlPrefix +
		// "Orders WHERE `type` = 2 AND `player` LIKE ? AND `exchanged` > 0 ORDER BY `exchanged` DESC";
		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Mailbox WHERE `player` LIKE ? ORDER BY `amount` DESC";

		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);
			statement.setString(1, sender.getName());

			ResultSet result = statement.executeQuery();

			int itemID, exchanged;
			short itemDur;
			String itemName;
			while (result.next()) {
				count += 1;

				itemID = result.getInt(3);
				itemDur = result.getShort(4);
				// amount = result.getInt(9);
				exchanged = result.getInt(6);
				itemName = ItemDb.getItemName(itemID, itemDur);

				// plugin.sendMessage(sender, id + ": "+itemName+"x"+exchanged);
				if (exchanged > 0)
					sender.sendMessage(ExchangeMarket.chatPrefix + F("collectPending", itemName, exchanged));

			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		closeSQLConnection(con);
	}

	public int collectPendingBuys(CommandSender sender) {
		Connection con = getSQLConnection();
		int value = collectPendingBuys(sender, con);
		closeSQLConnection(con);
		return value;
	}

	public int collectPendingBuys(CommandSender sender, Connection con) {
		int success = 0;

		// String SQL = "SELECT * FROM " + Config.sqlPrefix +
		// "Orders WHERE `type` = 2 AND `player` LIKE ? AND `exchanged` > 0 ORDER BY `exchanged` DESC";
		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Mailbox WHERE `player` LIKE ? ORDER BY `amount` DESC";

		Player player = (Player) sender;

		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);
			statement.setString(1, sender.getName());

			ResultSet result = statement.executeQuery();

			int id, type, itemID, mount, amount;
			short itemDur;
			double price;
			String itemName;
			Boolean infinite;
			String toCollect, enchants;
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

				enchants = result.getString(5);

				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				// plugin.sendMessage(sender, id + ": "+itemName+"x"+exchanged);
				if (amount > 0) {
					for (int i = amount; i > 0; i--) {
						if (plugin.database.giveItemToPlayer(player, itemID, itemDur, enchants, i) == true) {
							plugin.sendMessage(sender, F("collectedItem", itemName, i));
							plugin.database.decreaseInt(Config.sqlPrefix + "Mailbox", id, "amount", i);
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

		cleanMailbox();

		if (count == 0) {
			plugin.sendMessage(sender, L("nothingToCollect"));
		}

		return success;
	}

	public int getResultCount(String query, Object... args) {
		// String query = "SELECT COUNT(*) FROM " + Config.sqlPrefix +
		// "Orders WHERE `player` LIKE ? ";

		Connection con = getSQLConnection();
		int rows = 0;
		try {
			PreparedStatement statement = con.prepareStatement(query);
			// statement.setString(1, sender.getName());

			int i = 0;
			for (Object a : args) {
				i += 1;
				// plugin.info("executeQuery "+i+": " + a);
				statement.setObject(i, a);
			}

			ResultSet result = statement.executeQuery();
			while (result.next()) {
				rows = result.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return rows;
	}

	public int listPlayerOrders(CommandSender sender, String trader, int page) {

		int rows = getResultCount("SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `player` LIKE ?", sender.getName());

		int maxPages = (int) Math.floor(rows / Config.transactionsPerPage);

		if (page < 0) {
			page = maxPages;
		} else {
			page -= 1;
		}

		plugin.sendMessage(sender, F("transactionPage", page + 1, maxPages + 1));

		int updateSuccessful = 0;

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `player` LIKE ? ORDER BY id ASC LIMIT " + (Config.transactionsPerPage * page) + ", "
			+ Config.transactionsPerPage;// `amount`
		Connection con = getSQLConnection(); // >
		// 0
		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);
			statement.setString(1, trader);

			ResultSet result = statement.executeQuery();

			int id, type, itemID, itemDur, amount, exchanged;
			double price;
			String itemName, itemEnchants;
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
				itemEnchants = result.getString(7);
				price = result.getDouble(8);
				amount = result.getInt(9);
				// exchanged = result.getInt(10);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);
				if (itemEnchants != null)
					itemName += "-" + itemEnchants;
				
				// .sendMessage(sender, F("playerOrder",
				// TypeToString(type,infinite), id, itemName,amount, price) +
				// toCollect);
				String typeString = ChatColor.RED + TypeToString(type, infinite);
				if (type == 2)
					typeString = ChatColor.GREEN + TypeToString(type, infinite);

				sender.sendMessage(F("playerOrder", typeString, id, itemName, amount, plugin.Round(amount * price, Config.priceRounding),
					plugin.Round(price, Config.priceRounding), ColourName(sender, trader)));

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

	public String ColourName(CommandSender sender, String name) {
		if (plugin.pluginName.equalsIgnoreCase(name)) {
			return ChatColor.GOLD + name + ChatColor.RESET;

		}
		if (sender.getName().equalsIgnoreCase(name)) {
			return ChatColor.YELLOW + name + ChatColor.RESET;

		}

		return name;
	}

	public int removeOrder(CommandSender sender, int orderID) {
		Connection con = getSQLConnection();
		int success = 0;

		String query = "DELETE FROM " + Config.sqlPrefix + "Orders WHERE `id` = ?";

		try {
			PreparedStatement statement = con.prepareStatement(query);

			statement.setInt(1, orderID);

			success = statement.executeUpdate();

			statement.close();
			con.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		closeSQLConnection(con);
		return success;
	}

	public static class queryReturn {
		Connection con;
		PreparedStatement statement;
		ResultSet result;

		public queryReturn(Connection con, PreparedStatement statement, ResultSet result) {
			this.con = con;
			this.statement = statement;
			this.result = result;
		}
	}

	public queryReturn executeQuery(String query, Object... args) {
		Connection con = getSQLConnection();
		// myreturn = null;
		// List<Object> results = new ArrayList<Object>();

		queryReturn myreturn = null;// = new queryReturn();

		try {
			PreparedStatement statement = con.prepareStatement(query);

			// plugin.info("executeQuery: " + args.length);

			int i = 0;
			for (Object a : args) {
				i += 1;
				// plugin.info("executeQuery "+i+": " + a);
				statement.setObject(i, a);
			}

			ResultSet result = statement.executeQuery();

			myreturn = new queryReturn(con, statement, result);

			// result.close();
			// statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// closeSQLConnection(con);

		return myreturn;
	}

	public Boolean closeQuery(queryReturn qReturn) {
		try {
			qReturn.result.close();
			qReturn.statement.close();
			qReturn.con.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void searchOrders(CommandSender sender, int itemID, short itemDur, String itemEnchants) {
		int rows = 0;

		if (itemEnchants != null) {
			rows = getResultCount("SELECT COUNT(*) FROM " + Config.sqlPrefix
				+ "Orders WHERE `itemID` = ? AND `itemEnchants` = ? AND `itemDur` = ? AND amount > 0", itemID, itemDur, itemEnchants);
		} else {
			rows = getResultCount("SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `itemID` = ? AND `itemDur` = ? AND amount > 0", itemID, itemDur);
		}

		String itemName = plugin.itemdb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		
		plugin.sendMessage(sender, F("resultsForItem", rows, itemName));
		if (rows <= 0)
			return;
		queryReturn qReturn;

		String query;

		if (itemEnchants != null) {
			query = "SELECT * FROM " + Config.sqlPrefix
				+ "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` = ? AND amount > 0 ORDER BY `price` ASC;";
			qReturn = executeQuery(query, itemID, itemDur, itemEnchants);
		} else {
			query = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `itemID` = ? AND `itemDur` = ? AND amount > 0 ORDER BY `price` ASC;";
			qReturn = executeQuery(query, itemID, itemDur);
		}

		/**/

		int results = 0;
		try {
			int id, type, amount, exchanged;
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

				String typeString = ChatColor.RED + TypeToString(type, infinite);
				if (type == 2)
					typeString = ChatColor.GREEN + TypeToString(type, infinite);

					sender.sendMessage(F("playerOrder", typeString, id, itemName, amount, plugin.Round(amount * price, Config.priceRounding),
						plugin.Round(price, Config.priceRounding), ColourName(sender, trader)));

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (results == 0)
			plugin.sendMessage(sender, L("noActiveList"));

		closeQuery(qReturn);
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
					itemName = plugin.itemdb.getItemName(itemID, itemDur);
					if (itemEnchants != null)
						itemName += "-" + itemEnchants;
					
					// plugin.sendMessage(sender, TypeToString(type) +
					// "] itemName: " + itemName + "x" + amount + " @ $" +
					// price);

					if (infinite == false) {
						if (type == 1) {// Sale, return items.

							for (int i = amount; i > 0; i--) {
								if (plugin.database.giveItemToPlayer(player, itemID, itemDur, itemEnchants, i) == true) {
									// plugin.sendMessage(sender,
									// F("collectedItem", itemName, i));
									plugin.database.decreaseInt(Config.sqlPrefix + "Orders", orderID, "amount", i);
									
									plugin.sendMessage(sender, F("returnedYourItem", itemName, i));
									updateSuccessful = 1;
									break;
								}
							}

							// }

							// InventoryUtil.add(is, player.getInventory());

						} else if (type == 2) {// Buy, return money.
							double money = price * amount;

							plugin.payPlayer(sender.getName(), money);
							plugin.sendMessage(sender, F("refundedYourMoney", plugin.Round(money, Config.priceRounding)));

							updateSuccessful = removeRow(Config.sqlPrefix + "Orders", orderID, con);

							if (updateSuccessful > 0) {

								plugin.sendMessage(sender, F("canceledOrder", TypeToString(type, infinite), orderID, itemName, amount));
							}
						}

					}

				}
			}

			cleanSellOrders(con);

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

	public double getTradersLastPrice(String trader, int itemID, short itemDur, int type, Connection con) {
		double price = 0;
		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `player` LIKE ? AND `itemID` = ? AND `itemDur` = ?  AND `type` = ? ORDER BY `id` DESC";

		try {
			PreparedStatement statement = con.prepareStatement(SQL);

			statement.setString(1, trader);
			statement.setInt(2, itemID);
			statement.setShort(3, itemDur);
			statement.setInt(4, type);

			ResultSet result = statement.executeQuery();

			while (result.next()) {
				price = result.getDouble(8);
				break;
			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return price;
	}

	public Double getTradersLastPrice(String trader, int itemID, short itemDur, int type) {
		Connection con = getSQLConnection();
		Double value = getTradersLastPrice(trader, itemID, itemDur, type, con);
		closeSQLConnection(con);
		return value;
	}

	public int listOrders(CommandSender sender, int getType, int page, Connection con) {
		String query = "SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0";
		if (getType > 0) {
			// query = "SELECT * FROM " + Config.sqlPrefix
			// +
			// "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL AND `amount` > 0 AND `type` = ?;";

			query = "SELECT COUNT(*) FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0 AND `type` = ?";
			// SQL = "SELECT * FROM " + Config.sqlPrefix +
			// "Orders WHERE `amount` > 0 AND `type` = ? ORDER BY " +
			// Config.listSortOrder + " LIMIT " + (Config.transactionsPerPage *
			// page) + ", "+Config.transactionsPerPage;
		}

		// Connection con = getSQLConnection();
		int rows = 0;
		try {
			PreparedStatement statement = con.prepareStatement(query);
			// statement.setString(1, sender.getName());

			if (getType > 0) {
				statement.setInt(1, getType);
			}
			ResultSet result = statement.executeQuery();
			while (result.next()) {
				rows = (result.getInt(1) - 1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// int Config.transactionsPerPage = ;
		int maxPages = (int) Math.floor(rows / Config.transactionsPerPage);

		if (page < 0) {
			page = maxPages;
		} else {
			page -= 1;
		}

		plugin.sendMessage(sender, F("transactionPage", page + 1, maxPages + 1));

		// query = "SELECT * FROM " + Config.sqlPrefix + "Transactions" +
		// " WHERE `seller` LIKE ? LIMIT " + (Config.transactionsPerPage * page)
		// + ", "+Config.transactionsPerPage;

		int updateSuccessful = 0;

		String SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0 ORDER BY id ASC LIMIT " + (Config.transactionsPerPage * page) + ", "
			+ Config.transactionsPerPage;

		if (getType > 0) {
			// query = "SELECT * FROM " + Config.sqlPrefix
			// +
			// "Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL AND `amount` > 0 AND `type` = ?;";

			SQL = "SELECT * FROM " + Config.sqlPrefix + "Orders WHERE `amount` > 0 AND `type` = ? ORDER BY id ASC LIMIT " + (Config.transactionsPerPage * page)
				+ ", " + Config.transactionsPerPage;
		}

		int count = 0;
		try {
			PreparedStatement statement = con.prepareStatement(SQL);

			if (getType > 0) {
				statement.setInt(1, getType);
			}

			ResultSet result = statement.executeQuery();

			int type, itemID, itemDur, amount, id;
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
				itemDur = result.getInt(6);
				itemEnchants = result.getString(7);
				price = result.getDouble(8);
				amount = result.getInt(9);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);
				if (itemEnchants != null)
					itemName += "-" + itemEnchants;
				
				// plugin.info("id: " + id + ", type: " + type + ", infinite: "
				// + infinite + ", itemName: " + itemName + ", amount: " +
				// amount + ", price: " + price);

				String typeString = ChatColor.RED + TypeToString(type, infinite);
				if (type == 2)
					typeString = ChatColor.GREEN + TypeToString(type, infinite);

					sender.sendMessage(F("playerOrder", typeString, id, itemName, amount, plugin.Round(amount * price, Config.priceRounding),
						plugin.Round(price, Config.priceRounding), ColourName(sender, trader)));

				/*
				 * if (type == 1) {
				 * 
				 * sender.sendMessage(F("playerOrder", ChatColor.RED +
				 * TypeToString(type, infinite), id, itemName, amount,
				 * plugin.Round(amount * price, Config.priceRounding),
				 * plugin.Round(price, Config.priceRounding), ColourName(sender,
				 * trader))); } else { sender.sendMessage(F("playerOrder",
				 * ChatColor.GREEN + TypeToString(type, infinite), id, itemName,
				 * amount, plugin.Round(amount * price, Config.priceRounding),
				 * plugin.Round(price, Config.priceRounding), ColourName(sender,
				 * trader))); }
				 */
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

	public int listOrders(CommandSender sender, int getType, int page) {
		Connection con = getSQLConnection();
		int value = listOrders(sender, getType, page, con);
		closeSQLConnection(con);
		return value;
	}
}
