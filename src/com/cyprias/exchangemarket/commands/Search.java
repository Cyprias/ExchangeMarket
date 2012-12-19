package com.cyprias.exchangemarket.commands;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.cyprias.exchangemarket.Database;
import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.ItemDb;
import com.cyprias.exchangemarket.Utilis.Utils;

public class Search {
	private ExchangeMarket plugin;

	public Search(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return ExchangeMarket.F(string, args);
	}

	private String L(String string) {
		return ExchangeMarket.L(string);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) throws SQLException {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.search")) {
			return true;
		}
		if (args.length <= 1) {
			// plugin.sendMessage(sender, F("invalidItem", ""));
			plugin.sendMessage(sender, "§a/" + commandLabel + " search <itemName> §7- " + L("cmdSearchDesc"));
			return true;
		}
		ItemStack item = null;
		if (args.length > 1) {
			item = ItemDb.getItemStack(args[1]);
		}

		if (item == null) {
			plugin.sendMessage(sender, F("invalidItem", args[1]));
			return true;
		}
		String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
		
		int page = 0;
		if (args.length > 2) {// && args[1].equalsIgnoreCase("compact"))
			if (Utils.isInt(args[2])) {
				page = Math.abs(Integer.parseInt(args[2]));
			} else {
				plugin.sendMessage(sender, F("invalidPageNumber", args[2]));
				return true;
			}
		}
		
		Database.searchOrders(sender, item.getTypeId(), item.getDurability(), itemEnchants, page);

		return true;
	}
	
}
