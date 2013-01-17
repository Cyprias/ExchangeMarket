package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;
import com.cyprias.ExchangeMarket.database.Parcel;

public class CollectCommand  implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.COLLECT))
			list.add("/%s collect - Collect pending items in your mailbox.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.COLLECT, "/%s collect", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException {
		if (!Plugin.checkPermission(sender, Perm.CONFIRM)) {
			return false;
		}
		
		
		List<Parcel> packages = Plugin.database.getPackages(sender);
		
		if (packages.size() <= 0){
			ChatUtils.send(sender, "§7You have no mail to collect.");
			return true;
		}
		
		Player player = (Player) sender;
		ItemStack stock;
		int leftover, canTake;
		boolean noFound = true;
		for (Parcel parcel : packages){
			
		
			stock = parcel.getItemStack();
			
			canTake = Plugin.getFitAmount(stock, parcel.getAmount(), player.getInventory());
			
			stock.setAmount(canTake);
			
			
			if (parcel.setAmount(parcel.getAmount() - canTake)){
				leftover = InventoryUtil.add(stock, player.getInventory());
				if (leftover > 0)
					parcel.setAmount(parcel.getAmount() + leftover);
				
				Logger.debug("canTake: " + canTake + ", leftover: " + leftover);
				
				
				//ChatUtils.send(sender, "Received " + stock.getType() + "x" + (canTake - leftover) + ", you have " + parcel.getAmount() +" left in your inbox.");
				
				ChatUtils.send(sender, String.format("§7Received §f%s§7x§f%s§7, you have §f%x §7remaining in your inbox.",
					stock.getType(), (canTake - leftover), parcel.getAmount()));
				
				noFound = false;
			}
			
			/*
			if (!InventoryUtil.fits(stock, player.getInventory()))
				continue;
				
			leftover = InventoryUtil.add(stock, player.getInventory());
			
			parcel.setAmount(leftover);*/
			
			
		}
		Plugin.database.cleanMailboxEmpties();
		
		
		if (noFound)
			ChatUtils.send(sender, "§7Failed to receive mail.");
		
		
		return true;
	}



}
