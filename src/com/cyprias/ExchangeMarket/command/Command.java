package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;

import org.bukkit.command.CommandSender;

public interface Command extends Listable {

	boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException;

	CommandAccess getAccess();

	void getCommands(CommandSender sender, org.bukkit.command.Command cmd);

	// Temprary work around for commands that run with 0 args
	//boolean hasValues();

}
