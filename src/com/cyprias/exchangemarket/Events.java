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
			// if (plugin.latestVersion == null) {
			// plugin.latestVersion = plugin.getLatestVersion();
			// }
			// String curVersion = plugin.getDescription().getVersion();

			// if (curVersion.compareTo(plugin.latestVersion) < 0) {
			// plugin.sendMessage(player, F("versionAvailable", ChatColor.RED +
			// curVersion, ChatColor.GREEN + plugin.latestVersion));
			// }

			// plugin.queueVersionCheck(player, false, true);
			plugin.versionChecker.retreiveVersionInfo(player);

		}
	}

	/**/
	@EventHandler(priority = EventPriority.NORMAL)
	public void onVersionCheckerEvent(VersionCheckerEvent event) {
		plugin.info("onVersionCheckerEvent: " + event.getPluginName());

		if (event.getPluginName() == plugin.getName()) {
			versionInfo info = event.getVersionInfo(0);
			
			if (info == null){
				
				
				
				return;
			}
			
			
			
			Object[] args = event.getArgs();
			if (args.length > 0){
				if (args[0] instanceof CommandSender) {
					
					String curVersion = plugin.getDescription().getVersion();
					
					

					
					
					
				}
			}
			
			for (int l = 0; l < args.length; l++) {
				plugin.info("onVersionCheckerEvent arg " + l + ": " + args[l]);
			}

			//

			plugin.info("onVersionCheckerEvent title: " + info.getTitle());
			plugin.info("onVersionCheckerEvent link: " + info.getLink());
			plugin.info("onVersionCheckerEvent pubDate: " + info.getPubDate());
			String[] desc = info.getDescription();

			for (int l = 0; l < desc.length; l++) {
				plugin.info("onVersionCheckerEvent description " + l + ": " + desc[l]);
			}
		}

	}

}
