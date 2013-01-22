package com.cyprias.ExchangeMarket.listeners;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.database.Parcel;

public class PlayerListener implements Listener {

	static public void unregisterEvents(JavaPlugin instance) {
		PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
		PlayerJoinEvent.getHandlerList().unregister(instance);
		PluginEnableEvent.getHandlerList().unregister(instance);
	}


	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoinEvent(PlayerJoinEvent event) throws SQLException, IOException, InvalidConfigurationException  {
		List<Parcel> packages = Plugin.database.getPackages(event.getPlayer());
		
		if (packages.size() <= 0)
			return;

		ChatUtils.notify(event.getPlayer(), String.format("§7You have §f%s §7packages to collect.", packages.size()));
		
		ItemStack stock ;
		for (Parcel parcel : packages){
			stock = Plugin.getItemStack(parcel.getItemId(), parcel.getItemDur(), parcel.getItemEnchant());
			ChatUtils.sendSpam(event.getPlayer(), String.format("§f%s§7x§f%s", Plugin.getItemName(stock), parcel.getAmount()));
		}

	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)  {
		String msg = event.getMessage();
		String command = msg.split(" ")[0].replace("/", "");

		if (Plugin.aliases.containsKey(command.toLowerCase())) {
			event.setMessage(msg.replaceFirst("/" + command, "/" + Plugin.aliases.get(command.toLowerCase())));
			return;
		}
	}

}
