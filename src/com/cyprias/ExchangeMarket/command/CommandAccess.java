package com.cyprias.ExchangeMarket.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public enum CommandAccess {
	CONSOLE, PLAYER, BOTH;

	public boolean hasAccess(CommandSender sender) {
		switch (this) {
		case CONSOLE:
			return sender instanceof ConsoleCommandSender;
		case PLAYER:
			return sender instanceof Player;
		default:
			return true;
		}
	}
}