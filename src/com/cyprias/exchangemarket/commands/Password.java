package com.cyprias.exchangemarket.commands;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.cyprias.exchangemarket.Database;
import com.cyprias.exchangemarket.ExchangeMarket;

public class Password {
	private ExchangeMarket plugin;

	public Password(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) throws SQLException {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.password")) {
			return true;
		}

		if (args.length < 2) {
			ExchangeMarket.sendMessage(sender, "§a/" + commandLabel + " password <password>");
			return true;
		}

		Database.setPassword(sender, args[1].toString());
		return true;
	}
}
