package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.database.Order;

public class ReturnCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.CANCEL))
			list.add("/%s cancel - Cancel one of your orders.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.ORDERS, "/%s cancel <id> [amount]", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException {
		if (!Plugin.checkPermission(sender, Perm.CANCEL)) 
			return false;
		
		if (args.length <= 0 || args.length >= 3) {
			getCommands(sender, cmd);
			return true;
		}

		ItemStack stock = Plugin.getItemStack(args[0]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(sender, "Unknown item: " + args[0]);
			return true;
		}
		Player player = (Player) sender;
		
		stock.setAmount(1);
		if (!InventoryUtil.fits(stock, player.getInventory())){
			ChatUtils.send(sender, String.format("§7You cannot fit anymore %s in your inventory.", Plugin.getItemName(stock)));
			return true;
		}
			
		
		
	//	Logger.debug( "item: " + stock.getType());

		//Player player = (Player) sender;
		
		int amount = 1;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 1) {
			if (Plugin.isInt(args[1])) {
				amount = Integer.parseInt(args[1]);
			} else {
				ChatUtils.error(sender, "Invalid amount: " + args[1]);
				return true;
			}
		}
		
		List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER, sender);
		
		if (orders.size() == 0){
			ChatUtils.send(sender, String.format("§7You now have sell orders for §f%s§7.", Plugin.getItemName(stock)) );
			return true;
		}
		
		int receive;
		for (Order order: orders){
			
			if (amount <= 0)
				break;
			
			//stock = order.getItemStack();
			
			
			receive = Plugin.getFitAmount(stock, order.getAmount(), player.getInventory());
			
			if (amount>0)
				receive = Math.min(receive, amount);
			
			if (receive <= 0)
				break;
			
			stock.setAmount(receive);
			InventoryUtil.add(stock, player.getInventory());
			
			order.reduceAmount(receive);
			
			ChatUtils.send(sender, String.format("§7Returned §f%s§7x§f%s§7, there's §f%s §7remaining in order #§f%s§7.", Plugin.getItemName(stock), receive, order.getAmount(), order.getId()));
			amount -= receive;
			
		}
		Plugin.database.cleanEmpties();
		
		
		
		
		return true;
	}
	

}
