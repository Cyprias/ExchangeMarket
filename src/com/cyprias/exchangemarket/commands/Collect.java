package com.cyprias.exchangemarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.Localization;
import com.cyprias.exchangemarket.Utilis.Utils;

public class Collect {
	private ExchangeMarket plugin;

	public Collect(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.collect")) {
			return true;
		}

		plugin.database.collectPendingBuys(sender);

		return true;
	}
}
