package com.cyprias.exchangemarket;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


public class ExchangeMarket extends JavaPlugin {
	public static String chatPrefix = "§f[§6EM§f] ";
	public Config config;
	public Database database;
	public Commands commands;
	public ItemDb itemdb;
	public Events events;
	
	public YML yml;
	public Localization localization;
	
	public String pluginName;
	public static Economy econ = null;
	
	public void onEnable() {
		this.config = new Config(this);
		this.database = new Database(this);
		this.commands = new Commands(this);
		this.itemdb = new ItemDb(this);
		
		this.yml = new YML(this);
		this.localization = new Localization(this);
		
		this.events = new Events(this);
		getServer().getPluginManager().registerEvents(this.events, this);
		
		getCommand("em").setExecutor(this.commands);
		
		pluginName = getDescription().getName();
	}
	public boolean hasPermission(CommandSender sender, String node) {
		if (!(sender instanceof Player)) {
			return true;
		}
		Player player = (Player) sender;
		if (player.isOp()) {
			return true;
		}

		if (player.isPermissionSet(node))
			return player.hasPermission(node);

		String[] temp = node.split("\\.");
		String wildNode = temp[0];
		for (int i = 1; i < (temp.length); i++) {
			wildNode = wildNode + "." + temp[i];

			if (player.isPermissionSet(wildNode + ".*"))
				// plugin.info("wildNode1 " + wildNode+".*");
				return player.hasPermission(wildNode + ".*");

		}
		if (player.isPermissionSet(wildNode))
			return player.hasPermission(wildNode);

		if (player.isPermissionSet(wildNode))
			return player.hasPermission(wildNode);

		return player.hasPermission(pluginName.toLowerCase() + ".*");
	}
	public void info(String msg) {
		getServer().getConsoleSender().sendMessage(chatPrefix + msg);
	}
	
	public void sendMessage(CommandSender sender, String message, Boolean showConsole) {
		if (sender instanceof Player && showConsole == true) {
			info("§e" + sender.getName() + "->§f" + message);
		}
		sender.sendMessage(chatPrefix + message);
	}

	public void sendMessage(CommandSender sender, String message) {
		sendMessage(sender, message, true);
	}
	
	public boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public double getBalance(String pName) {
		if (setupEconomy()) {
			return econ.getBalance(pName.toLowerCase());
		}
		return 0;
	}
	
	public Player findPlayerByName(String name) {

		for (Player p : getServer().getOnlinePlayers()) {
			if (p.getName().equalsIgnoreCase(name))
				return p;

			if (p.getDisplayName().equalsIgnoreCase(name))
				return p;
		}

		return null;
	}
	
	public void notifyBuyerOfExchange(String buyerName, int itemID, int itemDur, int amount, double price, String trader){
		Player player = findPlayerByName(buyerName);
		String itemName = itemdb.getItemName(itemID, itemDur);
		if (player != null){
			//sendMessage(player, "Your " + itemName+"x"+amount+" just sold for $" +(price*amount) + " ($"+price+"e)");
			sendMessage(player, "You bought " + itemName+"x"+amount+" for $" +(price*amount) + " ($"+price+"e) from " + trader + ".");
			
		}
	}
	
	public void notifySellerOfExchange(String buyerName, int itemID, int itemDur, int amount, double price, String trader){
		Player player = findPlayerByName(buyerName);
		String itemName = itemdb.getItemName(itemID, itemDur);
		if (player != null){
			//sendMessage(player, "Your " + itemName+"x"+amount+" just sold for $" +(price*amount) + " ($"+price+"e)");
			sendMessage(player, "You sold " + itemName+"x"+amount+" for $" +(price*amount) + " ($"+price+"e) to " + trader + ".");
			
		}
	}
	
	public boolean payPlayer(String pName, double amount) {
		pName = pName.toLowerCase();
		if (setupEconomy()) {
			// return econ.getBalance(pName);
			if (!econ.hasAccount(pName))
				econ.createPlayerAccount(pName);
			double balance = econ.getBalance(pName);
			econ.depositPlayer(pName.toLowerCase(), amount);
			info("§aCrediting §f" + pName + "'s account. " + Round(balance,2) + "+§a" + Round(amount,2) + "§f=" + Round(econ.getBalance(pName),2));
			
			
			return true;
		}
		return false;
	}

	public boolean debtPlayer(String pName, double amount) {
		pName = pName.toLowerCase();
		if (setupEconomy()) {

			if (!econ.hasAccount(pName))
				econ.createPlayerAccount(pName);
			
			double balance = econ.getBalance(pName);
			
			econ.withdrawPlayer(pName, amount);

			info("§cDebting §f" + pName + "'s account. " + Round(balance,2) + "-§c" + Round(amount,2) + "§f=" + Round(econ.getBalance(pName),2));
			
			return true;
		}
		return false;
	}
	public static double Round(double Rval, int Rpl) {
		double p = (double) Math.pow(10, Rpl);
		Rval = Rval * p;
		double tmp = Math.round(Rval);
		return (double) tmp / p;
	}
	

}
