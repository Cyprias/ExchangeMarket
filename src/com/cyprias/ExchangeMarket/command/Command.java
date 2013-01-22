package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

public interface Command extends Listable {

	boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException, IOException, InvalidConfigurationException;

	CommandAccess getAccess();

	void getCommands(CommandSender sender, org.bukkit.command.Command cmd);

	// Temprary work around for commands that run with 0 args
	//boolean hasValues();

}
