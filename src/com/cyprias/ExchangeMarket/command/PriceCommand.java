package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class PriceCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.PRICE))
			list.add("/%s price - Get the price of an item..");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.PRICE, "/%s price <item> [amount]", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.PRICE))
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
		
		try {
			List<Order> orders = Plugin.database.search(stock);

		//	ChatUtils.send(sender, "Orders: " + orders.size());

			Order order;
			String format = Config.getColouredString("properties.list-row-format");
			String message;
			int totalAmount = 0;
			double totalPrice = 0;

			int lowestAmount, highestAmount;
			double lowest, highest;
			
			//List<Double> prices = new ArrayList<Double>();
			if (orders.size() > 0){
				lowest = orders.get(0).getPrice();
				lowestAmount = orders.get(0).getAmount();
				
				highest = orders.get(orders.size()-1).getPrice();
				highestAmount = orders.get(orders.size()-1).getAmount();
				double[] dPrices = new double[orders.size()];
				
				for (int i = 0; i < orders.size(); i++) {
					order = orders.get(i);
					totalAmount += order.getAmount();
					totalPrice += order.getPrice() * order.getAmount();
					dPrices[i] = order.getPrice();
					
					
				}
				
				Logger.debug("totalAmount: " + totalAmount + ", totalPrice: " + totalPrice);
				
				
				ChatUtils.send(sender, String.format("§7There are §f%s §7orders containing §f%s %s§7.", orders.size(), totalAmount, Plugin.getItemName(stock)));
				
				
				
				int dplaces = Config.getInt("properties.price-decmial-places");
				
				if (orders.size() > 1)
					ChatUtils.send(sender, String.format("§7Lowest price: $§f%s §7(x§f%s§7), Highest price: $§f%s §7(x§f%s§7)",  Plugin.Round(lowest*amount,dplaces), lowestAmount, Plugin.Round(highest*amount,dplaces), highestAmount));
				
				
				double average = totalPrice / totalAmount;
				
				
				
				String mean = Plugin.Round(mean(dPrices)*amount,dplaces);
				String median = Plugin.Round(median(dPrices)*amount,dplaces);
				String mode = Plugin.Round(mode(dPrices)*amount,dplaces);
				
				ChatUtils.send(sender, String.format("§7Average: $§f%s§7, mean:$§f%s§7, med:$§f%s§7, mod:$§f%s§7.", Plugin.Round(average*amount,dplaces), mean, median, mode));
				
				
			}else{
				ChatUtils.send(sender, String.format("§7There are no orders containing §f%s§7.", Plugin.getItemName(stock)));
			}
			
			


		} catch (SQLException e) {
			e.printStackTrace();
			ChatUtils.error(sender, "An error has occured: " + e.getLocalizedMessage());
			return true;
		}

		return true;
	}

	public static double mean(double[] p) {
		double sum = 0; // sum of all the elements
		for (int i = 0; i < p.length; i++) {
			sum += p[i];
		}
		return sum / p.length;
	}// end method mean

	public static double median(double[] m) {
		int middle = m.length / 2;
		if (m.length % 2 == 1) {
			return m[middle];
		} else {
			return (m[middle - 1] + m[middle]) / 2.0;
		}
	}

	public static double mode(double[] prices) {
		double maxValue = 0, maxCount = 0;

		for (int i = 0; i < prices.length; ++i) {
			int count = 0;
			for (int j = 0; j < prices.length; ++j) {
				if (prices[j] == prices[i])
					++count;
			}
			if (count > maxCount) {
				maxCount = count;
				maxValue = prices[i];
			}
		}

		return maxValue;
	}
	
}
