package com.cyprias.exchangemarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.Localization;
import com.cyprias.exchangemarket.Utilis.Utils;

public class Password {
	private ExchangeMarket plugin;

	public Password(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.password")) {
			return true;
		}

		if (args.length < 2) {
			plugin.sendMessage(sender, "§a/" + commandLabel + " password <password>");
			return true;
		}

		String password = args[1].toString();

		plugin.database.setPassword(sender, password);

		return true;
	}
}
