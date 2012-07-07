package com.cyprias.exchangemarket;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.MaterialUtil;

class Commands implements CommandExecutor {
	private ExchangeMarket plugin;

	public Commands(ExchangeMarket plugin) {
		this.plugin = plugin;

	}

	
	private String getItemStatsMsg(Database.itemStats stats, int stackCount) {
		int roundTo = Config.priceRounding;
		if (stats.total == 0)
			return "§7items: §f0";

		// "§7Total: §f" + stats.total

		return "§7items: §f"
			+ plugin.Round(stats.totalAmount, 0)
			// "§7, price: $§f" + Database.Round(stats.avgPrice * stackCount,
			// roundTo) + "/" + Database.Round(stats.median * stackCount,
			// roundTo) + "/" + Database.Round(stats.mode * stackCount, roundTo)
			+ "§7, avg: $§f" + plugin.Round(stats.avgPrice * stackCount, roundTo) + "§7, med: $§f" + plugin.Round(stats.median * stackCount, roundTo)
			+ "§7, mod: $§f" + plugin.Round(stats.mode * stackCount, roundTo);
	}

	public boolean hasCommandPermission(CommandSender player, String permission) {
		if (plugin.hasPermission(player, permission)) {
			return true;
		}
		// sendMessage(player, F("stNoPermission", permission));
		plugin.sendMessage(player, F("noPermission", permission));

		return false;
	}

	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}

	public static String getFinalArg(final String[] args, final int start) {
		final StringBuilder bldr = new StringBuilder();
		for (int i = start; i < args.length; i++) {
			if (i != start) {
				bldr.append(" ");
			}
			bldr.append(args[i]);
		}
		return bldr.toString();
	}

	
	public static HashMap<String, String[]> lastRequest = new HashMap<String, String[]>();
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		return onCommand(sender, cmd, commandLabel, args, null);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args, Boolean confirmed) {
		// TODO Auto-generated method stub

		if (commandLabel.equalsIgnoreCase("em")) {
			Boolean dryrun = Config.confirmAllOrders;

			if (confirmed != null && confirmed == true)
				dryrun = false;
			
			final String message = getFinalArg(args, 0);
			plugin.info(sender.getName() + ": /" + cmd.getName() + " " + message);

			if (args.length == 0) {

				plugin.sendMessage(sender, F("pluginsCommands", plugin.pluginName));

				if (plugin.hasPermission(sender, "exchangemarket.sell"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " sell <itemName> [amount] [price[e]] §7- " + L("cmdSellDesc"));
				if (plugin.hasPermission(sender, "exchangemarket.buy"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " buy <itemName> [amount] [price[e]] §7- " + L("cmdBuyDesc"));

				if (plugin.hasPermission(sender, "exchangemarket.price"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " price [itemName] [amount] [sale/buy] §7- " + L("cmdPriceDesc"));

				if (plugin.hasPermission(sender, "exchangemarket.infbuy"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " infbuy <itemName> <price> §7- " + L("cmdInfBuyDesc"));
				if (plugin.hasPermission(sender, "exchangemarket.infsell"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " infsell <itemName> <price> §7- " + L("cmdInfSellDesc"));

				if (plugin.hasPermission(sender, "exchangemarket.search"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " search <itemName>§7- " + L("cmdSearchDesc"));
				if (plugin.hasPermission(sender, "exchangemarket.list"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " list [Buy/Sell]§7- " + L("cmdListDesc"));
				if (plugin.hasPermission(sender, "exchangemarket.orders"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " orders §7- " + L("cmdOrdersDesc"));
				if (plugin.hasPermission(sender, "exchangemarket.collect"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " collect §7- " + L("cmdCollectDesc"));
				if (plugin.hasPermission(sender, "exchangemarket.cancel"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " cancel <ID/Buy/Sell> [itemName] [amount] §7- " + L("cmdCancelDesc"));

				if (plugin.hasPermission(sender, "exchangemarket.remove"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " remove <ID> §7- " + L("cmdRemoveDesc"));
				
				
				if (plugin.hasPermission(sender, "exchangemarket.reload"))
					plugin.sendMessage(sender, "  §a/" + commandLabel + " reload §7- " + L("cmdReloadDesc"));

				return true;
			}

			Player player = (Player) sender;
			if (args[0].equalsIgnoreCase("price")) {
				if (!hasCommandPermission(sender, "exchangemarket.price")) {
					return true;
				}
				//if (args.length < 2) {
				//	plugin.sendMessage(sender, "§a/" + commandLabel + " price <itemName> [amount] [Buy/Sale] §7- " + L("cmdPriceDesc"));
				//	return true;
				//}
				
				ItemStack item = null;
				int amount = 1;
				if (args.length > 1) {
					item = ItemDb.getItemStack(args[1]);
				}else{
					item = player.getItemInHand();
					amount = item.getAmount();
				}
				
				if (item == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}


				
				if (args.length > 2) {

					if (isInt(args[2])) {
						amount = Integer.parseInt(args[2]);
					} else {
						plugin.sendMessage(sender, F("invalidAmount", args[2]));

						return true;
					}
				}
				
				
				
				item.setAmount(amount);
				// plugin.sendMessage(sender, "amount: " + amount);

				int type = 0;
				if (args.length > 3) {
					if (args[3].equalsIgnoreCase("sell")) {
						type = 1;
					} else if (args[3].equalsIgnoreCase("buy")) {
						type = 2;

					} else {
						plugin.sendMessage(sender, F("invalidType", args[3]));
						return true;
					}
				}

				Database.itemStats stats = plugin.database.getItemStats(item.getTypeId(), item.getDurability(), type);
				String itemName = plugin.itemdb.getItemName(item.getTypeId(), item.getDurability());
				
				
				
				plugin.sendMessage(sender, F("itemShort", itemName, amount));
				plugin.sendMessage(sender, getItemStatsMsg(stats, amount));

				return true;
				
			} else if (args[0].equalsIgnoreCase("version") && args.length == 1) {
				if (!hasCommandPermission(sender, "exchangemarket.version")) {
					return true;
				}

				plugin.queueVersionCheck((Player) sender);
				return true;
			} else if (args[0].equalsIgnoreCase("remove")) {
				if (!hasCommandPermission(sender, "exchangemarket.remove")) {
					return true;
				}
				
				if (args.length < 1) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " remove <ID> §7- " + L("cmdCancelDesc"));
					return true;
				}
				
				if (isInt(args[1])) {
					int success = plugin.database.removeOrder(sender, Integer.parseInt(args[1]));
					if (success>0){
						plugin.sendMessage(sender, L("removeSuccessful"));
					}else{
						plugin.sendMessage(sender, L("removeFailed"));
					}
					
					
				}else{
					plugin.sendMessage(sender, F("invalidOrderNumber", args[1]));
				}
				return true;
				
				
			//searchOrders
			} else if (args[0].equalsIgnoreCase("search")) {
				if (!hasCommandPermission(sender, "exchangemarket.search")) {
					return true;
				}
				ItemStack item = null;
				int amount = 1;
				if (args.length > 1) {
					item = ItemDb.getItemStack(args[1]);
				}
				
				if (item == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}
				
				
				
				plugin.database.searchOrders(sender, item.getTypeId(), item.getDurability());
				
				return true;
				
			} else if (args[0].equalsIgnoreCase("cancel")) {
				if (!hasCommandPermission(sender, "exchangemarket.cancel")) {
					return true;
				}

				if (args.length < 2) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " cancel <ID/Buy/Sell> [itemName] [amount] §7- " + L("cmdCancelDesc"));
					return true;
				}

				if (isInt(args[1])) {
					plugin.database.cancelOrder(sender, Integer.parseInt(args[1]));
				}else{
					if (confirmed == null)
						dryrun = true;
					
					//plugin.sendMessage(sender, F("invalidOrderNumber", args[1]));
					//return true;
					int type = 0;
					if (args.length > 1) {
						if (args[1].equalsIgnoreCase("sell")) {
							type = 1;
						} else if (args[1].equalsIgnoreCase("buy")) {
							type = 2;

						} else {
							plugin.sendMessage(sender, F("invalidType", args[1]));
							return true;
						}
					}
					
					ItemStack item = null;
					int amount = 1;
					if (args.length > 2) {
						item = ItemDb.getItemStack(args[2]);
					}
					
					if (item == null) {
						plugin.sendMessage(sender, F("invalidItem", args[2]));
						return true;
					}
					
					if (args.length > 3) {

						if (isInt(args[3])) {
							amount = Integer.parseInt(args[3]);
						} else {
							plugin.sendMessage(sender, F("invalidAmount", args[3]));

							return true;
						}
					}
					String itemName = plugin.itemdb.getItemName(item.getTypeId(), item.getDurability());
					
					//plugin.info("type: " +type);
					//plugin.info("itemName: " +itemName);
					//plugin.info("amount: " +amount);
					
					int success = plugin.database.cancelOrders(sender, type, item.getTypeId(), item.getDurability(), amount, dryrun);
					//plugin.info("success: " +success);
					
					if (success > 0 && dryrun == true){
						lastRequest.put(sender.getName(), args);
						plugin.sendMessage(sender, L("confirmRequest"));
						
					}
					
					
				}

				

				return true;
			} else if (args[0].equalsIgnoreCase("orders")) {
				if (!hasCommandPermission(sender, "exchangemarket.orders")) {
					return true;
				}

				plugin.database.listPlayerOrders(sender, sender.getName());

				return true;
			} else if (args[0].equalsIgnoreCase("list")) {
				if (!hasCommandPermission(sender, "exchangemarket.list")) {
					return true;
				}

				int type = 0;
				if (args.length > 1) {
					if (args[1].equalsIgnoreCase("sell")) {
						type = 1;
					} else if (args[1].equalsIgnoreCase("buy")) {
						type = 2;

					} else {
						plugin.sendMessage(sender, F("invalidType", args[1]));
						return true;
					}
				}
				
			
				
				plugin.database.listOrders(sender, type);

				return true;

			} else if (args[0].equalsIgnoreCase("buy")) {
				if (!hasCommandPermission(sender, "exchangemarket.buy")) {
					return true;
				}
				if (args.length < 2) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " buy <itemName> <amount> [price[e]] §7- " + L("cmdBuyDesc"));
					return true;
				}

				
				
				
				ItemStack item = ItemDb.getItemStack(args[1]);

				if (item == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}

				int amount = 1;
				if (args.length > 2) {

					if (isInt(args[2])) {
						amount = Integer.parseInt(args[2]);
					} else {
						plugin.sendMessage(sender, F("invalidAmount", args[2]));
						return true;
					}
				}
				int rawAmount = amount;
				item.setAmount(amount);
				// plugin.sendMessage(sender, "amount: " + amount);
				
				
				
				
				
				double price = 0;
				// if (args.length > 2) {

				if (args.length > 3) {
					Boolean priceEach = false;
					
					if (args[3].substring(args[3].length() - 1, args[3].length()).equalsIgnoreCase("e")) {
						priceEach = true;
						args[3] = args[3].substring(0, args[3].length() - 1);
					}

					if (isDouble(args[3])) {
						price = Math.abs(Double.parseDouble(args[3]));
					} else {
						plugin.sendMessage(sender, F("invalidPrice", args[3]));
						return true;
					}
					if (price == 0) {
						plugin.sendMessage(sender, F("invalidPrice", 0));
						return true;
					}
					if (priceEach == false && Config.convertCreatePriceToPerItem == true)
						price = price / rawAmount;

				} else {

					//plugin.info("no price given.");
					if (confirmed == null)
						dryrun = Config.autoPriceConfirm;
					
					price = -1;
					
					/*
					Database.itemStats stats = plugin.database.getItemStats(item.getTypeId(), item.getDurability(), 0);//1

					//if (stats.total <= 0) {
					//	stats = plugin.database.getItemStats(item.getTypeId(), item.getDurability(), 0);//2
					//}
					
					if (stats.total <= 0) {
						plugin.sendMessage(sender, L("mustSupplyAPrice"));

						return true;
					}

					if (Config.autoPricePerUnit == true){
						price = stats.avgPrice + (Config.autoSellPrice*amount);
					}else{
						price = stats.avgPrice + Config.autoBuyPrice;
					}
					*/

				}
				// }

				
				// plugin.sendMessage(sender, "price: " + price);



				int success = plugin.database.processBuyOrder(sender, item.getTypeId(), item.getDurability(), amount, price, dryrun);
				
				if (success > 0 && dryrun == true){
					lastRequest.put(sender.getName(), args);
					plugin.sendMessage(sender, L("confirmRequest"));
					
				}
				
				return true;

			} else if (args[0].equalsIgnoreCase("infbuy")) {
				if (!hasCommandPermission(sender, "exchangemarket.infbuy")) {
					return true;
				}
				if (args.length < 3) {
					plugin.sendMessage(sender, "  §a/" + commandLabel + " infbuy <itemName> <price> §7- " + L("cmdInfBuyDesc"));
					return true;
				}

				ItemStack stock = ItemDb.getItemStack(args[1]);

				if (stock == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}
				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[2])) {
						price = Double.parseDouble(args[2]);
					} else {
						plugin.sendMessage(sender, F("invalidPrice", args[2]));
						return true;
					}
				}
				if (price == 0) {
					plugin.sendMessage(sender, F("invalidPrice", 0));
					return true;
				}
				// plugin.sendMessage(sender, "price: " + price);

				String itemName = plugin.itemdb.getItemName(stock.getTypeId(), stock.getDurability());

				
				
				int success = plugin.database.insertOrder(2, true, plugin.pluginName, stock.getTypeId(), stock.getDurability(), null, price, 1, dryrun);

				if (success > 0) {
					plugin.sendMessage(sender, F("infiniteBuyCreated", itemName, price));
				}
				return true;
			} else if (args[0].equalsIgnoreCase("infsell")) {
				if (!hasCommandPermission(sender, "exchangemarket.infsell")) {
					return true;
				}

				if (args.length < 3) {
					plugin.sendMessage(sender, "  §a/" + commandLabel + " infsell <itemName> <price> §7- " + L("cmdInfSellDesc"));
					return true;
				}
				ItemStack stock = ItemDb.getItemStack(args[1]);

				if (stock == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}
				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[2])) {
						price = Double.parseDouble(args[2]);
					} else {
						plugin.sendMessage(sender, F("invalidPrice", args[2]));
						return true;
					}
				}
				if (price == 0) {
					plugin.sendMessage(sender, F("invalidPrice", 0));
					return true;
				}
				// plugin.sendMessage(sender, "price: " + price);

				String itemName = plugin.itemdb.getItemName(stock.getTypeId(), stock.getDurability());

				int success = plugin.database.insertOrder(1, true, plugin.pluginName, stock.getTypeId(), stock.getDurability(), null, price, 1, dryrun);

				if (success > 0) {
					plugin.sendMessage(sender, F("infiniteSellCreated", itemName, price));
				}

				return true;
			} else if (args[0].equalsIgnoreCase("collect")) {
				if (!hasCommandPermission(sender, "exchangemarket.collect")) {
					return true;
				}
				
				plugin.database.collectPenderingBuys(sender);

				return true;
			} else if (args[0].equalsIgnoreCase("reload")) {
				if (!hasCommandPermission(sender, "exchangemarket.reload")) {
					return true;
				}

				plugin.config.reloadOurConfig();
				plugin.localization.loadLocales();
				plugin.sendMessage(sender, L("reloadedOurConfigs"));

				return true;
			} else if (args[0].equalsIgnoreCase("sell")) {
				if (!hasCommandPermission(sender, "exchangemarket.sell")) {
					return true;
				}

				if (args.length < 2) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " sell <itemName> [amount] [price] §7- " + L("cmdSellDesc"));
					return true;
				}
				
				
				
				ItemStack stock = ItemDb.getItemStack(args[1]);

				if (stock == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}

				//int invAmount = InventoryUtil.getAmount(stock, player.getInventory());
				// plugin.sendMessage(sender, "stock: " + stock);

				int invAmount = InventoryUtil.getAmount(stock, player.getInventory());
				int amount = invAmount;
				if (args.length > 2) {

					if (isInt(args[2])) {
						amount = Integer.parseInt(args[2]);
					} else {
						plugin.sendMessage(sender, F("invalidAmount", args[2]));
						return true;
					}
				}
				stock.setAmount(amount);
				int rawAmount = amount;
				// plugin.sendMessage(sender, "amount: " + amount);

				
				
				String itemName = plugin.itemdb.getItemName(stock.getTypeId(), stock.getDurability());
				//if (invAmount < amount) {
				//	amount = InventoryUtil.getAmount(stock, player.getInventory());
				//}

				amount = Math.min(invAmount, amount);
				
				if (amount == 0) {
					plugin.sendMessage(sender, F("sellNotEnoughItems", itemName, rawAmount));
					return true;
				}
				
				
				
				
				double price = 0;
				// if (args.length > 2) {

				if (args.length > 3) {
					Boolean priceEach = false;
					
					if (args[3].substring(args[3].length() - 1, args[3].length()).equalsIgnoreCase("e")) {
						priceEach = true;
						args[3] = args[3].substring(0, args[3].length() - 1);
					}

					if (isDouble(args[3])) {
						price = Math.abs(Double.parseDouble(args[3]));
					} else {
						plugin.sendMessage(sender, F("invalidPrice", args[3]));
						return true;
					}
					if (price == 0) {
						plugin.sendMessage(sender, F("invalidPrice", 0));
						return true;
					}
					if (priceEach == false && Config.convertCreatePriceToPerItem == true)
						price = price / rawAmount;
					
				} else {

					//plugin.info("no price given.");
					if (confirmed == null)
						dryrun = Config.autoPriceConfirm;
					
					price = -1;
				}


				int success = plugin.database.processSellOrder(sender, stock.getTypeId(), stock.getDurability(), amount, price, dryrun);
				
				if (success > 0 && dryrun == true){
					lastRequest.put(sender.getName(), args);
					plugin.sendMessage(sender, L("confirmRequest"));
				}

				return true;
			} else if (args[0].equalsIgnoreCase("confirm")) {
				
				if (!lastRequest.containsKey(sender.getName())){
					plugin.sendMessage(sender, L("noRequestYet"));
					return true;
				}

				
				onCommand(sender, cmd, commandLabel, lastRequest.get(sender.getName()), true);
				
				if (Config.clearRequestAfterConfirm == true)
					lastRequest.remove(sender.getName());
				
				return true;
			} else if (args[0].equalsIgnoreCase("sellhand")) {
				if (!hasCommandPermission(sender, "exchangemarket.sellhand")) {
					return true;
				}

				ItemStack item = player.getItemInHand();

				int type = 1;
				String playerName = sender.getName();
				int itemID = item.getTypeId();
				int itemDur = item.getDurability();
				String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);

				double price = 0;
				if (args.length > 1) {
					if (isDouble(args[1])) {
						price = Double.parseDouble(args[1]);
					} else {
						plugin.sendMessage(sender, F("invalidPrice", args[1]));
						return true;
					}
				}
				price = Math.abs(price);
				if (price == 0) {
					plugin.sendMessage(sender, F("invalidPrice", 0));
					return true;
				}

				int stock = item.getAmount();

				int success = plugin.database.insertOrder(type, false, playerName, itemID, itemDur, itemEnchants, price, stock, dryrun);

				plugin.sendMessage(sender, "success: " + success);

				if (success > 0 && dryrun == false) {
					InventoryUtil.remove(item, player.getInventory());
				}

				if (dryrun == true){
					lastRequest.put(sender.getName(), args);
					plugin.sendMessage(sender, "Type /em confirm to commit this order.");
				}
				
				return true;
				// insertOrder
			}
		}

		return false;
	}

	public static boolean isInt(final String sInt) {
		try {
			Integer.parseInt(sInt);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static boolean isDouble(final String sDouble) {
		try {
			Double.parseDouble(sDouble);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
