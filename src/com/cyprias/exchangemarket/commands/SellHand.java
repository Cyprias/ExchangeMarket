package com.cyprias.exchangemarket.commands;

import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.cyprias.exchangemarket.Config;
import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.Localization;
import com.cyprias.exchangemarket.Utilis.Utils;

public class SellHand {
	private ExchangeMarket plugin;

	public SellHand(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.sellhand")) {
			return true;
		}

		Player player = (Player) sender;
		
		ItemStack item = player.getItemInHand();
		Map<Enchantment, Integer> e = item.getEnchantments();

		int type = 1;
		String playerName = sender.getName();
		int itemID = item.getTypeId();
		int itemDur = item.getDurability();
		String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);

		int amount = item.getAmount();

		double price = 0;
		// plugin.sendMessage(sender, "amount: " + amount);

		if (args.length > 1) {

			// if (args.length > 2) {

			Boolean priceEach = false;

			if (args[1].substring(args[1].length() - 1, args[1].length()).equalsIgnoreCase("e")) {
				priceEach = true;
				args[1] = args[1].substring(0, args[1].length() - 1);
			}

			if (Utils.isDouble(args[1])) {
				price = Math.abs(Double.parseDouble(args[1]));
			} else {
				plugin.sendMessage(sender, F("invalidPrice", args[1]));
				return true;
			}
			if (price == 0) {
				plugin.sendMessage(sender, F("invalidPrice", 0));
				return true;
			}
			if (priceEach == false )
				price = price / amount;

			
		}else{
			price = plugin.database.getTradersLastPrice(1, sender.getName(), item.getTypeId(), item.getDurability(), itemEnchants);
		}

		if (price == 0 || price < Config.minOrderPrice) {
			plugin.sendMessage(sender, F("invalidPrice", price));
			return true;
		}
		
		if (Config.allowDamangedGear == false && plugin.isGear(item.getType()) && item.getDurability() > 0){
			plugin.sendMessage(sender, F("cannotPostDamagedOrder"));
			return true;
		}
		
		plugin.database.postSellOrder(sender, item.getTypeId(), item.getDurability(), itemEnchants, amount, price, false);

		return true;


	}
}
