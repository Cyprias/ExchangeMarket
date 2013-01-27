package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.database.Order;

public class TestCommand  implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
	//	if (Plugin.hasPermission(sender, Perm.SELL))
	//		list.add("/%s test");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.SELL, "/%s test", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IOException, InvalidConfigurationException, SQLException {
		if (!Plugin.checkPermission(sender, Perm.TEST)) {
			return false;
		}
		
		if (args.length > 0){
			ItemStack stock = Plugin.getItemStack(args[0]);
			if (stock == null || stock.getTypeId() == 0) {
				ChatUtils.error(sender, "Unknown item: " + args[0]);
				return true;
			}
			stock.setAmount(1000);
			
			if (args.length > 1){
			
				int amount = 0;// InventoryUtil.getAmount(item, player.getInventory());
				if (args.length > 1) {
					if (Plugin.isInt(args[1])) {
						amount = Integer.parseInt(args[1]);
					} else {
						// ExchangeMarket.sendMessage(sender, F("invalidAmount",
						// args[2]));
						ChatUtils.error(sender, "Invalid amount: " + args[1]);
						return true;
					}
				}
				
				
				
				if (args.length > 2){
					Order preOrder = new Order(Order.SELL_ORDER, false, sender.getName(), stock, 0);
					
					Player p = (Player) sender;
					
					if (args[2].equalsIgnoreCase("take")){
						
						Logger.debug("oAmount1: " + preOrder.getAmount());
						int taken = preOrder.takeAmount(p, amount);
						Logger.debug("taken: " + taken);
						Logger.debug("oAmount2: " + preOrder.getAmount());
						
					}else if (args[2].equalsIgnoreCase("give")){
						Logger.debug("oAmount1: " + preOrder.getAmount());
						int given = preOrder.giveAmount(p, amount);
						Logger.debug("given: " + given);
						Logger.debug("oAmount2: " + preOrder.getAmount());
						
					}
				}
			
			}
		}
		
		
		return true;
	}
}
