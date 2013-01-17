package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class SearchCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.SEARCH))
			list.add("/%s search - Search orders.");
	}

	
	@Override
	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.SEARCH))
			return false;

		if (args.length <= 0 || args.length >= 3){
			getCommands(sender, cmd);
			return true;
		}
		
		
		ItemStack stock = Plugin.getItemStack(args[0]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(sender, "Unknown item: " + args[0]);
			return true;
		}

		
		try {
			List<Order> orders = Plugin.database.search(stock);
			
			
			
			ChatUtils.send(sender, String.format("§7There are §f%s §7orders for §f%s§7.", orders.size(), Plugin.getItemName(stock)) );
			
			if (orders.size() < 0)
				return true;
			
			Order order;
			String format = Config.getColouredString("properties.list-row-format");
			String message;
			for (int i=0; i<orders.size();i++){
				order = orders.get(i);
				//stock = order.getItemStack();
				
				message = order.formatString(format, sender);
				
				ChatUtils.send(sender, message);
				
			}
			return true;
			
		} catch (SQLException e) {
			e.printStackTrace();
			ChatUtils.error(sender, "An error has occured: " + e.getLocalizedMessage());
			return true;
		}
		
	
	}

	@Override
	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "/%s serach <item> - Search orders.", cmd);
		//ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "Item "+ChatColor.WHITE+"i:"+ChatColor.GRAY+" - Item name or id", cmd);
		//ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "Player "+ChatColor.WHITE+"w:"+ChatColor.GRAY+" - Search by writer", cmd);
		//ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "Keyword "+ChatColor.WHITE+"k:"+ChatColor.GRAY+" - Search by note keyword", cmd);

		
		
	}

}
