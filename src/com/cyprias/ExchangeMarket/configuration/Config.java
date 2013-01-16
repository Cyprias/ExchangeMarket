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
	
}
