package com.cyprias.ExchangeMarket;

import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;


public class Econ {

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
