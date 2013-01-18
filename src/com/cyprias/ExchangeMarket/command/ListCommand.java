package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class ListCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.LIST))
			list.add("/%s list - List all notes.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.LIST)) 
			return false;
		
		int page = -1; //Default to last page.
		if (args.length > 0) {// && args[1].equalsIgnoreCase("compact"))
			if (Plugin.isInt(args[0])) {
				page = Integer.parseInt(args[0]);
				if (page>0)
					page-=1;
			} else {
				ChatUtils.error(sender, "Invalid page: " +  args[0]);
				return true;
			}
		}
		
		
		
		try {
			List<Order> orders = Plugin.database.list(sender, page);
			
			if (orders == null || orders.size() <= 0){
				ChatUtils.send(sender, "§7There are no orders present.");
				return true;
			}
			
			//ChatUtils.send(sender, "Orders: " + orders.size());
			
			Order order;
			String format = Config.getColouredString("properties.list-row-format");
			String message;
			for (int i=0; i<orders.size();i++){
				order = orders.get(i);

				message = order.formatString(format, sender);
				
				if (sender instanceof ConsoleCommandSender){
					ChatUtils.sendSpam(sender, order.getId() + ": "+message);
				}else
					ChatUtils.sendSpam(sender, message);
				
			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			ChatUtils.error(sender, e.getLocalizedMessage());
			return true;
		}
		
		
		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.LIST, "/%s list [page] - List all notes.", cmd);
	}

	public boolean hasValues() {
		return false;
	}

}
