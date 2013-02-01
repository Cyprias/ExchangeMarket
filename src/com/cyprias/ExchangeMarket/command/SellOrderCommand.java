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
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class SellOrderCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.SELL_ORDER))
			list.add("/%s sellorder - Create a sell order.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException, IOException,
		InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.SELL_ORDER))
			return false;

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

		int intAmount = InventoryUtil.getAmount(stock, player.getInventory());

		if (intAmount == 0) {
			ChatUtils.error(sender, "You do not have any " + Plugin.getItemName(stock));
			return true;
		}

		int amount = 0;// InventoryUtil.getAmount(item, player.getInventory());
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

		Logger.debug("amount1: " + amount);

		double price = 0;
		// plugin.sendMessage(sender, "amount: " + amount);

		if (args.length > 2) {

			if (args[2].substring(args[2].length() - 1, args[2].length()).equalsIgnoreCase("e")) {
				price = Double.parseDouble(args[2].substring(0, args[2].length() - 1));
			} else {

				if (Plugin.isDouble(args[2])) {
					price = Double.parseDouble(args[2]);
				} else {
					// ExchangeMarket.sendMessage(sender, F("invalidPrice",
					// args[3]));
					ChatUtils.error(sender, "Invalid price: " + args[2]);
					return true;
				}
				price = price / amount;

			}
			if (price <= 0) {
				ChatUtils.error(sender, "Invalid price: " + args[2]);
				return true;
			}
		}

		Order preOrder = new Order(Order.SELL_ORDER, false, sender.getName(), stock, price);

		if (price <= 0) {// price still zero, user never input it. lets find
							// their last price.

			Double lastPrice = Plugin.database.getLastPrice(preOrder);
			if (lastPrice > 0) {
				price = lastPrice;
				preOrder.setPrice(price);
			} else {
				ChatUtils.error(sender, "Invalid price: " + 0);
				return true;
			}

		} else if (price < Config.getDouble("properties.min-order-price")) {
			ChatUtils.error(sender, "Your price is too low.");
			return true;
		}

		if (amount <= 0) {
			amount = intAmount;
		} else {
			amount = Math.min(amount, intAmount);
		}

		// Logger.debug( "price1: " + price);

		Logger.debug("amount2: " + amount + ", " + preOrder.getAmount());
		stock.setAmount(amount);
		preOrder.setAmount(amount);

		if (!Config.getBoolean("properties.allow-damaged-gear") && Plugin.isGear(stock.getType()) && stock.getDurability() > 0) {
			// ExchangeMarket.sendMessage(sender, F("cannotPostDamagedOrder"));
			ChatUtils.error(sender, "You cannot sell damaged gear.");
			return true;
		}


		int pl = Config.getInt("properties.price-decmial-places");
		
		
		if (Config.getBoolean("properties.match-opposing-orders-before-posting")){
			List<Order> orders = Plugin.database.search(stock, Order.BUY_ORDER);
			Order o;
			
			if (!Config.getBoolean("properties.trade-to-yourself"))
				for (int i = (orders.size() - 1); i >= 0; i--) {
					o = orders.get(i); 
					if (sender.getName().equalsIgnoreCase(o.getPlayer()))
						orders.remove(o);
				}
			
			Logger.debug( "Orders: " + orders.size());
			
			if (orders.size() > 0){
				
				int traded;
				for (int i = (orders.size() - 1); i >= 0; i--) {
					if (amount <= 0)
						break;

					o = orders.get(i);
					
					if (o.getPrice() < price)
						continue;
					
					traded = amount;
					
					
					
					if (!o.isInfinite())
						traded = Math.min(o.getAmount(), traded);

					traded = Math.min(InventoryUtil.getAmount(stock, player.getInventory()), traded);

					if (traded <= 0)
						break;

					stock.setAmount(traded);

					// totalTraded = +po.amount;

					//Remove items from player's inventory for the buy order.
					InventoryUtil.remove(stock, player.getInventory());
					//Note, don't use takeAmount().
					
					if (!o.isInfinite()) {
						//Send the items to the orderer's mailbox.
						o.sendAmountToMailbox(traded);
						
						//Reduce the buy order's amount.
						o.reduceAmount(traded);
						
						//Notify the orderer of the transaction.
						o.notifyPlayerOfTransaction(traded);
					}
					
					
					double profit = (traded * o.getPrice());
					
					Econ.depositPlayer(sender.getName(), profit);
					o.insertTransaction(sender, traded);
					
					
					
					
					
					
					
					ChatUtils.send(
						sender,
						String.format("§7Sold §f%s§7x§f%s §7for $§f%s §7($§f%s§7e).", Plugin.getItemName(stock), traded, Plugin.Round(profit, pl),
							Plugin.Round(o.getPrice(), pl)));
					
					
					
					
					
					
					amount -= traded;
					
				}
				
				
			}
			
			
			
			Plugin.database.cleanEmpties();
		}
		if (amount <= 0)
			return true;
		
		if (Config.getDouble("taxes.sellOrder") > 0){
			//Logger.debug("taxes.sellOrder: " + Config.getDouble("taxes.sellOrder"));
			//Logger.debug("amount: " + amount);
			//Logger.debug("getPrice: " + preOrder.getPrice());

			double taxAmount = Config.getDouble("taxes.sellOrder") * (amount * preOrder.getPrice());
			//Logger.debug("taxAmount: " + taxAmount);
			
			if (Econ.getBalance(sender.getName()) < taxAmount){
				ChatUtils.send(sender, String.format("§7You do not have $§f%s §7needed to place that sell order.", Plugin.Round(taxAmount, Config.getInt("properties.price-decmial-places"))));
				return true;
			}
		}
		
		Order matchingOrder = Plugin.database.findMatchingOrder(preOrder);
		if (matchingOrder != null) {
			// ChatUtils.send(sender, "Found matching order " +
			// matchingOrder.getId());

			// int mAmount = matchingOrder.getAmount();
			// stock.setAmount(mAmount)


				
			
			amount = matchingOrder.takeAmount(player, amount);
			// if (matchingOrder.increaseAmount(amount)) {
			if (amount > 0) {
				// ChatUtils.send(sender, "Increased amount " + mAmount + "+" +
				// amount + "=" + matchingOrder.getAmount());

				ChatUtils.send(sender, String.format("§7Increased your existing sell order #§f%s §7of §f%s §7to §f%s§7.", matchingOrder.getId(),
					Plugin.getItemName(stock), matchingOrder.getAmount())); // amount

				// InventoryUtil.remove(stock, player.getInventory());

				// if (stock.getAmount() != amount)
				// Logger.warning("(A) We removed the wrong "+Plugin.getItemName(stock)
				// +" amount from " + sender.getName() + "'s inventory. stock: "
				// + stock.getAmount() + ", inserted: " + amount);

				ChatUtils.send(sender, String.format("§f%s§7x§f%s §7has been withdrawnfrom your inventory.", Plugin.getItemName(stock), amount));

				if (Config.getDouble("taxes.sellOrder") > 0){
					double taxAmount = Config.getDouble("taxes.sellOrder") * (amount * preOrder.getPrice());
					EconomyResponse r = Econ.withdrawPlayer(sender.getName(), taxAmount);
					if (r.transactionSuccess()) {
						ChatUtils.send(sender, String.format("$§f%s §7(§f%s§7%%) tax has been withdrawn from your account.", Plugin.Round(r.amount, Config.getInt("properties.price-decmial-places")), Plugin.Round(Config.getDouble("taxes.sellOrder") * 100)));
					} else {
						ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
					}
				}
				
			}
		} else {

			if (Plugin.database.insert(preOrder)) {

				int id = Plugin.database.getLastId();

				// ChatUtils.send(sender, "Created sell order " + id);

				ChatUtils.send(sender, String.format("§7Created sell order #§f%s §7for §f%s§7x§f%s §7@ §f%s §7(§f%s§7e)", id, Plugin.getItemName(stock),
					preOrder.getAmount(), Plugin.Round(preOrder.getPrice() * preOrder.getAmount(), pl), Plugin.Round(preOrder.getPrice(), pl)));

				InventoryUtil.remove(stock, player.getInventory());

				if (stock.getAmount() != (preOrder.getAmount()))
					Logger.warning("(B) We removed the wrong " + Plugin.getItemName(stock) + " amount from " + sender.getName() + "'s inventory. stock: "
						+ stock.getAmount() + ", inserted: " + (preOrder.getAmount()));

				ChatUtils.send(sender, String.format("§f%s§7x§f%s §7has been withdrawnfrom your inventory.", Plugin.getItemName(stock), stock.getAmount()));

				if (Config.getDouble("taxes.sellOrder") > 0){
					double taxAmount = Config.getDouble("taxes.sellOrder") * (amount * preOrder.getPrice());
					EconomyResponse r = Econ.withdrawPlayer(sender.getName(), taxAmount);
					if (r.transactionSuccess()) {
						ChatUtils.send(sender, String.format("$§f%s §7(§f%s§7%%) tax has been withdrawn from your account.", Plugin.Round(r.amount, Config.getInt("properties.price-decmial-places")), Plugin.Round(Config.getDouble("taxes.sellOrder")*100)));
					} else {
						ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
					}
				}
				
			}
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
