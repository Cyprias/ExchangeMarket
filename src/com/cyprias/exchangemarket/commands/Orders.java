package com.cyprias.exchangemarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.ItemDb;
import com.cyprias.exchangemarket.Localization;
import com.cyprias.exchangemarket.Utilis.Utils;

public class Orders {
	private ExchangeMarket plugin;

	public Orders(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.orders")) {
			return true;
		}

		int page = -1;
		if (args.length > 1) {// && args[1].equalsIgnoreCase("compact"))
			if (Utils.isInt(args[1])) {
				page = Math.abs(Integer.parseInt(args[1]));
			} else {
				plugin.sendMessage(sender, F("invalidPageNumber", args[1]));
				return true;
			}
		}

		plugin.database.listPlayerOrders(sender, sender.getName(), page);

		return true;
	}
}
