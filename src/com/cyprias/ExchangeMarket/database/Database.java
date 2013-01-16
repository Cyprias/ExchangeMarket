package com.cyprias.ExchangeMarket.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.SearchParser;

public interface Database {

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
	
	Boolean init();
	
	public Order getOrder(int id) throws SQLException;
	
	Boolean insert(Order order) throws SQLException;
	
	public int getLastId() throws SQLException;
	
	List<Order> search(ItemStack stock, int orderType) throws SQLException;
	List<Order> search(ItemStack stock) throws SQLException;
	
	public List<Order> list(CommandSender sender, int page) throws SQLException;
	
	
	Order findMatchingOrder(Order order) throws SQLException;

	Boolean setAmount(int id, int amount) throws SQLException;
	int getAmount(int id) throws SQLException;
	
	Double getLastPrice(Order order) throws SQLException;

	boolean setPrice(int id, double price) throws SQLException;
	
	List<Order> findOrders(int orderType, ItemStack stock) throws SQLException;

	Boolean remove(int id) throws SQLException;
	
	Boolean cleanEmpties() throws SQLException;

	Boolean sendToMailbox(String receiver, ItemStack stock, int amount) throws SQLException;

	boolean orderExists(int id) throws SQLException;
	
	//List<Order> orders = Plugin.database.search(stock);
	
	
}
