package com.cyprias.exchangemarket;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.MaterialUtil;

class Commands implements CommandExecutor {
	private ExchangeMarket plugin;

	public Commands(ExchangeMarket plugin) {
		this.plugin = plugin;

	}

	private String getItemStatsMsg(Database.itemStats stats, int stackCount) {
		int roundTo = 2;
		if (stats.total == 0)
			return "§7items: §f0";

		//"§7Total: §f" + stats.total

		return "§7items: §f"
			+ plugin.Round(stats.totalAmount, 0)
			// "§7, price: $§f" + Database.Round(stats.avgPrice * stackCount,
			// roundTo) + "/" + Database.Round(stats.median * stackCount,
			// roundTo) + "/" + Database.Round(stats.mode * stackCount, roundTo)
			+ "§7, avg: $§f" + plugin.Round(stats.avgPrice * stackCount, roundTo) + "§7, median: $§f" + plugin.Round(stats.median * stackCount, roundTo)
			+ "§7, mode: $§f" + plugin.Round(stats.mode * stackCount, roundTo);
	}
	
	public boolean hasCommandPermission(CommandSender player, String permission){
		if (plugin.hasPermission(player,permission)) {
			return true;
		}
		//sendMessage(player, F("stNoPermission", permission));
		plugin.sendMessage(player, "You do not have access to " + permission);
		
		return false;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		// TODO Auto-generated method stub
		
		if (commandLabel.equalsIgnoreCase("em")) {
			if (args.length == 0) {
				plugin.sendMessage(sender, "  §aMissing command...");
				return true;
			}
			
			Player player = (Player) sender;
			if (args[0].equalsIgnoreCase("test1")) {
				if (!hasCommandPermission(sender, "exchangemarket.test1")) {
					return true;
				}
				
				ItemStack item = player.getItemInHand();
				
				plugin.sendMessage(sender, "a: " + MaterialUtil.getName(item));
				plugin.sendMessage(sender, "b: " + MaterialUtil.getSignName(item));
				plugin.sendMessage(sender, "b: " + MaterialUtil.Enchantment.encodeEnchantment(item));
				
				
				
				
				
				return true;
			} else if (args[0].equalsIgnoreCase("price")) {
				if (!hasCommandPermission(sender, "exchangemarket.price")) {
					return true;
				}
				
				ItemStack stock = ItemDb.getItemStack(args[1]);
				if (stock == null){
					plugin.info("Invalid item: " + args[1]);
					return true;
				}
				
				int amount = 1;
				if (args.length > 2) {
					
					if (isInt(args[2])){
						amount = Integer.parseInt(args[2]);
					}else{
						plugin.sendMessage(sender, "Invalid amount: " + args[2]);
						return true;
					}
				}
				stock.setAmount(amount);
				plugin.sendMessage(sender, "amount: " + amount);
				
				
				Database.itemStats stats = plugin.database.getItemStats(stock.getTypeId(), stock.getDurability());
				
				plugin.sendMessage(sender, getItemStatsMsg(stats, amount));
				
				
				
				return true;
			} else if (args[0].equalsIgnoreCase("cancel")) {
				if (!hasCommandPermission(sender, "exchangemarket.cancel")) {
					return true;
				}
				
				if (args.length < 2){
					plugin.sendMessage(sender, "Include the order number.");
					return true;
				}
				
				if (!isInt(args[1])){
					plugin.sendMessage(sender, "Invalid order number: " + args[1]);
					return true;
				}
				
				
				
				plugin.database.cancelOrder(sender, Integer.parseInt(args[1]));
				
				return true;
			} else if (args[0].equalsIgnoreCase("orders")) {
				if (!hasCommandPermission(sender, "exchangemarket.orders")) {
					return true;
				}
				
				
				plugin.database.listPlayerOrders(sender, sender.getName());
				
				return true;
			} else if (args[0].equalsIgnoreCase("list")) {
				if (!hasCommandPermission(sender, "exchangemarket.list")) {
					return true;
				}
				
				plugin.database.listOrders(sender);
				
				return true;
				
			} else if (args[0].equalsIgnoreCase("buy")) {
				if (!hasCommandPermission(sender, "exchangemarket.buy")) {
					return true;
				}
				
				ItemStack item = ItemDb.getItemStack(args[1]);
				
				if (item == null){
					plugin.info("Invalid item: " + args[1]);
					return true;
				}
				
				int amount = 1;
				if (args.length > 1) {
					
					if (isInt(args[2])){
						amount = Integer.parseInt(args[2]);
					}else{
						plugin.sendMessage(sender, "Invalid amount: " + args[2]);
						return true;
					}
				}
				item.setAmount(amount);
				//plugin.sendMessage(sender, "amount: " + amount);
				
				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[3])){
						price = Double.parseDouble(args[3]);
					}else{
						plugin.sendMessage(sender, "Invalid price: " + args[3]);
						return true;
					}
				}
				if (price == 0){
					plugin.sendMessage(sender, "You need a price.");
					return true;
				}
				//plugin.sendMessage(sender, "price: " + price);
				
				
				
				
				plugin.database.processBuyOrder(sender, item.getTypeId(), item.getDurability(), amount, price);
				return true;
				
			} else if (args[0].equalsIgnoreCase("infbuy")) {
				if (!hasCommandPermission(sender, "exchangemarket.infbuy")) {
					return true;
				}
				if (args.length < 2) {
					plugin.sendMessage(sender, "Need more args.");
				}
				ItemStack stock = ItemDb.getItemStack(args[1]);
				
				if (stock == null){
					plugin.info("Invalid item: " + args[1]);
					return true;
				}
				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[2])){
						price = Double.parseDouble(args[2]);
					}else{
						plugin.sendMessage(sender, "Invalid price: " + args[2]);
						return true;
					}
				}
				if (price == 0){
					plugin.sendMessage(sender, "You need a price.");
					return true;
				}
				plugin.sendMessage(sender, "price: " + price);
				
				String itemName = plugin.itemdb.getItemName(stock.getTypeId(), stock.getDurability());
				
				
				
				int success = plugin.database.insertOrder(2, true, sender.getName(), stock.getTypeId(), stock.getDurability(), null, price, 1);
				
				if (success> 0){
					plugin.sendMessage(sender, "Infinite buy order created for " + itemName + " @ $" + price + " each.");
				}

			} else if (args[0].equalsIgnoreCase("infsell")) {
				if (!hasCommandPermission(sender, "exchangemarket.infsell")) {
					return true;
				}
				
				if (args.length < 2) {
					plugin.sendMessage(sender, "Need more args.");
				}
				ItemStack stock = ItemDb.getItemStack(args[1]);
				
				if (stock == null){
					plugin.info("Invalid item: " + args[1]);
					return true;
				}
				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[2])){
						price = Double.parseDouble(args[2]);
					}else{
						plugin.sendMessage(sender, "Invalid price: " + args[2]);
						return true;
					}
				}
				if (price == 0){
					plugin.sendMessage(sender, "You need a price.");
					return true;
				}
				plugin.sendMessage(sender, "price: " + price);
				
				String itemName = plugin.itemdb.getItemName(stock.getTypeId(), stock.getDurability());
				
				
				
				int success = plugin.database.insertOrder(1, true, sender.getName(), stock.getTypeId(), stock.getDurability(), null, price, 1);
				
				if (success> 0){
					plugin.sendMessage(sender, "Infinite sell order created for " + itemName + " @ $" + price + " each.");
					
					
				}
				
				return true;
			} else if (args[0].equalsIgnoreCase("sell")) {
				if (!hasCommandPermission(sender, "exchangemarket.sell")) {
					return true;
				}
				
				if (args.length < 3) {
					plugin.sendMessage(sender, "Need more args.");
				}
				
				ItemStack stock = ItemDb.getItemStack(args[1]);
				
				if (stock == null){
					plugin.info("Invalid item: " + args[1]);
					return true;
				}
				
				//plugin.sendMessage(sender, "stock: " + stock);

				int amount = 1;
				if (args.length > 1) {
					
					if (isInt(args[2])){
						amount = Integer.parseInt(args[2]);
					}else{
						plugin.sendMessage(sender, "Invalid amount: " + args[2]);
						return true;
					}
				}
				stock.setAmount(amount);
				//plugin.sendMessage(sender, "amount: " + amount);
				
				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[3])){
						price = Double.parseDouble(args[3]);
					}else{
						plugin.sendMessage(sender, "Invalid price: " + args[3]);
						return true;
					}
				}
				if (price == 0){
					plugin.sendMessage(sender, "You need a price.");
					return true;
				}
				//plugin.sendMessage(sender, "price: " + price);
				
				
				String itemName = plugin.itemdb.getItemName(stock.getTypeId(), stock.getDurability());
				if (InventoryUtil.getAmount(stock, player.getInventory()) < amount){
					//plugin.sendMessage(sender,"You do not have " + itemName + "x" + amount + " in your inv.");
					
					amount = InventoryUtil.getAmount(stock, player.getInventory());
				}
				
				

				plugin.database.processSellOrder(sender, stock.getTypeId(), stock.getDurability(), amount, price);
				
				
				/*
				String playerName = sender.getName(); 
				int success = plugin.database.insertOrder(
					1,
					false,
					playerName,
					stock.getTypeId(), 
					stock.getDurability(), 
					null, 
					price, 
					amount
				);
				
				if (success > 0) {
					InventoryUtil.remove(stock, player.getInventory());
					
					double each = plugin.Round(price/amount,2);
					plugin.sendMessage(sender, "Selling "  + itemName + "x" + amount + " for $" + price + " each.");
					
				}*/
				
				return true;
			} else if (args[0].equalsIgnoreCase("sellhand")) {
				if (!hasCommandPermission(sender, "exchangemarket.sellhand")) {
					return true;
				}
				
				ItemStack item = player.getItemInHand();

				int type = 1;
				String playerName = sender.getName(); 
				int itemID = item.getTypeId();
				int itemDur = item.getDurability();
				String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);

				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[1])){
						price = Double.parseDouble(args[1]);
					}else{
						plugin.sendMessage(sender, "Invalid price: " + args[1]);
						return true;
					}
				}
				if (price == 0){
					plugin.sendMessage(sender, "You need a price.");
					return true;
				}
				plugin.sendMessage(sender, "price: " + price);
				
				
				int stock = item.getAmount();
				
				int success = plugin.database.insertOrder(type,false,playerName,itemID, itemDur, itemEnchants, price, stock);
				
				plugin.sendMessage(sender, "success: " + success);
				
				if (success > 0){
					InventoryUtil.remove(item, player.getInventory());
				}
				
				return true;
			//insertOrder
			}
		}
		
		return false;
	}

	public static boolean isInt(final String sInt) {
		try {
			Integer.parseInt(sInt);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public static boolean isDouble(final String sDouble) {
		try {
			Double.parseDouble(sDouble);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
