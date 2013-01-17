package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class BuyOrderCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.BUY_ORDER))
			list.add("/%s buyorder - Create a buy order.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.BUY_ORDER)) {
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

		stock.setAmount(amount);
		Order preOrder = new Order(Order.BUY_ORDER, false, sender.getName(), stock, price);
		
		
		
		
		if (price == 0) {
			
			try {
				Double lastPrice = Plugin.database.getLastPrice(preOrder);
				if (lastPrice > 0){
					price = lastPrice;
					preOrder.setPrice(price);
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
		Logger.debug( "price1: " + price);
		
		Double accountBalance = Econ.getBalance(sender.getName());
		Logger.debug( "accountBalance: " + accountBalance);
		if (accountBalance <= (price*amount)){
			
			ChatUtils.send(sender, "Your account ($"+accountBalance+ ") does not have enough funds to supply this buy order.");
			return true;
		}
		
		
		/*SELLORDER
		if (amount <= 0){
			amount = intAmount;
		}else{
			amount = Math.min(amount, intAmount);
		}
		
		

		
		
		Logger.debug("amount2: " + amount +", " + order.getAmount());
		stock.setAmount(amount);
		try {
			order.setAmount(amount);
		} catch (SQLException e) {
			e.printStackTrace();
			ChatUtils.error(sender, "An error has occured: " + e.getLocalizedMessage());
			return true;
		}
		*/
		
		/*SELLORDER
		if (!Config.getBoolean("properties.allow-damaged-gear") && Plugin.isGear(stock.getType()) && stock.getDurability() > 0) {
			// ExchangeMarket.sendMessage(sender, F("cannotPostDamagedOrder"));
			ChatUtils.send(sender, "You cannot sell damaged gear.");
			return true;
		}
		 */
		

		try {

			Order matchingOrder = Plugin.database.findMatchingOrder(preOrder);
			if (matchingOrder != null) {
				ChatUtils.send(sender, "Found matching order " + matchingOrder.getId());

				//int mAmount = matchingOrder.getAmount();

				if (matchingOrder.increaseAmount(amount)) {
					ChatUtils.send(sender, "Increased existing order's amount by " + amount + ".");
					
					//Econ.depositPlayer();
					
					EconomyResponse r = Econ.withdrawPlayer(sender.getName(), (price * amount));
					
		            if(r.transactionSuccess()) {
		                sender.sendMessage(String.format("%s has been withdrawnfrom your account, you now have %s.", Econ.format(r.amount), Econ.format(r.balance)));
		            } else {
		                sender.sendMessage(String.format("An error occured: %s", r.errorMessage));
		            }
					
				}
			} else {

				if (Plugin.database.insert(preOrder)) {

					int id = Plugin.database.getLastId();

					ChatUtils.send(sender, "Created sell order " + id);

					EconomyResponse r = Econ.withdrawPlayer(sender.getName(), (price * amount));
					
		            if(r.transactionSuccess()) {
		                sender.sendMessage(String.format("%s has been withdrawnfrom your account, you now have %s.", Econ.format(r.amount), Econ.format(r.balance)));
		            } else {
		                sender.sendMessage(String.format("An error occured: %s", r.errorMessage));
		            }
					
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
		ChatUtils.sendCommandHelp(sender, Perm.BUY_ORDER, "/%s buyorder <item> [amount] [price]", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
