package com.cyprias.exchangemarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.cyprias.exchangemarket.Config;
import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.ItemDb;
import com.cyprias.exchangemarket.Localization;
import com.cyprias.exchangemarket.Utilis.Utils;

public class BuyOrder {
	private ExchangeMarket plugin;

	public BuyOrder(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.buyorder")) {
			return true;
		}
		if (args.length < 3) {
			plugin.sendMessage(sender, "§a/" + commandLabel + " buyorder <itemName> <amount> [price] §7- " + L("cmdBuyOrderDesc"));
			return true;
		}

		// //////////////

		ItemStack item = ItemDb.getItemStack(args[1]);

		if (item == null) {
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
		double price = 0;
		// plugin.sendMessage(sender, "amount: " + amount);

		if (args.length > 3) {

			// if (args.length > 2) {

			Boolean priceEach = false;

			if (args[3].substring(args[3].length() - 1, args[3].length()).equalsIgnoreCase("e")) {
				priceEach = true;
				args[3] = args[3].substring(0, args[3].length() - 1);
			}

			if (Utils.isDouble(args[3])) {
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

			// price = plugin.Round(price, Config.priceRounding);

		} else {
			price = plugin.database.getTradersLastPrice(sender.getName(), item.getTypeId(), item.getDurability(), 2);
		}

		if (price == 0 || price < Config.minOrderPrice) {
			plugin.sendMessage(sender, F("invalidPrice", price));
			return true;
		}

		if (Config.allowDamangedGear == false && plugin.isGear(item.getType()) && item.getDurability() > 0){
			plugin.sendMessage(sender, F("cannotPostDamagedOrder"));
			return true;
		}
		
		// postBuyOrder
		String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
		
		plugin.database.postBuyOrder(sender, item.getTypeId(), item.getDurability(), itemEnchants, amount, price, false);

		// //////////////
		return true;
	}
}
