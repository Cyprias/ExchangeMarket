package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class SellOrderCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.SELL_ORDER))
			list.add("/%s sellorder - Create a sell order.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException {
		if (!Plugin.checkPermission(sender, Perm.SELL_ORDER)) {
			return false;
		}

		if (args.length <= 0 || args.length >= 4) {
			getCommands(sender, cmd);
			return true;
		}

		ItemStack stock = Plugin.getItemStack(args[0]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(sender, "Unknown item: " + args[0]);
			return true;
		}

		Logger.debug( "item: " + stock.getType());

		Player player = (Player) sender;

		int intAmount = InventoryUtil.getAmount(stock, player.getInventory());

		if (intAmount == 0) {
			ChatUtils.error(sender, "You do not have any " + stock.getType());
			return true;
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

		
		Logger.debug("amount1: " + amount);

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

		Order order = new Order(Order.SELL_ORDER, false, sender.getName(), stock, price);
		
		
		if (price == 0) {
			
			try {
				Double lastPrice = Plugin.database.getLastPrice(order);
				if (lastPrice > 0){
					price = lastPrice;
					order.setPrice(price);
				}else{
					ChatUtils.error(sender, "Invalid price: " + 0);
					return true;
				}
				
			} catch (SQLException e) {
				e.printStackTrace();
				ChatUtils.error(sender, "An error has occured: " + e.getLocalizedMessage());
				return true;
			}
			

		} else if (price < Config.getDouble("properties.min-order-price")) {
			ChatUtils.error(sender, "Your price is too low.");
			return true;
		}

		
		if (amount <= 0){
			amount = intAmount;
		}else{
			amount = Math.min(amount, intAmount);
		}
		
		Logger.debug( "price1: " + price);

		
		
		Logger.debug("amount2: " + amount +", " + order.getAmount());
		stock.setAmount(amount);
		try {
			order.setAmount(amount);
		} catch (SQLException e) {
			e.printStackTrace();
			ChatUtils.error(sender, "An error has occured: " + e.getLocalizedMessage());
			return true;
		}
		
		
		
		if (!Config.getBoolean("properties.allow-damaged-gear") && Plugin.isGear(stock.getType()) && stock.getDurability() > 0) {
			// ExchangeMarket.sendMessage(sender, F("cannotPostDamagedOrder"));
			ChatUtils.send(sender, "You cannot sell damaged gear.");
			return true;
		}

		

		try {

			Order matchingOrder = Plugin.database.findMatchingOrder(order);
			if (matchingOrder != null) {
				ChatUtils.send(sender, "Found matching order " + matchingOrder.getId());

				int mAmount = matchingOrder.getAmount();

				if (matchingOrder.setAmount(mAmount + amount)) {
					ChatUtils.send(sender, "Increased amount " + mAmount + "+" + amount + "=" + matchingOrder.getAmount());
					InventoryUtil.remove(stock, player.getInventory());
				}
			} else {

				if (Plugin.database.insert(order)) {
					InventoryUtil.remove(stock, player.getInventory());
					int id = Plugin.database.getLastId();

					ChatUtils.send(sender, "Created sell order " + id);

				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			ChatUtils.error(sender, "An error has occured: " + e.getLocalizedMessage());
			return true;
		}

		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.SELL_ORDER, "/%s sellorder <item> [amount] [price]", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
