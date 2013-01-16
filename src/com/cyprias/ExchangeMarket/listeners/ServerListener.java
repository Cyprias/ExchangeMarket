package com.cyprias.ExchangeMarket.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;

public class ServerListener implements Listener {
	static public void unregisterEvents(JavaPlugin instance) {
		PluginEnableEvent.getHandlerList().unregister(instance);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPluginEnableEvent(PluginEnableEvent event)  {
		/*
		if (event.getPlugin().getName().equalsIgnoreCase("Vault")){
			Logger.info("Reloading economy stuff.");
			Econ.setupEconomy();
		}*/
	}
}
