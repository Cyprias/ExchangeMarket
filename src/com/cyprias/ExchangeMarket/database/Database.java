package com.cyprias.ExchangeMarket.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

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
	
	boolean cleanEmpties() throws SQLException;
	boolean cleanMailboxEmpties() throws SQLException;
	boolean init();
	boolean insert(Order order) throws SQLException;
	boolean insertTransaction(int type, String buyer, int itemID, int itemDur, String itemEnchants, int amount, double price, String seller) throws SQLException;
	boolean orderExists(int id) throws SQLException;
	boolean remove(int id) throws SQLException;
	boolean sendToMailbox(String receiver, ItemStack stock, int amount) throws SQLException;
	boolean setAmount(int id, int amount) throws SQLException;
	boolean setPackageAmount(int id, int amount) throws SQLException;
	boolean setPrice(int id, double price) throws SQLException;
	
	double getLastPrice(Order order) throws SQLException;
	
	int getAmount(int id) throws SQLException;
	int getLastId() throws SQLException;

	List<Order> findOrders(int orderType, ItemStack stock) throws SQLException;
	List<Order> getPlayerOrders(CommandSender sender, int page) throws SQLException;
	List<Order> list(CommandSender sender, int page) throws SQLException;
	List<Order> search(ItemStack stock) throws SQLException;
	List<Order> search(ItemStack stock, int orderType) throws SQLException;
	List<Order> search(ItemStack stock, int orderType, CommandSender sender) throws SQLException;
	List<Parcel> getPackages(CommandSender sender) throws SQLException;
	List<Transaction> listTransactions(CommandSender sender, int page) throws SQLException;
	
	Order findMatchingOrder(Order order) throws SQLException;
	Order getOrder(int id) throws SQLException;

}
