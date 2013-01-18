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

public class InfSellCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.INF_SELL))
			list.add("/%s infsell - Create an infinite sell order (spawns items)");
	}

	public CommandAccess getAccess() {
		return CommandAccess.CONSOLE;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.INF_SELL, "/%s infsell <item> <amount> <price>", cmd);
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException {
		if (!Plugin.checkPermission(sender, Perm.INF_SELL)) 
			return false;
		
		if (args.length < 3 || args.length > 3) {
			getCommands(sender, cmd);
			return true;
		}
		ItemStack stock = Plugin.getItemStack(args[0]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(sender, "Unknown item: " + args[0]);
			return true;
		}

		//Logger.debug( "item: " + stock.getType());

		Player player = (Player) sender;

		int amount = 1;// InventoryUtil.getAmount(item, player.getInventory());
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
		
		//Logger.debug("amount1: " + amount);

		double price = 0;
		// plugin.sendMessage(sender, "amount: " + amount);

		if (args.length > 2) {

			// if (args.length > 2) {

			Boolean priceEach = false;

			if (args[2].substring(args[2].length() - 1, args[2].length()).equalsIgnoreCase("e")) {
				priceEach = true;
				price = Math.abs(Double.parseDouble(args[2].substring(0, args[2].length() - 1)));
			} else {

				if (Plugin.isDouble(args[2])) {
					price = Math.abs(Double.parseDouble(args[2]));
				} else {
					// ExchangeMarket.sendMessage(sender, F("invalidPrice",
					// args[3]));
					ChatUtils.error(sender, "Invalid price: " + args[2]);
					return true;
				}
				price = price / amount;

			}
		}

		
		
		
		if (price == 0) {
			ChatUtils.error(sender, "Invalid price: " + 0);
		} else if (price < Config.getDouble("properties.min-order-price")) {
			ChatUtils.error(sender, "Your price is too low.");
			return true;
		}

		stock.setAmount(amount);
		Order preOrder = new Order(Order.SELL_ORDER, true, sender.getName(), stock, price);

		if (!Config.getBoolean("properties.allow-damaged-gear") && Plugin.isGear(stock.getType()) && stock.getDurability() > 0) {
			// ExchangeMarket.sendMessage(sender, F("cannotPostDamagedOrder"));
			ChatUtils.error(sender, "You cannot sell damaged gear.");
			return true;
		}
		
		if (Plugin.database.insert(preOrder)) {
			
			int id = Plugin.database.getLastId();
			
			int pl = Config.getInt("properties.price-decmial-places");
			ChatUtils.send(sender,String.format("§7Created infinite sell order #§f%s §7for §f%s§7x§f%s §7@ §f%s §7(§f%s§7e)", id, Plugin.getItemName(stock), preOrder.getAmount(), Plugin.Round(preOrder.getPrice()*preOrder.getAmount(),pl), Plugin.Round(preOrder.getPrice(),pl)));
		}
		
		
		return false;
	}
	
	
}
