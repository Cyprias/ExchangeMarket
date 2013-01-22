package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
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
		Player player = (Player) sender;
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1) {
			ChatUtils.send(sender, "Cannot use ExchangeMarket while in creative mode.");
			return true;
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

		
		//Logger.debug("amount1: " + amount);

		double price = 0;
		// plugin.sendMessage(sender, "amount: " + amount);

		if (args.length > 2) {


			if (args[2].substring(args[2].length() - 1, args[2].length()).equalsIgnoreCase("e")) {
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
			ChatUtils.error(sender, "§7Your price is too low.");
			return true;
		}
		//Logger.debug( "price1: " + price);
		
		Double accountBalance = Econ.getBalance(sender.getName());
		//Logger.debug( "accountBalance: " + accountBalance);
		if (accountBalance <= (price*amount)){
			
			ChatUtils.send(sender, "§7Your account ($§f"+accountBalance+ "§7) does not have enough funds to supply this buy order.");
			return true;
		}
		
		

		try {

			Order matchingOrder = Plugin.database.findMatchingOrder(preOrder);
			if (matchingOrder != null) {
				//ChatUtils.send(sender, "Found matching order " + matchingOrder.getId());

				
				
				//int mAmount = matchingOrder.getAmount();

				if (matchingOrder.increaseAmount(amount)) {
					//
					ChatUtils.send(sender, String.format("§7Increased your existing buy order #§f%s §7of §f%s §7to §f%s§7.", matchingOrder.getId(), Plugin.getItemName(stock), matchingOrder.getAmount())); //amount
					
					//Econ.depositPlayer();
					
					EconomyResponse r = Econ.withdrawPlayer(sender.getName(), (price * amount));
					
		            if(r.transactionSuccess()) {
		            	ChatUtils.send(sender, String.format("$§f%s §7has been withdrawnfrom your account, you now have $§f%s§7.", r.amount, r.balance));
		            } else {
		            	ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
		            }
					
				}
			} else {

				if (Plugin.database.insert(preOrder)) {

					int id = Plugin.database.getLastId();

					int pl = Config.getInt("properties.price-decmial-places");
					ChatUtils.send(sender,String.format("§7Created buy order #§f%s §7for §f%s§7x§f%s §7@ §f%s §7(§f%s§7e)", id, Plugin.getItemName(stock), preOrder.getAmount(), Plugin.Round(preOrder.getPrice()*preOrder.getAmount(),pl), Plugin.Round(preOrder.getPrice(),pl)));

					EconomyResponse r = Econ.withdrawPlayer(sender.getName(), (price * amount));
					
					
					
		            if(r.transactionSuccess()) {
		            	ChatUtils.send(sender, String.format("$§f%s §7has been withdrawnfrom your account, you now have $§f%s§7.", r.amount, r.balance));
				           } else {
				      	   ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
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
