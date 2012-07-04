package com.cyprias.exchangemarket;

import java.util.HashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class Events implements Listener {
	private ExchangeMarket plugin;
	public static  HashMap<String, String> aliases = new HashMap<String, String>();
	
	public Events(ExchangeMarket plugin) {
		this.plugin = plugin;
		
		FileConfiguration cfgAliases = plugin.yml.getYMLConfig("aliases.yml"); //
		
		String value;
		ConfigurationSection info;
		for (String aliase : cfgAliases.getKeys(false)) {
			aliases.put(aliase, cfgAliases.getString(aliase));
		}
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
}
