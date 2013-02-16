package com.cyprias.ExchangeMarket.command;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;

public interface Listable {

	void listCommands(CommandSender sender, List<String> commands) throws SQLException;

}