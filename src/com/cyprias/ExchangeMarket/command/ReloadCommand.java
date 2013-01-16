package com.cyprias.ExchangeMarket.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;

public class ReloadCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.RELOAD))
			list.add("/%s reload - Reload the plugin.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.RELOAD)) {
			return false;
		}

		Plugin instance = Plugin.getInstance();
		
		instance.reloadConfig();
		instance.getPluginLoader().disablePlugin(instance);
		instance.getPluginLoader().enablePlugin(instance);

		ChatUtils.send(sender, "Plugin reloaded.");

		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.CONSOLE;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.RELOAD, "/%s reload - Reload the plugin.", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
