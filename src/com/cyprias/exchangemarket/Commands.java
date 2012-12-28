package com.cyprias.exchangemarket;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;
import org.mindrot.jbcrypt.BCrypt;

import com.cyprias.Utils.InventoryUtil;
import com.cyprias.Utils.MaterialUtil;
import com.cyprias.exchangemarket.Utilis.Utils;
import com.cyprias.exchangemarket.commands.BuyOrder;
import com.cyprias.exchangemarket.commands.Collect;
import com.cyprias.exchangemarket.commands.ItemInfo;
import com.cyprias.exchangemarket.commands.List;
import com.cyprias.exchangemarket.commands.Orders;
import com.cyprias.exchangemarket.commands.Password;
import com.cyprias.exchangemarket.commands.Price;
import com.cyprias.exchangemarket.commands.Search;
import com.cyprias.exchangemarket.commands.SellHand;
import com.cyprias.exchangemarket.commands.SellOrder;
import com.cyprias.exchangemarket.commands.Transactions;

public class Commands implements CommandExecutor {
	protected ExchangeMarket plugin;

	private ItemInfo itemInfo;
	private Price price;
	private SellOrder sellOrder;
	private Search search;
	private Orders orders;
	private List list;
	private BuyOrder buyOrder;
	private Collect collect;
	private Transactions transactions;
	private Password password;
	private SellHand sellHand;
	
	//CommandExecutor info = null;
	public Commands(ExchangeMarket plugin) {
		this.plugin = plugin;
		this.itemInfo = new ItemInfo(plugin);
		this.price = new Price(plugin);
		this.sellOrder = new SellOrder(plugin);
		this.search = new Search(plugin);
		this.orders = new Orders(plugin);
		this.list = new List(plugin);
		this.buyOrder = new BuyOrder(plugin);
		this.collect = new Collect(plugin);
		this.transactions = new Transactions(plugin);
		this.password = new Password(plugin);
		this.sellHand = new SellHand(plugin);
	}

	public String getItemStatsMsg(Database.itemStats stats, int stackCount) {
		int roundTo = Config.priceRounding;
		if (stats.total == 0)
			return "§7items: §f0";

		// "§7Total: §f" + stats.total

		return "§7Items: §f"
			+ plugin.Round(stats.totalAmount, 0)
			+ "§7, avg: $§f" + plugin.Round(stats.avgPrice * stackCount, roundTo) + "§7, med: $§f" + plugin.Round(stats.median * stackCount, roundTo)
			+ "§7, mod: $§f" + plugin.Round(stats.mode * stackCount, roundTo);
	}

	public boolean hasCommandPermission(CommandSender player, String permission) {
		return plugin.hasCommandPermission(player, permission);
	}

	private String F(String string, Object... args) {
		return ExchangeMarket.F(string, args);
	}

	private String L(String string) {
		return ExchangeMarket.L(string);
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

	static class cmdRequest {
		CommandSender sender;
		Command cmd;
		String commandLabel;
		String[] args;
	}

	public class cmdTask implements Runnable {
		private int id = 0;

		public void setId(int n) {
			this.id = n;
		}

		// cmdRequest savedCmd = null;
		// public void addCommand(cmdRequest newCmd) {
		// savedCmd = newCmd;
		// }

		CommandSender sender;
		Command cmd;
		String commandLabel;
		String[] args;

		public void setSender(CommandSender value) {
			sender = value;
		}

		public void setCmd(Command value) {
			cmd = value;
		}

		public void setCommandLable(String value) {
			commandLabel = value;
		}

		public void setArgs(String[] value) {
			args = value;
		}

		public void run() {
			try{
				commandHandler(sender, cmd, commandLabel, args, null);
			} catch (Exception e) {e.printStackTrace();}

			cmdTasks.remove(this);
		}
	}

	public ArrayList<cmdTask> cmdTasks = new ArrayList<cmdTask>();

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args, Boolean confirmed) {
		if ((sender instanceof Player)) {
			Player player = (Player) sender;
			if (Config.blockUsageInCreativeMode == true && player.getGameMode().getValue() == 1) {
				plugin.sendMessage(sender, F("pleaseLeaveCreativeMode"));
				return true;
			}
		}

		for (int i = 0; i < cmdTasks.size(); i++) {
			if (cmdTasks.get(i).sender.equals(sender)) {
				plugin.sendMessage(sender, F("processingPreviousCMD"));
				return true;
			}
		}

		cmdTask task = new cmdTask();
		int taskID = plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, task, 0L);
		task.setId(taskID);
		task.setSender(sender);
		task.setCmd(cmd);
		task.setCommandLable(commandLabel);
		task.setArgs(args);
		// task.addCommand(newCmd);

		cmdTasks.add(task);

		return true;
	}

	public boolean commandHandler(CommandSender sender, Command cmd, String commandLabel, String[] args, Boolean confirmed) throws SQLException {
		// TODO Auto-generated method stub

		if (commandLabel.equalsIgnoreCase("em")) {
			Boolean dryrun = Config.confirmAllOrders;

			if (confirmed != null && confirmed == true)
				dryrun = false;

			final String message = getFinalArg(args, 0);
			plugin.info(sender.getName() + ": /" + cmd.getName() + " " + message);

			if (args.length == 0) {

				plugin.sendMessage(sender, F("pluginsCommands", plugin.pluginName));

				if (plugin.hasPermission(sender, "exchangemarket.buy") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " buy §7- " + L("cmdBuyDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.buyorder") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " buyorder §7- " + L("cmdBuyOrderDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.cancel") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " cancel §7- " + L("cmdCancelDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.collect") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " collect §7- " + L("cmdCollectDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.infbuy"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " infbuy §7- " + L("cmdInfBuyDesc"), true, false);
				if (plugin.hasPermission(sender, "exchangemarket.infsell"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " infsell §7- " + L("cmdInfSellDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.iteminfo"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " iteminfo [itemName] §7- " + L("cmdItemInfoDesc"), true, false);
				
				
				if (plugin.hasPermission(sender, "exchangemarket.list") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " list [Buy/Sell] [page]§7- " + L("cmdListDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.orders") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " orders [page] §7- " + L("cmdOrdersDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.password") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " password §7- " + L("cmdPasswordDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.price") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " price §7- " + L("cmdPriceDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.reload"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " reload §7- " + L("cmdReloadDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.remove"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " remove §7- " + L("cmdRemoveDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.search") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " search §7- " + L("cmdSearchDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.sell") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " sell §7- " + L("cmdSellDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.sellhand") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " sellhand [price] §7- " + L("cmdSellHandDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.sellorder") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " sellorder §7- " + L("cmdSellOrderDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.transactions") && (sender instanceof Player))
					plugin.sendMessage(sender, "§a/" + commandLabel + " transactions [page] §7- " + L("cmdTransactionsDesc"), true, false);

				if (plugin.hasPermission(sender, "exchangemarket.version"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " version §7- " + L("cmdVersionDesc"), true, false);
				if (plugin.hasPermission(sender, "exchangemarket.whatsnew"))
					plugin.sendMessage(sender, "§a/" + commandLabel + " whatsnew §7- " + L("cmdWhatsnewDesc"), true, false);

				return true;
			}

			//

			if (args[0].equalsIgnoreCase("version") && args.length == 1) {
				if (!hasCommandPermission(sender, "exchangemarket.version")) {
					return true;
				}

				// plugin.queueVersionRSS();

				// plugin.queueVersionCheck(sender, false, false);

				VersionChecker.retreiveVersionInfo(plugin,"http://dev.bukkit.org/server-mods/exchangemarket/files.rss", sender, false, false);

				return true;

			} else if (args[0].equalsIgnoreCase("whatsnew") && args.length == 1) {
				if (!hasCommandPermission(sender, "exchangemarket.whatsnew")) {
					return true;
				}

				// plugin.queueVersionRSS();

				VersionChecker.retreiveVersionInfo(plugin,"http://dev.bukkit.org/server-mods/exchangemarket/files.rss", sender, false, true);
				return true;
			} else if (args[0].equalsIgnoreCase("reload")) {
				if (!hasCommandPermission(sender, "exchangemarket.reload")) {
					return true;
				}

				plugin.config.reloadOurConfig();
				ExchangeMarket.loadLocales();
				plugin.sendMessage(sender, L("reloadedOurConfigs"));

				return true;
			} else if (args[0].equalsIgnoreCase("infbuy")) {
				if (!hasCommandPermission(sender, "exchangemarket.infbuy")) {
					return true;
				}
				if (args.length < 3) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " infbuy <itemName> <price> §7- " + L("cmdInfBuyDesc"));
					return true;
				}

				ItemStack item = ItemDb.getItemStack(args[1]);

				if (item == null || item.getType() == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}
				double price = 0;
				if (args.length > 1) {
					if (Utils.isDouble(args[2])) {
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

				String itemName = ItemDb.getItemName(item.getTypeId(), item.getDurability());
				String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
				
				
				int success = 0;
				if (dryrun == false)
					success = Database.insertOrder(2, true, plugin.pluginName, item.getTypeId(), item.getDurability(), itemEnchants, price, 1);

				if (success > 0) {
					plugin.sendMessage(sender, F("infiniteBuyCreated", itemName, price));
				}
				return true;
			} else if (args[0].equalsIgnoreCase("infsell")) {
				if (!hasCommandPermission(sender, "exchangemarket.infsell")) {
					return true;
				}

				if (args.length < 3) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " infsell <itemName> <price> §7- " + L("cmdInfSellDesc"));
					return true;
				}
				ItemStack item = ItemDb.getItemStack(args[1]);

				if (item == null || item.getType() == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}
				double price = 0;
				if (args.length > 1) {
					if (Utils.isDouble(args[2])) {
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

				String itemName = ItemDb.getItemName(item.getTypeId(), item.getDurability());
				String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
				
				int success = 0;
				if (dryrun == false)
					success = Database.insertOrder(1, true, plugin.pluginName, item.getTypeId(), item.getDurability(), itemEnchants, price, 1);

				if (success > 0) {
					plugin.sendMessage(sender, F("infiniteSellCreated", itemName, price));
				}

				return true;
			} else if (args[0].equalsIgnoreCase("remove")) {
				if (!hasCommandPermission(sender, "exchangemarket.remove")) {
					return true;
				}

				if (args.length < 2) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " remove <ID> §7- " + L("cmdCancelDesc"));
					return true;
				}

				if (Utils.isInt(args[1])) {
					int success = Database.removeOrder(sender, Integer.parseInt(args[1]));
					if (success > 0) {
						plugin.sendMessage(sender, L("removeSuccessful"));
					} else {
						plugin.sendMessage(sender, L("removeFailed"));
					}

				} else {
					plugin.sendMessage(sender, F("invalidOrderNumber", args[1]));
				}
				return true;
			}

			if (!(sender instanceof Player)){
				sender.sendMessage("You must be a player to issue that command.");
				return true;
			}
			
			Player player = (Player) sender;
			
			if (args[0].equalsIgnoreCase("price")) {
				return price.onCommand(sender, cmd, commandLabel, args);
			}else if (args[0].equalsIgnoreCase("iteminfo")) {
				return itemInfo.onCommand(sender, cmd, commandLabel, args);
			} else if (args[0].equalsIgnoreCase("search")) {
				return search.onCommand(sender, cmd, commandLabel, args);
			} else if (args[0].equalsIgnoreCase("orders")) {
				return orders.onCommand(sender, cmd, commandLabel, args);
				
			} else if (args[0].equalsIgnoreCase("list")) {
				return list.onCommand(sender, cmd, commandLabel, args);

			} else if (args[0].equalsIgnoreCase("sellorder")) {
				return sellOrder.onCommand(sender, cmd, commandLabel, args);
				
			} else if (args[0].equalsIgnoreCase("buyorder")) {
				return buyOrder.onCommand(sender, cmd, commandLabel, args);
			} else if (args[0].equalsIgnoreCase("collect")) {
				return collect.onCommand(sender, cmd, commandLabel, args);
			} else if (args[0].equalsIgnoreCase("transactions")) {
				return transactions.onCommand(sender, cmd, commandLabel, args);
			} else if (args[0].equalsIgnoreCase("password")) {
				return password.onCommand(sender, cmd, commandLabel, args);
			} else if (args[0].equalsIgnoreCase("sellhand")) {
				return sellHand.onCommand(sender, cmd, commandLabel, args);
				
				
			} else if (args[0].equalsIgnoreCase("cancel")) {
				if (!hasCommandPermission(sender, "exchangemarket.cancel")) {
					return true;
				}

				if (args.length < 2) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " cancel <ID/Buy/Sell> [itemName] [amount] §7- " + L("cmdCancelDesc"));
					return true;
				}

				if (Utils.isInt(args[1])) {
					Database.cancelOrder(sender, Integer.parseInt(args[1]));
				} else {
					if (confirmed == null)
						dryrun = true;

					// plugin.sendMessage(sender, F("invalidOrderNumber",
					// args[1]));
					// return true;
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

						if (Utils.isInt(args[3])) {
							amount = Integer.parseInt(args[3]);
						} else {
							plugin.sendMessage(sender, F("invalidAmount", args[3]));

							return true;
						}
					}
					String itemName = ItemDb.getItemName(item.getTypeId(), item.getDurability());

					// plugin.info("type: " +type);
					// plugin.info("itemName: " +itemName);
					// plugin.info("amount: " +amount);

					dryrun = false;
					String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
					int success = Database.cancelOrders(sender, type, item.getTypeId(), item.getDurability(), itemEnchants, amount, dryrun);
					// plugin.info("success: " +success);

					if (success > 0 && dryrun == true) {
						lastRequest.put(sender.getName(), args);
						plugin.sendMessage(sender, L("confirmRequest"));

					}

				}

				return true;

				
			} else if (args[0].equalsIgnoreCase("buy")) {
				if (!hasCommandPermission(sender, "exchangemarket.buy")) {
					return true;
				}
				if (args.length < 2) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " buy <itemName> <amount> [price] §7- " + L("cmdBuyDesc"));
					return true;
				}

				ItemStack item = ItemDb.getItemStack(args[1]);

				if (item == null || item.getTypeId() == 0) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}

				int amount = 1;
				if (args.length > 2) {

					if (Utils.isInt(args[2])) {
						amount = Integer.parseInt(args[2]);
					} else {
						plugin.sendMessage(sender, F("invalidAmount", args[2]));
						return true;
					}
				}
				int rawAmount = amount;
				item.setAmount(amount);
				// plugin.sendMessage(sender, "amount: " + amount);
				String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
				
				double price = ExchangeMarket.getBalance(sender.getName());
				//double price = Database.getUsersLastPrice(1, sender.getName(), item.getTypeId(), item.getDurability(), itemEnchants);//plugin.getBalance(sender.getName());
				//plugin.info("price: " + price);
				
				if (args.length > 3) {
					Boolean priceEach = false;
					
					String sPrice = args[3].trim();
					
					if (sPrice.substring(sPrice.length() - 1, sPrice.length()).equalsIgnoreCase("e")) {
						priceEach = true;
						sPrice = sPrice.substring(0, sPrice.length() - 1);
					}

					if (Utils.isDouble(sPrice)) {
						price = Math.abs(Double.parseDouble(sPrice));
					} else {
						plugin.sendMessage(sender, F("invalidPrice", sPrice));
						return true;
					}
					if (price == 0) {
						plugin.sendMessage(sender, F("invalidPrice", 0));
						return true;
					}
					if (priceEach == false)
						price = price / rawAmount;

				} else {

					// plugin.info("no price given.");
					if (confirmed == null)
						dryrun = Config.autoPriceConfirm;


				}
				// }

				// plugin.sendMessage(sender, "price: " + price);
				
				int success = Database.processBuyOrder(sender, item.getTypeId(), item.getDurability(), itemEnchants, amount, price, dryrun);

				if (success > 0 && dryrun == true) {
					lastRequest.put(sender.getName(), args);
					plugin.sendMessage(sender, L("confirmRequest"));

				}

				return true;




			} else if (args[0].equalsIgnoreCase("sell")) {
				if (!hasCommandPermission(sender, "exchangemarket.sell")) {
					return true;
				}

				if (args.length < 2) {
					plugin.sendMessage(sender, "§a/" + commandLabel + " sell <itemName> [amount] [price] §7- " + L("cmdSellDesc"));
					return true;
				}

				ItemStack item = ItemDb.getItemStack(args[1]);
				
				
				if (item == null || item.getType() == null) {
					plugin.sendMessage(sender, F("invalidItem", args[1]));
					return true;
				}
				
				
				
				int invAmount = InventoryUtil.getAmount(item, player.getInventory());
				int amount = invAmount;
				if (args.length > 2) {

					if (Utils.isInt(args[2])) {
						amount = Integer.parseInt(args[2]);
					} else {
						plugin.sendMessage(sender, F("invalidAmount", args[2]));
						return true;
					}
				}
				item.setAmount(amount);
				int rawAmount = amount;
				// plugin.sendMessage(sender, "amount: " + amount);


				String itemName = ItemDb.getItemName(item.getTypeId(), item.getDurability());
				String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
				if (itemEnchants != null)
					itemName += "-" + itemEnchants;

				amount = Math.min(invAmount, amount);

				if (amount == 0) {
					ExchangeMarket.sendMessage(sender, F("sellNotEnoughItems", itemName, rawAmount));
					return true;
				}

				double price = 0;
				//double price = Database.getUsersLastPrice(2, sender.getName(), item.getTypeId(), item.getDurability(), itemEnchants);//plugin.getBalance(sender.getName());
				//plugin.info("price: " + price);
				// if (args.length > 2) {

				if (args.length > 3) {
					Boolean priceEach = false;

					String sPrice = args[3].trim();
					if (sPrice.substring(sPrice.length() - 1, sPrice.length()).equalsIgnoreCase("e")) {
						priceEach = true;
						sPrice = sPrice.substring(0, sPrice.length() - 1);
					}

					if (Utils.isDouble(sPrice)) {
						price = Math.abs(Double.parseDouble(sPrice));
					} else {
						plugin.sendMessage(sender, F("invalidPrice", sPrice));
						return true;
					}
					if (price == 0) {
						plugin.sendMessage(sender, F("invalidPrice", 0));
						return true;
					}
					if (priceEach == false)
						price = price / rawAmount;

				} else {

					// plugin.info("no price given.");
					if (confirmed == null)
						dryrun = Config.autoPriceConfirm;

				}
				

				int success = Database.processSellOrder(sender, item.getTypeId(), item.getDurability(), itemEnchants, amount, price, dryrun);

				// if (success == 0){
				// plugin.sendMessage(sender, F("noBuyersForSell", itemName,
				// amount, price*amount, price));
				// }

				if (success > 0 && dryrun == true) {
					lastRequest.put(sender.getName(), args);
					plugin.sendMessage(sender, L("confirmRequest"));
				}

				return true;


				
			} else if (args[0].equalsIgnoreCase("confirm")) {
				if (!hasCommandPermission(sender, "exchangemarket.confirm")) {
					return true;
				}

				if (!lastRequest.containsKey(sender.getName())) {
					plugin.sendMessage(sender, L("noRequestYet"));
					return true;
				}

				commandHandler(sender, cmd, commandLabel, lastRequest.get(sender.getName()), true);

				if (Config.clearRequestAfterConfirm == true)
					lastRequest.remove(sender.getName());

				return true;


			}

			plugin.sendMessage(sender, F("invalidCommand", args[0]));

		}

		return false;
	}


}
