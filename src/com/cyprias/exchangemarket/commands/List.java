package com.cyprias.exchangemarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.Localization;
import com.cyprias.exchangemarket.Utilis.Utils;

public class List {
	private ExchangeMarket plugin;

	public List(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.list")) {
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

		int page = -1;
		if (args.length > 2) {// && args[1].equalsIgnoreCase("compact"))
			if (Utils.isInt(args[2])) {
				page = Math.abs(Integer.parseInt(args[2]));
			} else {
				plugin.sendMessage(sender, F("invalidPageNumber", args[2]));
				return true;
			}
		}

		plugin.database.listOrders(sender, type, page);

		return true;
	}
}
