package com.cyprias.ExchangeMarket.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.SearchParser;
import com.cyprias.ExchangeMarket.database.Database.queryReturn;

public class SQLite implements Database {
	private static String sqlDB;
	
	static String order_table = "Orders";
	static String prefix = ""; //empty
	
	@Override
	public Boolean init() {
		File file = Plugin.getInstance().getDataFolder();
		String pluginPath = file.getPath() + File.separator;

		sqlDB = "jdbc:sqlite:" + pluginPath + "database.sqlite";
		
		try {
			createTables();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(sqlDB);
	}
	
	public static boolean tableExists(String tableName) throws SQLException {
		boolean exists = false;
		Connection con = getConnection();
		ResultSet result = con.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';");
		while (result.next()) {
			exists = true;
			break;
		}
		con.close();
		return exists;
	}
	
	public static void createTables() throws SQLException, ClassNotFoundException {
		// database.plugin.debug("Creating SQLite tables...");
		Class.forName("org.sqlite.JDBC");
		Connection con = getConnection();
		Statement stat = con.createStatement();

		if (tableExists(prefix + "Orders") == false) {
			con.prepareStatement(
				"CREATE TABLE "
					+ prefix
					+ "Orders (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type` INT NOT NULL, `infinite` BOOLEAN NOT NULL DEFAULT '0' , `player` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NULL, `price` DOUBLE NOT NULL, `amount` INT NOT NULL)")
				.executeUpdate();
		}

			
		if (tableExists(prefix + "Transactions") == false) {
			con.prepareStatement(
				"CREATE TABLE "
					+ prefix
					+ "Transactions"
					+ " (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type` INT NOT NULL, `buyer` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NOT NULL, `amount` INT NOT NULL, `price` DOUBLE NOT NULL, `seller` VARCHAR(32) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)")
				.executeUpdate();
		}

		if (tableExists(prefix + "Passwords") == false) {
			con.prepareStatement(
				"CREATE TABLE `"
					+ prefix
					+ "Passwords` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `username` VARCHAR(32) NOT NULL, `hash` VARCHAR(64) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE (`username`))")
				.executeUpdate();
		} 

		if (tableExists(prefix + "Mailbox") == false) {
			con.prepareStatement(
				"CREATE TABLE `"
					+ prefix
					+ "Mailbox"
					+ "` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `player` VARCHAR(32) NOT NULL, `itemId` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchant` VARCHAR(16) NULL, `amount` INT NOT NULL, `time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)")
				.executeUpdate();
		}
		
		stat.close();
		con.close();

	}
	
	public static int getResultCount(String query, Object... args) throws SQLException {
		queryReturn qReturn = executeQuery(query, args);
		//qReturn.result.first(); //not needed for sqlite.
		int rows = qReturn.result.getInt(1);
		qReturn.close();
		return rows;
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

	public Order getOrder(int id) throws SQLException {
		queryReturn results = executeQuery("SELECT * FROM `"+order_table+"` WHERE `id` = ? LIMIT 0 , 1", id);
		ResultSet r = results.result;

		Order order = null;
		while (r.next()) {
		//	Logger.info("id: " + r.getInt(1));
			order = new Order(
				r.getInt("type"),
				r.getBoolean("infinite"),
				r.getString("player"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price")
			);
			order.setId(id);
			
		}
		
		results.close();
		
		return order;
	}
	
	public Boolean insert(Order o) throws SQLException {
		int success = 0;
		String query;
		/*
		if (o.hasEnchantments()){
			query = "UPDATE "
				+ order_table
				+ " SET `amount` = `amount` + ? WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` = ? AND `price` = ? LIMIT 0 , 1;";
			success = executeUpdate(query, o.getAmount(), o.getOrderType(), o.isInfinite(), o.getPlayer(), o.getItemId(), o.getDurability(), o.getEncodedEnchantments(), o.getPrice());
		} else {
			query = "UPDATE "
				+ order_table
				+ " SET `amount` = `amount` + ? WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL  AND `price` = ? LIMIT 0 , 1;";

			success = executeUpdate(query, o.getAmount(), o.getOrderType(), o.isInfinite(), o.getPlayer(), o.getItemId(), o.getDurability(), o.getPrice());
		}
		if (success > 0) return true;
		*/
		
		query = "INSERT INTO " + order_table + " (`type`, `infinite`, `player`, `itemID`, `itemDur`, `itemEnchants`, `price`, `amount`) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
		success = executeUpdate(query, o.getOrderType(), o.isInfinite(), o.getPlayer(), o.getItemId(), o.getDurability(),o.getEncodedEnchantments(), o.getPrice(),o.getAmount());
		return (success > 0) ? true : false;
	}
	
	public int getLastId() throws SQLException {
		int id = 0;
		queryReturn results = executeQuery("SELECT * FROM `"+order_table+"` ORDER BY `id` DESC LIMIT 0 , 1");
		ResultSet r = results.result;
		while (r.next()) {
			id = r.getInt("id");
		}
		results.close();
		return id;
	}


	@Override
	public List<Order> list(CommandSender sender, int page) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order findMatchingOrder(Order order) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean setAmount(int id, int amount) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getLastPrice(Order order) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setPrice(int id, double price) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Order> search(ItemStack stock) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Order> search(ItemStack stock, int orderType) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Order> findOrders(int orderType, ItemStack stock) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean remove(int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean cleanEmpties() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean sendToMailbox(String receiver, ItemStack stock, int amount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean orderExists(int id) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getAmount(int id) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Parcel> getPackages(CommandSender sender) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean setPackageAmount(int id, int amount) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean cleanMailboxEmpties() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}


}
