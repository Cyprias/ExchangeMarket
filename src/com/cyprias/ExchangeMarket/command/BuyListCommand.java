package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class BuyListCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.LIST))
			list.add("/%s buylist - List buy orders.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IOException, InvalidConfigurationException, SQLException {
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
		
		
		
			List<Order> orders = Plugin.database.list(sender, Order.BUY_ORDER, page);
			
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
			
			
			
		
		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.LIST, "/%s buylist [page] - List all buy orders.", cmd);
	}

	public boolean hasValues() {
		return false;
	}

}