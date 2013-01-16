package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingOrder;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingTranasction;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class BuyCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.BUY))
			list.add("/%s buy - Buy items from sell orders.");
	}


	
	@Override
	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.BUY)) {
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

	//	Logger.debug( "item: " + stock.getType());

		//Player player = (Player) sender;
		
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

		
	//	Order preOrder = new Order(Order.BUY_ORDER, false, sender.getName(), stock, price);
		
		//Double accountBalance = Econ.getBalance(sender.getName());
		
		Player player = (Player) sender;
		
		try {
			List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER);
			
			ChatUtils.send(sender, "Orders: " + orders.size());
			
			Order o;
			int canBuy;
			String format = "§7Bought §f%s§7x§f%s §7@ $§f%s §7($§f%s§7e)";
			
			String message;
			int dplaces = Config.getInt("properties.price-decmial-places");
			double moneySpent = 0;
			int itemsBought = 0;
			
			int playerCanFit = Plugin.getFitAmount(stock, 64*36, player.getInventory());
			
			if (ConfirmCommand.pendingTransactions.containsKey(sender.getName()))
				ConfirmCommand.pendingTransactions.remove(sender.getName());

			pendingTranasction pT = new ConfirmCommand.pendingTranasction(player, new ArrayList<pendingOrder>(), Order.SELL_ORDER);
			ConfirmCommand.pendingTransactions.put(sender.getName(), pT);
			
			List<pendingOrder> pending = pT.pendingOrders; //ConfirmCommand.pendingOrders.get(sender.getName());
			
			//ConfirmCommand.pendingOrders.put(sender.getName(), value)
			
			for (int i=0; i<orders.size();i++){
				if (amount <= 0)
					break;
				
				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory()))
					break;
				
				o = orders.get(i);

				canBuy = Math.min(o.getAmount(), amount);
				canBuy = (int) Math.floor(Math.min(canBuy, Econ.getBalance(sender.getName()) / o.getPrice()));
				


				//stock.setAmount(canBuy);
				canBuy = Math.min(canBuy, playerCanFit);
				
				if (canBuy <= 0)
					break;
				
				int added = canBuy;//(canBuy - leftover);
				playerCanFit -= added;
				
				double spend = (added*o.getPrice());
				moneySpent += spend;
				
				pendingOrder po = new pendingOrder(o, added);
				
				pending.add(po);
				
				
				
				Logger.debug(o.getId() + " x" + o.getAmount() + ", canBuy: " + canBuy + " (" + (canBuy*o.getPrice())+ ") added: " + added);

				//message = format.format(format, o.getItemType(), added, Plugin.Round((added*o.getPrice()),dplaces), Plugin.Round(o.getPrice(),dplaces));
				//ChatUtils.send(sender, "§a[Prevew] " + message);
				
				itemsBought += added;
				amount -=added;

			}
			
			if (moneySpent > 0){
				
				
				ChatUtils.send(sender, String.format("§a[Estimite] §f%s§7x§f%s§7 costs $§f%s§7, type §d/em confirm §7to commit transaction.", stock.getType(), itemsBought, Plugin.Round(moneySpent, dplaces)));
				
	
				
			}else{
				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory())){
					ChatUtils.send(sender, "You have no bag space available.");
				
				}else{
					
					ChatUtils.send(sender, "Failed to buy any items, try creating a buy order.");
				}
			
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return true;

	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.BUY, "/%s buy <item> [amount]", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
