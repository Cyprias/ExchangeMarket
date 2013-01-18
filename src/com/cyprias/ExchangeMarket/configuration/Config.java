package com.cyprias.ExchangeMarket.configuration;

import com.cyprias.ExchangeMarket.Plugin;

public class Config {
	private static final Plugin plugin = Plugin.getInstance();


	public static long getLong(String property) {
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
		return plugin.getConfig().getString(property).replaceAll("(?i)&([a-k0-9])", "\u00A7$1");
	}
	
	static public void migrateConfig(){
		//Migrate options from pre v1.1.0;
		migrateBoolean("logTransactionsToDB", "properties.log-transactions-to-db");
		migrateBoolean("blockUsageInCreativeMode", "properties.block-usage-in-creative");
		migrateBoolean("checkNewVersionOnStartup", "properties.check-new-version");
		migrateBoolean("allowDamangedGear", "properties.allow-damaged-gear");
		
		migrateDouble("minOrderPrice", "properties.min-order-price");
		
		migrateInt("priceRounding", "properties.price-decmial-places");
		migrateInt("transactionsPerPage", "properties.rows-per-page");
	}
	
	public static void migrateBoolean(String oldPath, String newPath){
		if (plugin.getConfig().contains(oldPath)){
			plugin.getConfig().set(newPath, plugin.getConfig().getBoolean(oldPath));
			plugin.getConfig().set(oldPath, null);
		}
	}
	
	public static void migrateDouble(String oldPath, String newPath){
		if (plugin.getConfig().contains(oldPath)){
			plugin.getConfig().set(newPath, plugin.getConfig().getDouble(oldPath));
			plugin.getConfig().set(oldPath, null);
		}
	}
	
	public static void migrateInt(String oldPath, String newPath){
		if (plugin.getConfig().contains(oldPath)){
			plugin.getConfig().set(newPath, plugin.getConfig().getInt(oldPath));
			plugin.getConfig().set(oldPath, null);
		}
	}
	
}

