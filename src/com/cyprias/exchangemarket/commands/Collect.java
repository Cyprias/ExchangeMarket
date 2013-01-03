package com.cyprias.exchangemarket.commands;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.cyprias.exchangemarket.Database;
import com.cyprias.exchangemarket.ExchangeMarket;

public class Collect {
	private ExchangeMarket plugin;

	public Collect(ExchangeMarket plugin) {
		this.plugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) throws SQLException {
		if (!plugin.hasCommandPermission(sender, "exchangemarket.collect")) {
			return true;
		}

		Database.collectPendingBuys(sender);

		return true;
	}
}
