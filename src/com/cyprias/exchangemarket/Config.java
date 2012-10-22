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
	
	public static String sqlUsername, sqlPassword, sqlURL, sqlPrefix, sqlDatabase, sqlHost, sqlPort;
	//public static int sqlPort;
	public static String locale;
	public static Boolean blockUsageInCreativeMode,  logBalanceChangesToConsole, announceNewOrders, logTransactionsToDB, convertCreatePriceToPerItem, cancelSelfSalesWhenBuying, autoPricePerUnit, clearRequestAfterConfirm, confirmAllOrders, autoPriceConfirm;
	public static int priceRounding, transactionsPerPage;
	public static Double autoBuyPrice, autoSellPrice;
	
	
	private void loadConfigOpts(){
		sqlUsername = config.getString("mysql.username");
		sqlPassword = config.getString("mysql.password");
		sqlPrefix = config.getString("mysql.prefix"); 

		sqlDatabase = config.getString("mysql.database");
		
		sqlHost = config.getString("mysql.hostname");
		sqlPort = config.getString("mysql.port");
		//sqlFile = config.getString("mysql.dbfile");
		
		//if (sqlSystem.equals("sqlite")){
		//	sqlURL = "jdbc:sqlite:"+sqlFile;
		//}else if (sqlSystem.equals("mysql")){
			sqlURL = "jdbc:mysql://" + sqlHost + ":" + sqlPort + "/" + sqlDatabase;
		//}
			
		announceNewOrders = config.getBoolean("announceNewOrders");
		blockUsageInCreativeMode = config.getBoolean("blockUsageInCreativeMode");
		
		clearRequestAfterConfirm = config.getBoolean("clearRequestAfterConfirm");
		autoPriceConfirm = config.getBoolean("autoPriceConfirm");
		confirmAllOrders = config.getBoolean("confirmAllOrders");
		logTransactionsToDB = config.getBoolean("logTransactionsToDB");
		
		logBalanceChangesToConsole = config.getBoolean("logBalanceChangesToConsole");
		
		locale = config.getString("locale");
		
		convertCreatePriceToPerItem = config.getBoolean("convertCreatePriceToPerItem");
		cancelSelfSalesWhenBuying = config.getBoolean("cancelSelfSalesWhenBuying");
		
		priceRounding = config.getInt("priceRounding");
		transactionsPerPage = config.getInt("transactionsPerPage");
		
		autoBuyPrice = config.getDouble("autoBuyPrice");
		autoSellPrice = config.getDouble("autoSellPrice");
		autoPricePerUnit = config.getBoolean("autoPricePerUnit");
		
		
	}
}
