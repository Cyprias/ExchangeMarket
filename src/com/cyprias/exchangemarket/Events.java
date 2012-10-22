package com.cyprias.exchangemarket;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.cyprias.exchangemarket.VersionChecker.VersionCheckerEvent;
import com.cyprias.exchangemarket.VersionChecker.versionInfo;

public class Events implements Listener {
	private ExchangeMarket plugin;
	public static HashMap<String, String> aliases = new HashMap<String, String>();

	public Events(ExchangeMarket plugin) {
		this.plugin = plugin;

		FileConfiguration cfgAliases = plugin.yml.getYMLConfig("aliases.yml"); //

		String value;
		ConfigurationSection info;
		for (String aliase : cfgAliases.getKeys(false)) {
			aliases.put(aliase, cfgAliases.getString(aliase));
		}
	}

	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String msg = event.getMessage();
		String command = msg.split(" ")[0].replace("/", "");

		if (aliases.containsKey(command.toLowerCase())) {
			event.setMessage(msg.replaceFirst("/" + command, "/" + aliases.get(command.toLowerCase())));
			return;
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (player.hasPermission("exchangemarket.loginNewVersion")) {
			plugin.versionChecker.retreiveVersionInfo(player, true);
		}
	}

	/**/
	@EventHandler(priority = EventPriority.NORMAL)
	public void onVersionCheckerEvent(VersionCheckerEvent event) {

		if (event.getPluginName() == plugin.getName()) {
			versionInfo info = event.getVersionInfo(0);
			Object[] args = event.getArgs();

			String curVersion = plugin.getDescription().getVersion();

			if (args.length == 0) {
				if (info != null && !info.getTitle().equalsIgnoreCase(curVersion)) {
					plugin.info(F("versionAvailable", curVersion, info.getTitle()));
				} else {
					plugin.info(F("version", curVersion));
				}
				return;
			}

			if (args.length >= 1) {
				CommandSender sender = (CommandSender) args[0];

				Boolean silentNewestVersion = false, showChangelog = false;
				if (args.length >= 2)
					silentNewestVersion = (Boolean) args[1];
				if (args.length >= 3)
					showChangelog = (Boolean) args[2];

				if (info.getTitle().equalsIgnoreCase(curVersion)) {
					if (silentNewestVersion == true)
						return;

					plugin.sendMessage(sender, F("version", curVersion));

				} else {

					plugin.sendMessage(sender, F("versionAvailable", curVersion, info.getTitle()));

				}

				if (showChangelog == true) {

					String[] changes;
					for (int v = 0; v < event.getVersionCount() && v < 3; v++) {

						info = event.getVersionInfo(v);
						plugin.sendMessage(sender, F("versionChanges", info.getTitle()));

						changes = info.getDescription();

						for (int l = 0; l < changes.length; l++) {
							plugin.sendMessage(sender, (l + 1) + ChatColor.GRAY.toString() + ": " + ChatColor.WHITE.toString() + changes[l]);

						}

					}

				}
				return;

			}


		}
	}

}
