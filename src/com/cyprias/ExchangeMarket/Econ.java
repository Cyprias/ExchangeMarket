package com.cyprias.ExchangeMarket;

import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;


public class Econ {
	/*
	public static Economy econ = null;
	
	public static boolean setupEconomy() {
		if (Plugin.getInstance().getServer().getPluginManager().getPlugin("Vault") == null) 
			return false;
		
		RegisteredServiceProvider<Economy> rsp = Plugin.getInstance().getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) 
			return false;
		
		
		
		econ = rsp.getProvider();
		return econ != null;
	}

	public static double getBalance(String pName) {
		return (econ != null ||  setupEconomy()) ? econ.getBalance(pName.toLowerCase()): 0.0;
	}

	public static boolean depositPlayer(String pName, double amount) {
		pName = pName.toLowerCase();
		if (econ != null ||  setupEconomy()) {
			// return econ.getBalance(pName);
			if (!econ.hasAccount(pName))
				econ.createPlayerAccount(pName);
			double balance = econ.getBalance(pName.toLowerCase());
			econ.depositPlayer(pName, amount);

			if (Config.getBoolean("properties.log-balance-changes-to-console"))
				Logger.info("Crediting " + pName + "'s account. " + Plugin.Round(balance, 2) + "+§a" + Plugin.Round(amount, 2) + "§f=" + Plugin.Round(econ.getBalance(pName), 2));

			return true;
		}
		return false;
	}
	
	public static boolean withdrawPlayer(String pName, double amount) {
		pName = pName.toLowerCase();
		if (econ != null ||  setupEconomy()) {

			if (!econ.hasAccount(pName))
				econ.createPlayerAccount(pName);

			double balance = econ.getBalance(pName);

			econ.withdrawPlayer(pName, amount);
			
			if (Config.getBoolean("properties.log-balance-changes-to-console"))
				Logger.info("§cDebting §f" + pName + "'s account. " + Plugin.Round(balance, 2) + "-§c" + Plugin.Round(amount, 2) + "§f="
					+ Plugin.Round(econ.getBalance(pName.toLowerCase()), 2));

			return true;
		}
		return false;
	}*/
	
	
    public static Economy econ = null;
    static boolean setupEconomy(){
    if (Plugin.getInstance().getServer().getPluginManager().getPlugin("Vault") == null) {
        return false;
    }
    RegisteredServiceProvider<Economy> rsp = Plugin.getInstance().getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
        return false;
    }
    econ = rsp.getProvider();
    return econ != null;
}

	public static double getBalance(String pName) {
		return (setupEconomy() == true) ? econ.getBalance(pName): 0.0;
		//return (econ != null ||  setupEconomy()) ? econ.getBalance(pName.toLowerCase()): 0.0;
	}
    
	public static EconomyResponse depositPlayer(String playerName, double amount){
		return (setupEconomy() == true) ? econ.depositPlayer(playerName, amount): null;
	}
	
	public static EconomyResponse withdrawPlayer(String playerName, double amount){
		return (setupEconomy() == true) ? econ.withdrawPlayer(playerName, amount): null;
	}
	
	public static String format(double amount){
		return econ.format(amount);
	}
}
