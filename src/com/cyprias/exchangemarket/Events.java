package com.cyprias.exchangemarket;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
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

	public Events(ExchangeMarket plugin) throws FileNotFoundException, IOException, InvalidConfigurationException {
		this.plugin = plugin;

		YML yml = new YML(plugin.getResource("aliases.yml"),plugin.getDataFolder(), "aliases.yml");
		
		for (String key : yml.getKeys(false)) {
			Events.aliases.put(key, yml.getString(key));
		}
		
	}

	private String F(String string, Object... args) {
		return ExchangeMarket.F(string, args);
	}

	@SuppressWarnings("unused")
	private String L(String string) {
		return ExchangeMarket.L(string);
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

		if (player.hasPermission("exchangemarket.loginNewVersion")) 
			VersionChecker.retreiveVersionInfo(plugin, "http://dev.bukkit.org/server-mods/exchangemarket/files.rss", player, true);
		
		
		if (player.hasPermission("exchangemarket.loginPendingCollection")) 
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Database.checkPendingBuysTask(player));

		//, 
		
	}

	/**/
	@EventHandler(priority = EventPriority.NORMAL)
	public void onVersionCheckerEvent(VersionCheckerEvent event) {

		if (event.getPluginName() == plugin.getName()) {
			versionInfo info = event.getVersionInfo(0);
			Object[] args = event.getArgs();

			String curVersion = plugin.getDescription().getVersion();

			if (args.length == 0) {
				
				int compare = VersionChecker.compareVersions(curVersion, info.getTitle());
				//plugin.info("curVersion: " + curVersion +", title: " + info.getTitle() + ", compare: " + compare);
				if (compare < 0){
					ExchangeMarket.info(F("versionAvailable", curVersion, info.getTitle()));
					ExchangeMarket.info(info.getLink());
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

				int compare = VersionChecker.compareVersions(curVersion, info.getTitle());
				//plugin.info("curVersion: " + curVersion +", title: " + info.getTitle() + ", compare: " + compare);
				if (compare < 0){
					ExchangeMarket.sendMessage(sender, F("versionAvailable", curVersion, info.getTitle()));
				}else{
					if (silentNewestVersion == true)
						return;
					
					ExchangeMarket.sendMessage(sender, F("version", curVersion));
				}
				
				if (showChangelog == true) {

					String[] changes;
					for (int v = 0; v < event.getVersionCount(); v++) { // && v < 3
						info = event.getVersionInfo(v);
						
						compare = VersionChecker.compareVersions(curVersion, info.getTitle());
						//plugin.info("curVersion: " + curVersion +", title: " + info.getTitle() + ", compare: " + compare);
						if (compare > 0)
							break;
						
						ExchangeMarket.sendMessage(sender, F("versionChanges", info.getTitle()));


						

						
						changes = info.getDescription();

						for (int l = 0; l < changes.length; l++) {
							ExchangeMarket.sendMessage(sender, (l + 1) + ChatColor.GRAY.toString() + ": " + ChatColor.WHITE.toString() + changes[l]);

						}

					}

				}
				return;

			}


		}
	}

}
