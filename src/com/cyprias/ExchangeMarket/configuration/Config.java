package com.cyprias.ExchangeMarket.configuration;

import java.io.IOException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Plugin;

public class Config {
	private static final Plugin plugin = Plugin.getInstance();
	
	public static long getLong(String property){
		return plugin.getConfig().getLong(property);
	}

	public static int getInt(String property) {
			return plugin.getConfig().getInt(property);
	}

	public static double getDouble(String property) {
		return plugin.getConfig().getDouble(property);
	}

	public static boolean getBoolean(String property) {
			return plugin.getConfig().getBoolean(property);
	}

	public static String getString(String property) {
			return plugin.getConfig().getString(property);
	}

	public static String getColouredString(String property) {
		return getString(property).replaceAll("(?i)&([a-k0-9])", "\u00A7$1");
	}
	
	public static void checkForMissingProperties() throws IOException, InvalidConfigurationException{
		YML diskConfig = new YML(plugin.getDataFolder(), "config.yml");
		YML defaultConfig = new YML(plugin.getResource("config.yml"));

		for (String property : defaultConfig.getKeys(true)){
			if (!diskConfig.contains(property))
				Logger.warning(property + " is missing from your config.yml, using default.");
		}
		
	}
	
	static public boolean migrateConfig(){
		//Migrate options from pre v1.1.0;
		int change = 0;

		change += migrateBoolean("logTransactionsToDB", "properties.log-transactions-to-db");
		change += migrateBoolean("blockUsageInCreativeMode", "properties.block-usage-in-creative");
		change += migrateBoolean("checkNewVersionOnStartup", "properties.check-new-version");
		change += migrateBoolean("allowDamangedGear", "properties.allow-damaged-gear");
		
		change += migrateDouble("minOrderPrice", "properties.min-order-price");
		
		change += migrateInt("priceRounding", "properties.price-decmial-places");
		change += migrateInt("transactionsPerPage", "properties.rows-per-page");
		
		return (change > 0) ? true : false;
	}
	
	public static int migrateBoolean(String oldPath, String newPath){
		if (plugin.getConfig().contains(oldPath)){
			plugin.getConfig().set(newPath, plugin.getConfig().getBoolean(oldPath));
			plugin.getConfig().set(oldPath, null);
			return 1;
		}
		return 0;
	}
	
	public static int migrateDouble(String oldPath, String newPath){
		if (plugin.getConfig().contains(oldPath)){
			plugin.getConfig().set(newPath, plugin.getConfig().getDouble(oldPath));
			plugin.getConfig().set(oldPath, null);
			return 1;
		}
		return 0;
	}
	
	public static int migrateInt(String oldPath, String newPath){
		if (plugin.getConfig().contains(oldPath)){
			plugin.getConfig().set(newPath, plugin.getConfig().getInt(oldPath));
			plugin.getConfig().set(oldPath, null);
			return 1;
		}
		return 0;
	}
	
}

