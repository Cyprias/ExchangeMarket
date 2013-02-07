package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class ReturnCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.RETURN))
			list.add("/%s return - Return items from your sell order.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.ORDERS, "/%s return <item> [amount]", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.RETURN)) 
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
		Player player = (Player) sender;
		
		stock.setAmount(1);
		if (!InventoryUtil.fits(stock, player.getInventory())){
			ChatUtils.send(sender, String.format("§7You cannot fit anymore %s in your inventory.", Plugin.getItemName(stock)));
			return true;
		}
			
		
		
	//	Logger.debug( "item: " + stock.getType());

		//Player player = (Player) sender;
		
		int amount = 1;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 1) {
			if (Plugin.isInt(args[1]) && Integer.parseInt(args[1]) >= amount) {
				amount = Integer.parseInt(args[1]);
			} else {
				ChatUtils.error(sender, "Invalid amount: " + args[1]);
				return true;
			}
		}
		

		
		List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER, sender);
		
		if (orders.size() == 0){
			ChatUtils.send(sender, String.format("§7You now have sell orders for §f%s§7.", Plugin.getItemName(stock)) );
			return true;
		}
		
		int receive;
		Order order;
		for (int i = (orders.size() - 1); i >= 0; i--) {
			
			if (amount <= 0)
				break;
			order = orders.get(i);
			
			//stock = order.getItemStack();

			receive = Math.min(order.getAmount(), amount);

			receive = Math.min(receive, Plugin.getFitAmount(stock, player.getInventory()));
			
			if (receive <= 0)
				break;
			
			if (Config.getDouble("taxes.sellCancellation") > 0){
				//Logger.debug("taxes.sellOrder: " + Config.getDouble("taxes.sellOrder"));
				//Logger.debug("amount: " + amount);
				//Logger.debug("getPrice: " + preOrder.getPrice());

				double taxAmount = Config.getDouble("taxes.sellCancellation") * (amount * order.getPrice());
				//Logger.debug("taxAmount: " + taxAmount);
				
				if (Econ.getBalance(sender.getName()) < taxAmount){
					continue;
				}
				
				EconomyResponse r = Econ.withdrawPlayer(sender.getName(), taxAmount);
				if (r.transactionSuccess()) {
					ChatUtils.send(sender, String.format("$§f%s §7(§f%s§7%%) cancellation tax has been withdrawn from your account.", Plugin.Round(r.amount, Config.getInt("properties.price-decmial-places")), Plugin.Round(Config.getDouble("taxes.sellCancellation") * 100)));
				} else {
					ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
				}
			}
			
			
			receive = order.giveAmount(player, receive);
			
			
			ChatUtils.send(sender, String.format("§7Returned §f%s§7x§f%s§7, there's §f%s §7remaining in order #§f%s§7.", Plugin.getItemName(stock), receive, order.getAmount(), order.getId()));
			amount -= receive;
			
		}
		Plugin.database.cleanEmpties();
		
		
		
		
		return true;
	}
	

}
