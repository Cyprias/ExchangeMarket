package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.database.Order;

public class RemoveCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.REMOVE))
			list.add("/%s remove - Force remove an order from existence, no refund given.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.CONSOLE;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.REMOVE, "/%s remove <id>", cmd);
	}

	@Override
	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.REMOVE)) 
			return false;
		
		if (args.length <= 0 || args.length >= 3){
			getCommands(sender, cmd);
			return true;
		}
		
		int id = 0;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 0) {
			if (Plugin.isInt(args[0])) {
				id = Integer.parseInt(args[0]);
			} else {
				// ExchangeMarket.sendMessage(sender, F("invalidAmount",
				// args[2]));
				ChatUtils.error(sender, "Invalid id: " + args[0]);
				return true;
			}
		}
		
		
		Order order = Plugin.database.getOrder(id);
		
		if (order == null){
			ChatUtils.send(sender, "§7That order does not exist.");
			return true;
		}
		
		if (order.remove()){
			ChatUtils.send(sender, "§7Order #§f" + id + " §7has been removed from the database.");
		}else{
			ChatUtils.send(sender, "§7Unknown failure, could not remove order.");
		}
		return true;
	}

	
	
}
