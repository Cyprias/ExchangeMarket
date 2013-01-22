package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.xml.sax.SAXException;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.VersionChecker;
import com.cyprias.ExchangeMarket.VersionChecker.versionInfo;
import com.cyprias.ExchangeMarket.configuration.Config;

public class VersionCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.VERSION))
			list.add("/%s version - Get the plugin version.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.VERSION)) {
			return false;
		}
		/*
		 * if (args.length == 0){ getCommands(sender, cmd); return true; }
		 */

		final Plugin instance = Plugin.getInstance();

		if (Config.getBoolean("properties.check-new-version")) {

			instance.getServer().getScheduler().runTaskAsynchronously(instance, new Runnable() {
				public void run() {
					try {
						VersionChecker version = new VersionChecker("http://dev.bukkit.org/server-mods/dynamicdroprate/files.rss");
						versionInfo info = (version.versions.size() > 0) ? version.versions.get(0) : null;
						String curVersion = instance.getDescription().getVersion();
						if (info != null) {

							if (VersionChecker.compareVersions(curVersion, info.getTitle()) < 0) {
								ChatUtils.send(sender, "§7We're running v" + curVersion + ", v" + info.getTitle() + " is available");
							} else {
								ChatUtils.send(sender, "§7We're running the latest version v" + curVersion);
							}
						} else {
							ChatUtils.send(sender, "§7We're running version v" + curVersion);
						}
					} catch (SAXException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ParserConfigurationException e) {
						e.printStackTrace();
					}

				}
			});
		} else {
			ChatUtils.send(sender, "§7We're running version v" + instance.getDescription().getVersion());
		}

		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.VERSION, "/%s version - Get the plugin version.", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
