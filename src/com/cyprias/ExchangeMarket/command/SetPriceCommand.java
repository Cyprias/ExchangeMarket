package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class SetPriceCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.SET_PRICE))
			list.add("/%s setprice - Set the price for an existing order.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.SET_PRICE, "/%s setprice <orderID> <price>", cmd);
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException, IOException,
		InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.SET_PRICE))
			return false;

		if (args.length < 2 || args.length > 2) {
			getCommands(sender, cmd);
			return true;
		}

		int id = 0;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 0) {
			if (Plugin.isInt(args[0])) {
				id = Integer.parseInt(args[0]);
			} else {
				// ExchangeMarket.sendMessage(sender, F("invalidAmount",
				// args[2]));
				ChatUtils.error(sender, "Invalid id: " + args[0]);
				return true;
			}
		}

		Order order = Plugin.database.getOrder(id);
		if (order == null) {
			ChatUtils.send(sender, "§7That order does not exist.");
			return true;
		}

		if ((sender instanceof Player) && !sender.getName().equalsIgnoreCase(order.getPlayer())) {
			ChatUtils.send(sender, "§7That order does not belong to you.");
			return true;
		}

		if (order.getOrderType() == Order.BUY_ORDER){
			// Todo: add support to withdraw funds from player to supply the buy order later.
			ChatUtils.send(sender, "§7Cannot set price on buy orders, cancel the order and create it again.");
			return true;
		}
		
		double price = 0;
		if (args.length > 1) {

			if (args[1].substring(args[1].length() - 1, args[1].length()).equalsIgnoreCase("e")) {
				price = Double.parseDouble(args[1].substring(0, args[1].length() - 1));
			} else {

				if (Plugin.isDouble(args[1])) {
					price = Double.parseDouble(args[1]);
				} else {
					// ExchangeMarket.sendMessage(sender, F("invalidPrice",
					// args[3]));
					ChatUtils.error(sender, "Invalid price: " + args[1]);
					return true;
				}
				price = price / order.getAmount();

			}
			if (price <= 0) {
				ChatUtils.error(sender, "Invalid price: " + args[1]);
				return true;
			}
		}

		Logger.debug("getItemName price: " + price);
		
		if (price < Config.getDouble("properties.min-order-price")) {
			ChatUtils.error(sender, "§7Your price is too low.");
			return true;
		}
		
		order.setPrice(price);
		
		
		int pl = Config.getInt("properties.price-decmial-places");
		ChatUtils.send(sender, String.format("§7Set order #§f%s §7(§f%s§7x§f%s§7) to $§f%s §7($§f%s§7e)", id, order.getColourName(sender), order.getAmount(), Plugin.Round(order.getPrice()*order.getAmount(), pl), Plugin.Round(order.getPrice(), pl)));
		
		

		return true;
	}

}
