package com.cyprias.exchangemarket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.InventoryUtil;


public class Database {
	private ExchangeMarket plugin;
	
	/*
	 * Order types
	 * 1: Sell
	 * 2: Buy
	 * 3: Infinite Sell
	 * 4: Infinite Buy
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

			PreparedStatement statement = con.prepareStatement("show tables like '%"+Config.sqlPrefix+"Orders%'");
			ResultSet result = statement.executeQuery();
			
			/**/

			result.last();
			if (result.getRow() == 0) {

				query = "CREATE TABLE "+Config.sqlPrefix+"Orders (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` INT NOT NULL, `infinite` BOOLEAN NOT NULL DEFAULT '0' , `player` VARCHAR(32) NOT NULL, `itemID` INT NOT NULL, `itemDur` INT NOT NULL, `itemEnchants` VARCHAR(16) NULL, `price` DOUBLE NOT NULL, `amount` INT NOT NULL, `exchanged` INT NOT NULL DEFAULT '0')";

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
	
	public void processBuyOrder(CommandSender sender, int itemID, short itemDur, int buyAmount, double buyPrice){
		Connection con = getSQLConnection();

		
		
		
		
		String query = "SELECT * FROM "+Config.sqlPrefix+"Orders WHERE `type` = 1 AND `itemID` = ? AND `itemDur` = ? AND `price` <= ? AND `amount` > 0 ORDER BY `price` ASC";
		
		int updateSuccessful = 0;
		String itemName = plugin.itemdb.getItemName(itemID, itemDur);
		
		Player player = (Player) sender;
		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, itemID);
			statement.setInt(2, itemDur);

			statement.setDouble(3, buyPrice);

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
				
				plugin.info("processBuyOrder id: " + id + ", price: " + price + ", amount: " + amount);
				
				
				senderBalance = plugin.getBalance(sender.getName());
				plugin.info("senderBalance:" + senderBalance);
				
				canBuy=(int) Math.floor(senderBalance / price);
				canBuy = Math.min(canBuy, buyAmount);
				if (infinite == false)
					canBuy = Math.min(canBuy, amount);
				
				plugin.info("canBuy:" + canBuy);
				
				
				
				/**/
			
				
				if (canBuy>0){
					itemStack = new ItemStack(itemID, 1);
					itemStack.setDurability(itemDur);
					itemStack.setAmount(canBuy);
					
					
					if (InventoryUtil.fits(itemStack, player.getInventory())){
						plugin.sendMessage(sender, "Buying " + itemName + "x" + canBuy + " from " + trader + " for $" + price + " each ($"+canBuy*price+")");
						plugin.debtPlayer(sender.getName(), canBuy*price);
						
						InventoryUtil.add(itemStack, player.getInventory());
						
						//setInt(Config.sqlPrefix+"Orders", id, "amount", amount-canBuy);
						
						if (infinite == false)
							decreaseInt(Config.sqlPrefix+"Orders", id, "amount", canBuy);
							increaseInt(Config.sqlPrefix+"Orders", id, "exchanged", canBuy);
						
						buyAmount-=canBuy;
					}

				}
			}
			
			
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		plugin.info("buyAmount: " + buyAmount);
		
		if (buyAmount> 0) {
			int success = insertOrder(2, false, sender.getName(), itemID, itemDur, null, buyPrice, buyAmount, con);
			if (success>0){
				plugin.sendMessage(sender, "Created buy order for " + itemName + "x" + buyAmount + " at $"+buyPrice+ " each ($"+buyAmount*buyPrice+")");
				
				plugin.debtPlayer(sender.getName(), buyAmount*buyPrice);
				
			}
			
		}
		
		closeSQLConnection(con);
	}
	
	
	public int insertOrder(int type, Boolean infinite, String player, int itemID, int itemDur, String itemEnchants, double price, int amount, Connection con){
		int updateSuccessful = 0;
		String query = "INSERT INTO "+Config.sqlPrefix+"Orders (`id`, `type`, `infinite`, `player`, `itemID`, `itemDur`, `itemEnchants`, `price`, `amount`, `exchanged`) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, 0);";
		try {
			PreparedStatement statement = con.prepareStatement(query);
			statement.setInt(1, type);
			statement.setBoolean(2, infinite);
			statement.setString(3, player);
			statement.setInt(4, itemID);
			statement.setInt(5, itemDur);
		//	if (itemEnchants != null && !itemEnchants.equals("")){
				statement.setString(6, itemEnchants);
			//}else{
			//	plugin.info("NULL! " + itemEnchants);
			//	statement.setNull(5, java.sql.Types.VARCHAR);
			//}

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
		int value = insertOrder(type,infinite, player,itemID, itemDur, itemEnchants, price, amount, con);
		closeSQLConnection(con);
		return value;
	}
	public void closeSQLConnection(Connection con){
		try {
			con.close();
		} catch (SQLException e) {e.printStackTrace();}
	}
	
	public String TypeToString(int type){
		switch (type) {
		case 1:
			return "Sell";
		case 2:
			return "Buy";
		case 3:
			return "Infinite Sell";
		case 4:
			return "Infinite Buy";
		}
		return null;
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
	//String SQL = "UPDATE " + tblAllegiance + " SET XP=XP+" + amount + " WHERE `player` LIKE ?" + " AND `patron` LIKE ?";
	
	
	public int increaseInt(String table, int rowID, String column, int amount, Connection con){
		int sucessful = 0;
		
		String query = "UPDATE " + table + " SET "+column+"="+column+" + ? WHERE `id` = ?;";
		
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
	public int increaseInt(String table, int rowID, String column, int amount){
		Connection con = getSQLConnection();
		int sucessful = increaseInt(table, rowID, column, amount, con);
		closeSQLConnection(con);
		return sucessful;		
	}
	
	public int decreaseInt(String table, int rowID, String column, int amount, Connection con){
		int sucessful = 0;
		
		String query = "UPDATE " + table + " SET "+column+"="+column+" - ? WHERE `id` = ?;";
		
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
	public int decreaseInt(String table, int rowID, String column, int amount){
		Connection con = getSQLConnection();
		int sucessful = decreaseInt(table, rowID, column, amount, con);
		closeSQLConnection(con);
		return sucessful;		
	}
	
	public int setInt(String table, int rowID, String column, int value, Connection con){
		int sucessful = 0;
		
		String query = "UPDATE "+table+" SET " + column + " = ? WHERE `id` = ?;";
		
		
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
	
	public int setInt(String table, int rowID, String column, int value){
		Connection con = getSQLConnection();
		int sucessful = setInt(table, rowID, column, value, con);
		closeSQLConnection(con);
		return sucessful;		
	}
	
	public itemStats getItemStats(int itemID, int itemDur) {
		itemStats myReturn = new itemStats();
		
		Connection con = getSQLConnection();

		String query = "SELECT * FROM "+Config.sqlPrefix+"Orders WHERE `itemID` = ? AND `itemDur` = ? AND `itemEnchants` IS NULL AND `amount` > 0";
		
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
			
			
			ResultSet result = statement.executeQuery();


			while (result.next()) {
				// patron = result.getString(1);
				
				type = result.getInt(2);
				itemID = result.getInt(5);
				itemDur = result.getInt(6);
				price = result.getDouble(8);
				amount = result.getInt(9);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				aPrice = price;// / amount;
				
				
				totalPrice += aPrice;
				totalAmount += amount;

				// prices[i] = aPrice;

				prices.add(aPrice);
				amounts.add(amount);
				i+=1;
				
			}

			result.close();
			statement.close();
			myReturn.total = i;			
			myReturn.totalAmount = totalAmount;
			
			double avgPrice = totalPrice / i;
			// sender.sendMessage("avgPrice: " + Round(avgPrice,2));

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
	
	public int listPlayerOrders(CommandSender sender, String trader){
		int updateSuccessful = 0;
		
		Connection con = getSQLConnection();

		String SQL = "SELECT *" + " FROM "+Config.sqlPrefix+"Orders WHERE `player` LIKE ? ORDER BY itemID DESC;";
		
		try {
			PreparedStatement statement = con.prepareStatement(SQL);
			statement.setString(1, trader);

			ResultSet result = statement.executeQuery();

			int id, type, itemID, itemDur, amount;
			double price;
			String itemName;
			while (result.next()) {
				// patron = result.getString(1);
				id = result.getInt(1);
				type = result.getInt(2);
				itemID = result.getInt(5);
				itemDur = result.getInt(6);
				price = result.getDouble(8);
				amount = result.getInt(9);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				plugin.sendMessage(sender, "[" + TypeToString(type) +"] #" + id +": " + itemName + 
					"x" + amount +
					" @ $" + price
				
				);
			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		closeSQLConnection(con);
		
		return updateSuccessful;
	}
	
	public int listOrders(CommandSender sender, Connection con){
		int updateSuccessful = 0;
		
		String SQL = "SELECT *" + " FROM "+Config.sqlPrefix+"Orders ORDER BY itemID DESC;";
		
		try {
			PreparedStatement statement = con.prepareStatement(SQL);


			ResultSet result = statement.executeQuery();

			int type, itemID, itemDur, amount;
			double price;
			String itemName;
			while (result.next()) {
				// patron = result.getString(1);
				
				type = result.getInt(2);
				itemID = result.getInt(5);
				itemDur = result.getInt(6);
				price = result.getDouble(7);
				amount = result.getInt(8);
				itemName = plugin.itemdb.getItemName(itemID, itemDur);

				plugin.sendMessage(sender, TypeToString(type) +"] itemName: " + itemName + 
					"x" + amount +
					" @ $" + price
				
				);
			}

			result.close();
			statement.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return updateSuccessful;
	}
	public int listOrders(CommandSender sender){
		Connection con = getSQLConnection();
		int value = listOrders(sender, con);
		closeSQLConnection(con);
		return value;		
	}
}
