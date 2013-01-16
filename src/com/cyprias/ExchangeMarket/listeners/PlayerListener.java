package com.cyprias.ExchangeMarket.listeners;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.server.PluginEnableEvent;

import com.cyprias.ExchangeMarket.Econ;

public class PlayerListener implements Listener {

	static public void unregisterEvents(JavaPlugin instance) {
		PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
		PlayerJoinEvent.getHandlerList().unregister(instance);
		PluginEnableEvent.getHandlerList().unregister(instance);
	}


	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoinEvent(PlayerJoinEvent event)  {
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)  {
		
	}

}
