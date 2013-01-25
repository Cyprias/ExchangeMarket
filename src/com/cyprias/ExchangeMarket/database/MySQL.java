package com.cyprias.ExchangeMarket.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;


import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.configuration.Config;


public class MySQL implements Database {
	static String prefix;
	static String order_table;
	static String mailbox_table;
	static String transaction_table;
	
	public boolean init() throws IOException, InvalidConfigurationException {
		if (!canConnect()){
			Logger.warning("Failed to connect to MySQL!");
			return false;
		}
		prefix = Config.getString("mysql.prefix");
		order_table = prefix+ "Orders";
		mailbox_table  = prefix+ "Mailbox"; 
		transaction_table = prefix 	+ "Transactions";
		
		try {
			createTables();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public static void createTables() throws SQLException {
		Connection con = getConnection();

		if (tableExists(prefix + "Orders") == false) {
			con.prepareStatement(
				"CREATE TABLE "
					+ prefix
					+ "Orders (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `infinite` BOOLEAN NOT NULL DEFAULT '0' , `player` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NULL, `price` DOUBLE NOT NULL, `amount` INT NOT NULL) ENGINE = InnoDB")
				.executeUpdate();
		}

			
		if (tableExists(prefix + "Transactions") == false) {
			con.prepareStatement(
				"CREATE TABLE "
					+ prefix
					+ "Transactions"
					+ " (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `buyer` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NOT NULL, `amount` INT NOT NULL, `price` DOUBLE NOT NULL, `seller` VARCHAR(32) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE = InnoDB")
				.executeUpdate();
		}

		if (tableExists(prefix + "Passwords") == false) {
			con.prepareStatement(
				"CREATE TABLE `"
					+ prefix
					+ "Passwords` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `username` VARCHAR(32) NOT NULL, `hash` VARCHAR(64) NOT NULL, `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE (`username`))")
				.executeUpdate();
		} else if (tableFieldExists(prefix + "Passwords", "salt") == true) {
			con.prepareStatement("ALTER TABLE `" + prefix + "Passwords` DROP `salt`").executeUpdate();

		}

		if (tableExists(mailbox_table) == false) {
			con.prepareStatement(
				"CREATE TABLE `"
					+ mailbox_table
					+ "` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `player` VARCHAR(32) NOT NULL, `itemId` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchant` VARCHAR(16) NULL, `amount` INT NOT NULL, `time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE = InnoDB")
				.executeUpdate();
		}

		if (tableFieldExists(prefix + "Orders", "exchanged") == true) {
			migrateExchangedToMailbox();
			con.prepareStatement("ALTER TABLE `" + prefix + "Orders` DROP `exchanged`").executeUpdate();
		}

		con.close();
	}

	private static void migrateExchangedToMailbox() throws SQLException {
		queryReturn results = executeQuery("SELECT * FROM `" + order_table + "` WHERE `type` =2 AND `exchanged` >0");
		ResultSet r = results.result;
		
		String playerName, itemEnchant;
		int itemID, exchanged;
		short itemDur;
		while (r.next()) {
			playerName = r.getString(4);
			itemID = r.getInt(5);
			itemDur = r.getShort(6);
			itemEnchant = r.getString(7);
			// amount = result.getInt(9);
			exchanged = r.getInt(10);
			insertIntoMailbox(playerName, itemID, itemDur, itemEnchant, exchanged);
	}

		results.close();
	}
	private static int insertIntoMailbox(String player, int itemID, int itemDur, String itemEnchant, int amount) throws SQLException {
		int success = executeUpdate("UPDATE `" + mailbox_table
			+ "` SET `amount` = `amount` + ?, `time` = CURRENT_TIMESTAMP WHERE `player` = ? AND `itemId` = ? AND `itemDur` = ? AND `itemEnchant` = ?", amount, player, itemID, itemDur, ((itemEnchant != null) ? itemEnchant : null));

		if (success == 0) 
			success = executeUpdate("INSERT INTO `" + mailbox_table
				+ "` (`id`, `player`, `itemId`, `itemDur`, `itemEnchant`, `amount`, `time`) VALUES (NULL, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);", player, itemID, itemDur, ((itemEnchant != null)?itemEnchant : null), amount);

		

		return success;
	}

	
	public static boolean tableFieldExists(String table, String field) throws SQLException {
		boolean found = false;
		queryReturn results = executeQuery("SELECT * FROM " + table + ";");
		
		ResultSetMetaData rsMetaData = results.result.getMetaData();
		int numberOfColumns = rsMetaData.getColumnCount();

		String columnName;
		for (int i = 1; i < numberOfColumns + 1; i++) {
			columnName = rsMetaData.getColumnName(i);
			if (columnName.equalsIgnoreCase(field)) {
				found = true;
				break;
			}
		}

		results.close();

		return found;
	}
	
	@SuppressWarnings("rawtypes")
	public static queryReturn executeQuery(String query, Object... args) throws SQLException {
		Connection con = getConnection();
		queryReturn myreturn = null;// = new queryReturn();
		PreparedStatement statement = con.prepareStatement(query);
		int i = 0;
		List tList;
		for (Object a : args) {
			
			if (a instanceof List){
				tList = (List) a;
				
				for (int li=0; li<tList.size();li++){
					
					i += 1;
					statement.setObject(i, tList.get(li));

				}
				
			}else{
			
				i += 1;
				statement.setObject(i, a);
			}
		}
		ResultSet result = statement.executeQuery();
		myreturn = new queryReturn(con, statement, result);
		return myreturn;
	}
	
	public static int getResultCount(String query, Object... args) throws SQLException {
		queryReturn qReturn = executeQuery(query, args);
		qReturn.result.first();
		int rows = qReturn.result.getInt(1);
		qReturn.close();
		return rows;
	}
	
	public static boolean tableExists(String tableName) throws SQLException {
		boolean exists = false;
		Connection con = getConnection();
		ResultSet result = con.prepareStatement("show tables like '" + tableName + "'").executeQuery();
		result.last();
		if (result.getRow() != 0) 
			exists = true;
		con.close();
		return exists;
	}

	
	private static String getURL() {
		return "jdbc:mysql://" + Config.getString("mysql.hostname") + ":" + Config.getInt("mysql.port") + "/" + Config.getString("mysql.database");
	}
	
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(getURL(), Config.getString("mysql.username"), Config.getString("mysql.password"));
	}
	
	private Boolean canConnect() throws IOException, InvalidConfigurationException{
		try {
			@SuppressWarnings("unused")
			Connection con = getConnection();
		} catch (SQLException e) {
			return false;
		}
		return true;
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

	@Override
	public Order getOrder(int id) throws SQLException {
		queryReturn results = executeQuery("SELECT * FROM `"+order_table+"` WHERE `id` = ? LIMIT 0 , 1", id);
		ResultSet r = results.result;

		Order order = null;
		while (r.next()) {
		//	Logger.info("id: " + r.getInt(1));
			order = new Order(r.getInt("id"),
				r.getInt("type"),
				r.getBoolean("infinite"),
				r.getString("player"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price")
			);
			
		}
		
		results.close();
		
		return order;
	}


	public boolean insert(Order o) throws SQLException {
		int success = 0;
		String query;
		/*
		if (o.hasEnchantments()){
			query = "UPDATE "
				+ order_table
				+ " SET `amount` = `amount` + ? WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` = ? AND `price` = ?;";
			success = executeUpdate(query, o.getAmount(), o.getOrderType(), o.isInfinite(), o.getPlayer(), o.getItemId(), o.getDurability(), o.getEncodedEnchantments(), o.getPrice());
		} else {
			query = "UPDATE "
				+ order_table
				+ " SET `amount` = `amount` + ? WHERE `type` = ? AND `infinite` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL  AND `price` = ?;";

			success = executeUpdate(query, o.getAmount(), o.getOrderType(), o.isInfinite(), o.getPlayer(), o.getItemId(), o.getDurability(), o.getPrice());
		}
		if (success > 0) return true;
		*/
		
		Logger.debug("insert amount: " + o.getAmount());
		
		query = "INSERT INTO " + order_table + " (`type`, `infinite`, `player`, `itemID`, `itemDur`, `itemEnchants`, `price`, `amount`) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
		success = executeUpdate(query, o.getOrderType(), o.isInfinite(), o.getPlayer(), o.getItemId(), o.getDurability(),o.getEncodedEnchantments(), o.getPrice(),o.getAmount());
		return (success > 0) ? true : false;
	}

	@Override
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
	public List<Order> search(ItemStack stock) throws SQLException {
		return search(stock, 0, null);
	}
	public List<Order> search(ItemStack stock, int orderType) throws SQLException, IOException, InvalidConfigurationException{
		return search(stock, orderType, null);
	}
	
	
	@Override
	public List<Order> search(ItemStack stock, int orderType, CommandSender sender) throws SQLException {
	//	queryReturn results = executeQuery("SELECT * FROM `"+order_table+"` WHERE `player` LIKE ?", parser.players.get(i));
		List<Order> orders = new ArrayList<Order>();
		
		String query = "SELECT * FROM `"+order_table+"` WHERE `itemID` = ? AND `itemDur` = ?";
		
		String orderBy = " ORDER BY `price` ASC, `amount` ASC";

		
		queryReturn results;
		if (stock.getEnchantments().size() > 0){
			query += " AND `itemEnchants` = ?";
			results = executeQuery(query + orderBy, stock.getTypeId(),stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock));
		}else{
			query += " AND `itemEnchants` IS NULL";
			results = executeQuery(query + orderBy, stock.getTypeId(),stock.getDurability());
		}
		
		// ORDER BY `price` ASC, `amount` ASC;
		


		
		
	//	Logger.debug("query: " + query);
		
		
		
		ResultSet r = results.result;
		
		
		Order order;
		while (r.next()) {
			
			if (sender != null && !sender.getName().equalsIgnoreCase(r.getString("player")))
				continue;

			if (orderType > 0 && orderType != r.getInt("type"))
				continue;
				
			order = new Order(r.getInt("id"),
				r.getInt("type"),
				r.getBoolean("infinite"),
				r.getString("player"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price")
			);
			
			orders.add(order);
			
			
		}
		
		return orders;
	}

	public List<Order> list(CommandSender sender, int page) throws SQLException {

		int rows = getResultCount("SELECT COUNT(*) FROM " + order_table);
		Logger.debug("rows: " +rows);
		
		int perPage = Config.getInt("properties.rows-per-page");

		int max = (rows / perPage);// + 1;
		Logger.debug("max 1: " +max);
		
		if (rows % perPage == 0)
			max--;
		Logger.debug("max 2: " +max);
		Logger.debug("page 1: " +page);
		if (page < 0){
			page = max - (Math.abs(page) - 1);
			Logger.debug("page 2: " +page);
		}else{
			if (page > max)
				page = max;
			
			Logger.debug("page 13: " +page);
		}

		
		ChatUtils.send(sender, "§7Page: §f" + (page+1) + "§7/§f" + (max+1));
		if (rows == 0)
			return null;
		
		queryReturn results = executeQuery("SELECT * FROM `"+order_table+"` ORDER BY `id` LIMIT "+(perPage * page)+" , " + perPage);
		ResultSet r = results.result;
		
		List<Order> orders = new ArrayList<Order>();
		Order order;
		while (r.next()) {
			Logger.debug("id: " + r.getInt(1));
			order = new Order(r.getInt("id"),
				r.getInt("type"),
				r.getBoolean("infinite"),
				r.getString("player"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price")
			);
			
			orders.add(order);
		}

		results.close();
		return orders;
	}

	@Override
	public Order findMatchingOrder(Order order) throws SQLException {
		Order foundOrder = null;
		queryReturn results = executeQuery("SELECT * FROM `"+order_table+"` WHERE `type` = ? AND `player` = ? AND `itemID` = ? AND `itemDur` = ? AND `price` = ? ORDER BY `id` DESC LIMIT 0 , 1", order.getOrderType(), order.getPlayer(), order.getItemId(), order.getDurability(), order.getPrice());
		ResultSet r = results.result;

		if (r.next()) {
		
			foundOrder = new Order(r.getInt("id"),
				r.getInt("type"),
				r.getBoolean("infinite"),
				r.getString("player"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price")
			);
		}
		
		return foundOrder;
	}

	@Override
	public boolean setAmount(int id, int amount) throws SQLException {
		return (executeUpdate("UPDATE `"+order_table+"` SET `amount` = ? WHERE `id` = ?;", amount, id) > 0) ? true : false;
	}
	public boolean setPrice(int id, double price) throws SQLException {
		return (executeUpdate("UPDATE `"+order_table+"` SET `price` = ? WHERE `id` = ?;", price, id) > 0) ? true : false;
	}
	
	
	@Override
	public double getLastPrice(Order order) throws SQLException {
		double price = 0.0;
		
		String query;
		
		queryReturn results;
		if (order.hasEnchantments()){
			query = "SELECT * FROM " + order_table
				+ " WHERE `type` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ?  AND `itemEnchants` = ? ORDER BY `id` DESC LIMIT 0 , 1";
			results = executeQuery(query, order.getOrderType(), order.getPlayer(), order.getItemId(), order.getDurability(), order.getEncodedEnchantments());
		}else{
			query = "SELECT * FROM " + order_table
				+ " WHERE `type` = ? AND `player` LIKE ? AND `itemID` = ? AND `itemDur` = ?  AND `itemEnchants` IS NULL ORDER BY `id` DESC LIMIT 0 , 1";
			results = executeQuery(query, order.getOrderType(), order.getPlayer(), order.getItemId(), order.getDurability());
		}
		while (results.result.next()) {
			price = results.result.getDouble("price");
			break;
		}
		results.close();
		
		return price;
	}

	@Override
	public List<Order> findOrders(int orderType, ItemStack stock) throws SQLException {
		String query = "SELECT * FROM `"+order_table+"` WHERE `type` = ? AND `itemID` = ? AND `itemDur` = ?";
		
		queryReturn results;
		if (stock.getEnchantments().size() > 0){
			query += " AND `itemEnchants` = ?";
			query += " ORDER BY `id`";
			results = executeQuery(query, orderType, stock.getTypeId(),stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock));
		}else{
			query += " AND `itemEnchants` IS NULL";
			query += " ORDER BY `id`";
			results = executeQuery(query, orderType, stock.getTypeId(),stock.getDurability());
		}
		
		
		
		//Logger.info("query: " + query);
		
		
		
		ResultSet r = results.result;
		
		List<Order> orders = new ArrayList<Order>();
		Order order;
		while (r.next()) {
			
			order = new Order(r.getInt("id"),
				r.getInt("type"),
				r.getBoolean("infinite"),
				r.getString("player"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price")
			);
			
			
			orders.add(order);
			
		}
		
		return orders;
	}

	public boolean remove(int id) throws SQLException {
		return (executeUpdate("DELETE FROM `"+order_table+"` WHERE `id` = ?", id) > 0) ? true : false;
	}

	@Override
	public boolean cleanEmpties() throws SQLException {
		return (executeUpdate("DELETE FROM `"+order_table+"` WHERE `amount` = 0") > 0) ? true : false;
	}

	
	
	@Override
	public boolean sendToMailbox(String receiver, ItemStack stock, int amount) throws SQLException {
		String query = "UPDATE `" + mailbox_table
			+ "` SET `amount` = `amount` + ?, `time` = CURRENT_TIMESTAMP WHERE `player` = ? AND `itemId` = ? AND `itemDur` = ?";// AND `itemEnchant` = ?";
		
		int succsess;
		if (stock.getEnchantments().size() > 0){
			query += " AND `itemEnchant` = ?";
			succsess = executeUpdate(query, amount, receiver, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock));
		}else{
			query += " AND `itemEnchant` IS NULL";
			succsess = executeUpdate(query, amount, receiver, stock.getTypeId(), stock.getDurability());
		}
		

		if (succsess > 0) return true;
		
		query = "INSERT INTO `" + mailbox_table + "` (`player`, `itemId`, `itemDur`, `itemEnchant`, `amount`, `time`) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP);";
		
		succsess = executeUpdate(query, receiver, stock.getTypeId(), stock.getDurability(), ((stock.getEnchantments().size() > 0) ? MaterialUtil.Enchantment.encodeEnchantment(stock) : null), amount);
		return (succsess > 0) ? true : false;
	}

	@Override
	public boolean orderExists(int id) throws SQLException {
		boolean exists = false;
		queryReturn results = executeQuery("SELECT * FROM `"+order_table+"` WHERE `id` = ? LIMIT 0 , 1", id);
		ResultSet r = results.result;

		//Order order = null;
		while (r.next()) {
			exists = true;
			break;
		}
		
		results.close();
		
		return exists;
	}

	@Override
	public int getAmount(int id) throws SQLException {
		int amount = 0;
		String query = "SELECT * FROM `"+order_table+"` WHERE `id` = ? ";
		
		queryReturn results = executeQuery(query, id);
	

		ResultSet r = results.result;
		
		//Order order;
		while (r.next()) {
			amount = r.getInt("amount");
		}
		
		results.close();
		
		return amount;
	}

	@Override
	public List<Parcel> getPackages(CommandSender sender) throws SQLException {
		// TODO Auto-generated method stub
		List<Parcel> packages = new ArrayList<Parcel>();
		
		queryReturn results = executeQuery("SELECT * FROM `"+mailbox_table+"` WHERE `player` LIKE ? ORDER BY `id`", sender.getName());
		
		ResultSet r = results.result;
		while (r.next()) {
			
			
			packages.add(new Parcel(r.getInt("id"), r.getString("player"), r.getInt("itemId"), r.getShort("itemDur"), r.getString("itemEnchant"), r.getInt("amount"), r.getTimestamp("time")));
		}
		
		results.close();
		
		return packages;
	}

	@Override
	public boolean setPackageAmount(int id, int amount) throws SQLException {
		return (executeUpdate("UPDATE `"+mailbox_table+"` SET `amount` = ? WHERE `id` = ?;", amount, id) > 0) ? true : false;
	}
	

	@Override
	public boolean cleanMailboxEmpties() throws SQLException {
		return (executeUpdate("DELETE FROM `"+mailbox_table+"` WHERE `amount` = 0") > 0) ? true : false;
	}

	@Override
	public List<Order> getPlayerOrders(CommandSender sender, int page) throws SQLException {
		int rows = getResultCount("SELECT COUNT(*) FROM " + order_table + " WHERE `player` LIKE ?", sender.getName());

		int perPage = Config.getInt("properties.rows-per-page");
		
		//Logger.info("page1: " + page);
		int max = (rows / perPage);// + 1;
		
		if (rows % perPage == 0)
			max--;
		
		//Logger.info("max: " + max);
		if (page < 0){
			page = max - (Math.abs(page) - 1);
		}else{
			if (page > max)
				page = max;
			
		}


		ChatUtils.send(sender, "§7Page: §f" + (page+1) + "§7/§f" + (max+1));
		
		if (rows <= 0)
			return null;
		
		
		List<Order> orders = new ArrayList<Order>();
		
		queryReturn results = executeQuery("SELECT * FROM `" + order_table + "` WHERE `player` LIKE ? ORDER BY `id` LIMIT "+(perPage * page)+" , " + perPage, sender.getName());

		ResultSet r = results.result;
		
		
		Order order;
		while (r.next()) {
			
			order = new Order(r.getInt("id"),
				r.getInt("type"),
				r.getBoolean("infinite"),
				r.getString("player"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price")
			);
			
			orders.add(order);
			
		}
		
		return orders;
	}

	@Override
	public boolean insertTransaction(int type, String orderer, int itemID, int itemDur, String itemEnchants, int amount, double price, String owner) throws SQLException {
		if (itemEnchants == null)
			itemEnchants = "";
		return (executeUpdate("INSERT INTO "+ transaction_table+ " (`type`, `buyer`, `itemID`, `itemDur`, `itemEnchants`, `amount`, `price`, `seller`, `timestamp`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);", type, orderer, itemID, itemDur, itemEnchants, amount, price, owner) > 0) ? true : false;
	}

	@Override
	public List<Transaction> listTransactions(CommandSender sender, int page) throws SQLException {

		int rows = getResultCount("SELECT COUNT(*) FROM " + transaction_table + " WHERE `seller` LIKE ? ", sender.getName());

		//Logger.info("rows: " + rows);
		
		int perPage = Config.getInt("properties.rows-per-page");
		
		//Logger.info("page1: " + page);
		int max = (rows / perPage);// + 1;
		
		if (rows % perPage == 0)
			max--;
		
		//Logger.info("max: " + max);
		if (page < 0){
			page = max - (Math.abs(page) - 1);
		}else{
			if (page > max)
				page = max;
		}

		
		
		ChatUtils.send(sender, "§7Page: §f" + (page+1) + "§7/§f" + (max+1));

		if (rows == 0)
			return null;
		
		
		List<Transaction> transactions = new ArrayList<Transaction>();
		

		
		queryReturn results = executeQuery("SELECT * FROM `"+transaction_table+"` WHERE `seller` LIKE ? ORDER BY `id` LIMIT "+(perPage * page)+" , " + perPage, sender.getName());
		ResultSet r = results.result;
		
		//List<Order> orders = new ArrayList<Order>();
		Transaction transaction;
		while (r.next()) {
		//	Logger.info("id: " + r.getInt(1));
			transaction = new Transaction(
				r.getInt("id"),
				r.getInt("type"),
				r.getString("buyer"),
				r.getInt("itemID"),
				r.getShort("itemDur"),
				r.getString("itemEnchants"),
				r.getInt("amount"),
				r.getDouble("price"),
				r.getString("seller"),
				r.getTimestamp("timestamp")
			);

			transactions.add(transaction);
		}

		results.close();
		return transactions;
	}
	
}
