package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class CancelCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.CANCEL))
			list.add("/%s cancel - Cancel one of your orders.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.CANCEL, "/%s cancel <id> [amount]", cmd);
	}


	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.CANCEL)) 
			return false;
		
		Player player = (Player) sender;
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1) {
			ChatUtils.send(sender, "Cannot use ExchangeMarket while in creative mode.");
			return true;
		}
		
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
		
		Order order = Plugin.database.getOrder(id);
		if (order == null){
			ChatUtils.send(sender, "§7That order does not exist.");
			return true;
		}
		
		if (!sender.getName().equalsIgnoreCase(order.getPlayer())){
			ChatUtils.send(sender, "§7That order does not belong to you.");
			return true;
		}
		
		if (order.getOrderType() == Order.SELL_ORDER){
		
			ItemStack stock = order.getItemStack();
			
			int receive = Plugin.getFitAmount(stock, order.getAmount(), player.getInventory());
			
			if (amount>0)
				receive = Math.min(receive, amount);
			
			stock.setAmount(receive);
			InventoryUtil.add(stock, player.getInventory());
			
			order.reduceAmount(receive);
			
			ChatUtils.send(sender, String.format("§7Returned §f%s§7x§f%s§7, there's §f%s §7remaining in order #§f%s§7.", Plugin.getItemName(stock), receive, order.getAmount(), order.getId()));
			
			
		}else if (order.getOrderType() == Order.BUY_ORDER){
			//ChatUtils.send(sender, "Can't cancel buy yet.");
			
			double money = order.getPrice() * order.getAmount();
			
			Econ.depositPlayer(sender.getName(), money);
			order.setAmount(0);
			
			ChatUtils.send(sender, String.format("§7Returned your $§f%s§7.", Plugin.Round(money, Config.getInt("properties.price-decmial-places"))));
			
			
			
		}
		Plugin.database.cleanEmpties();
		
		return true;
	}
	
	
}
