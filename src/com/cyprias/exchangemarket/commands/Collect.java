package com.cyprias.exchangemarket.commands;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.cyprias.exchangemarket.Database;
import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.Utilis.Utils;

public class Collect {
	private ExchangeMarket plugin;

	public Collect(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	private String F(String string, Object... args) {
		return ExchangeMarket.F(string, args);
	}

	private String L(String string) {
		return ExchangeMarket.L(string);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) throws SQLException {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.collect")) {
			return true;
		}

		Database.collectPendingBuys(sender);

		return true;
	}
}
