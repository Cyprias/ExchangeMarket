package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class OrdersCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) throws SQLException {
		if (Plugin.hasPermission(sender, Perm.ORDERS))
			if (Plugin.database.getPlayerOrderCount(sender) > 0)
				list.add("/%s orders - Show your existing orders.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.ORDERS, "/%s orders [itemName] <page>", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	// static HashMap<String, List<pendingOrder>> pendingOrders = new
	// HashMap<String, List<pendingOrder>>();
	/*
	public static class pendingOrder {
		public pendingOrder(int oId, int amount) {
			// this.player = player;
			this.orderId = oId;
			this.amount = amount;
		}

		// private Player player;
		private int orderId;
		private int amount;
		
	}*/
	

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.ORDERS)) 
			return false;
		ItemStack stock = null;
		int page = -1; //Default to last page.
		if (args.length > 0) {// && args[1].equalsIgnoreCase("compact"))
			if (Plugin.isInt(args[0])) {
				page = Integer.parseInt(args[0]);
				if (page>0)
					page-=1;
			} else {
				stock = Plugin.getItemStack(args[0]);
				
				if (stock == null || stock.getTypeId() == 0) {
					ChatUtils.error(sender, "Unknown input: " + args[0]);
					return true;
				}
				
				if (args.length > 1) {
					if (Plugin.isInt(args[1])) {
						page = Integer.parseInt(args[1]);
						if (page>0)
							page-=1;
					}
				}
			}
		}
		
		List<Order> orders;
		if (stock != null){
			orders = Plugin.database.getPlayerOrders(sender, stock, page);
		}else{
			orders = Plugin.database.getPlayerOrders(sender, page);
		}
		
		
		//ChatUtils.send(sender, "§7You have §f" + orders.size() + " §7orders.");
		
		if (orders == null || orders.size() == 0)
			return true;
		
		Order order;
		//ItemStack stock;
		String format = Config.getColouredString("properties.orders-row-format");
		String message;
		//int dplaces = Config.getInt("properties.price-decmial-places");
		for (int i=0; i<orders.size();i++){
			order = orders.get(i);

			message = order.formatString(format, sender);
			
			ChatUtils.sendSpam(sender, message);
			
		}
		
		return true;
	}
	
	
}
