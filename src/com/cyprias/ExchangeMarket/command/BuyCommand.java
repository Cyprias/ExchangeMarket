package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingOrder;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingTranasction;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class BuyCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.BUY))
			list.add("/%s buy - Buy items from sell orders.");
	}


	

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.BUY)) 
			return false;
		

		Player player = (Player) sender;
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1) {
			ChatUtils.send(sender, "Cannot use ExchangeMarket while in creative mode.");
			return true;
		}
		
		if (args.length < 1 || args.length > 2) {
			getCommands(sender, cmd);
			return true;
		}

		ItemStack stock = Plugin.getItemStack(args[0]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(sender, "Unknown item: " + args[0]);
			return true;
		}

		
		int amount = 1;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 1) {
			if (Plugin.isInt(args[1]) && Integer.parseInt(args[1]) >= amount) {
				amount = Integer.parseInt(args[1]);
			} else {
				// ExchangeMarket.sendMessage(sender, F("invalidAmount",
				// args[2]));
				ChatUtils.error(sender, "Invalid amount: " + args[1]);
				return true;
			}
		}

		
		if (ConfirmCommand.pendingTransactions.containsKey(sender.getName()))
			ConfirmCommand.pendingTransactions.remove(sender.getName());

		if (ConfirmCommand.expiredTransactions.containsKey(sender.getName())) 
			ConfirmCommand.expiredTransactions.remove(sender.getName());	
		
	//	Order preOrder = new Order(Order.BUY_ORDER, false, sender.getName(), stock, price);
		
		//Double accountBalance = Econ.getBalance(sender.getName());
	
		try {
			List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER);
			
			Order o;
			
			if (!Config.getBoolean("properties.trade-to-yourself"))
				for (int i = (orders.size() - 1); i >= 0; i--) {
					o = orders.get(i); 
					if (sender.getName().equalsIgnoreCase(o.getPlayer()))
						orders.remove(o);
				}
			
			
			if (orders.size() <= 0){
				ChatUtils.send(sender, String.format("§7There are §f%s §7sell orders for §f%s§7, try creating a buy order.", orders.size(), Plugin.getItemName(stock)) );
				return true;
			//}else{
			//	ChatUtils.send(sender, String.format("§7There are §f%s §7sell orders for §f%s§7.", orders.size(), stock.getType()) );
			}
			
			
			
		//	String format = "§7Bought §f%s§7x§f%s §7@ $§f%s §7($§f%s§7e)";
			
			//String message;
			int dplaces = Config.getInt("properties.price-decmial-places");
			double moneySpent = 0;
			int itemsTraded = 0;
			
			int playerCanFit = Plugin.getFitAmount(stock, player.getInventory());
			

			
			pendingTranasction pT = new ConfirmCommand.pendingTranasction(player, new ArrayList<pendingOrder>(), Order.SELL_ORDER);
			ConfirmCommand.pendingTransactions.put(sender.getName(), pT);
			
			List<pendingOrder> pending = pT.pendingOrders; //ConfirmCommand.pendingOrders.get(sender.getName());
			
			//ConfirmCommand.pendingOrders.put(sender.getName(), value)
			
			if (Econ.getBalance(sender.getName()) <= 0){
				ChatUtils.send(sender, String.format("§7You have no money in your account."));
				return true;
			}
			
			for (int i=0; i<orders.size();i++){
				if (amount <= 0)
					break;
				
				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory()))
					break;
				
				o = orders.get(i);

				
				
				int canTrade = amount;
				if (!o.isInfinite())
					canTrade  = Math.min(o.getAmount(), amount);
				
				
				canTrade = (int) Math.floor(Math.min(canTrade, Econ.getBalance(sender.getName()) / o.getPrice()));
				


				//stock.setAmount(canBuy);
				canTrade = Math.min(canTrade, playerCanFit);
				
				if (canTrade <= 0)
					break;
				
				int traded = canTrade;//(canBuy - leftover);
				playerCanFit -= traded;
				
				double spend = (traded*o.getPrice());
				moneySpent += spend;
				
				pendingOrder po = new pendingOrder(o.getId(), traded);
				
				pending.add(po);
				
				
				
				Logger.debug(o.getId() + " x" + o.getAmount() + ", canTrade: " + canTrade + " (" + (canTrade * o.getPrice()) + ") traded: " + traded + ", player: " + o.getPlayer());

				//message = format.format(format, o.getItemType(), added, Plugin.Round((added*o.getPrice()),dplaces), Plugin.Round(o.getPrice(),dplaces));
				//ChatUtils.send(sender, "§a[Prevew] " + message);
				
				itemsTraded += traded;
				amount -=traded;

			}
			
			if (moneySpent > 0){
				ChatUtils.send(sender, String.format("§a[Estimate] §f%s§7x§f%s§7 will cost $§f%s§7, type §d/em confirm §7to confirm estimate.", Plugin.getItemName(stock), itemsTraded, Plugin.Round(moneySpent, dplaces)));
			}else{
				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory())){
					ChatUtils.send(sender, "You have no bag space available.");
				
				}else{
					
					ChatUtils.send(sender, "Failed to buy any items, try creating a buy order.");
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
		ChatUtils.sendCommandHelp(sender, Perm.BUY, "/%s buy <item> [amount]", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
