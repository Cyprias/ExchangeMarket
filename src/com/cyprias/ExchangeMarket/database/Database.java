package com.cyprias.ExchangeMarket.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
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
	
	boolean cleanEmpties() throws SQLException, IOException, InvalidConfigurationException;
	boolean cleanMailboxEmpties() throws SQLException, IOException, InvalidConfigurationException;
	boolean init() throws IOException, InvalidConfigurationException;
	boolean insert(Order order) throws SQLException, IOException, InvalidConfigurationException;
	boolean insertTransaction(int type, String buyer, int itemID, int itemDur, String itemEnchants, int amount, double price, String seller) throws SQLException, IOException, InvalidConfigurationException;
	boolean orderExists(int id) throws SQLException, IOException, InvalidConfigurationException;
	boolean remove(int id) throws SQLException, IOException, InvalidConfigurationException;
	boolean sendToMailbox(String receiver, ItemStack stock, int amount) throws SQLException, IOException, InvalidConfigurationException;
	boolean setAmount(int id, int amount) throws SQLException, IOException, InvalidConfigurationException;
	boolean setPackageAmount(int id, int amount) throws SQLException, IOException, InvalidConfigurationException;
	boolean setPrice(int id, double price) throws SQLException, IOException, InvalidConfigurationException;
	
	double getLastPrice(Order order) throws SQLException, IOException, InvalidConfigurationException;
	
	int getAmount(int id) throws SQLException, IOException, InvalidConfigurationException;
	int getLastId() throws SQLException, IOException, InvalidConfigurationException;

	List<Order> findOrders(int orderType, ItemStack stock) throws SQLException, IOException, InvalidConfigurationException;
	List<Order> getPlayerOrders(CommandSender sender, int page) throws SQLException, IOException, InvalidConfigurationException;
	List<Order> getPlayerOrders(CommandSender sender, ItemStack stock, int page) throws SQLException, IOException, InvalidConfigurationException;

	int getPlayerOrderCount(CommandSender sender, ItemStack stock) throws SQLException;
	
	List<Order> list(CommandSender sender, int page) throws SQLException, IOException, InvalidConfigurationException;
	List<Order> list(CommandSender sender, int orderType, int page) throws SQLException, IOException, InvalidConfigurationException;
	
	List<Order> search(ItemStack stock) throws SQLException, IOException, InvalidConfigurationException;
	List<Order> search(ItemStack stock, int orderType) throws SQLException, IOException, InvalidConfigurationException;
	List<Order> search(ItemStack stock, int orderType, CommandSender sender) throws SQLException, IOException, InvalidConfigurationException;
	List<Parcel> getPackages(CommandSender sender) throws SQLException, IOException, InvalidConfigurationException;
	List<Transaction> listTransactions(CommandSender sender, int page) throws SQLException, IOException, InvalidConfigurationException;
	
	Order findMatchingOrder(Order order) throws SQLException, IOException, InvalidConfigurationException;
	Order getOrder(int id) throws SQLException, IOException, InvalidConfigurationException;
	

}
