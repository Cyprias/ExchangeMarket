package com.cyprias.exchangemarket;

import org.bukkit.configuration.Configuration;


public class Config {
	private ExchangeMarket plugin;
	private static Configuration config;
	
	public Config(ExchangeMarket plugin) {
		this.plugin = plugin;
		config = plugin.getConfig().getRoot();
		config.options().copyDefaults(true);
		plugin.saveConfig();
		
		loadConfigOpts();
	}
	
	public void reloadOurConfig(){
		plugin.reloadConfig();
		config = plugin.getConfig().getRoot();
		loadConfigOpts();
	}
	
	public static String sqlUsername, sqlPassword, sqlURL, sqlPrefix;
	public static String locale;
	public static Boolean convertCreatePriceToPerItem, cancelSelfSalesWhenBuying, autoPricePerUnit;
	public static int priceRounding;
	public static Double autoBuyPrice, autoSellPrice;
	
	private void loadConfigOpts(){
		sqlUsername = config.getString("mysql.username");
		sqlPassword = config.getString("mysql.password");
		sqlURL = "jdbc:mysql://" + config.getString("mysql.hostname") + ":" + config.getInt("mysql.port") + "/" + config.getString("mysql.database");
		sqlPrefix = config.getString("mysql.prefix"); 
			
		locale = config.getString("locale");
		
		convertCreatePriceToPerItem = config.getBoolean("convertCreatePriceToPerItem");
		cancelSelfSalesWhenBuying = config.getBoolean("cancelSelfSalesWhenBuying");
		
		priceRounding = config.getInt("priceRounding");
		
		autoBuyPrice = config.getDouble("autoBuyPrice");
		autoSellPrice = config.getDouble("autoSellPrice");
		autoPricePerUnit = config.getBoolean("autoPricePerUnit");
		
	}
}
