package com.cyprias.exchangemarket;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ExchangeMarket extends JavaPlugin {
	public static String chatPrefix = "§f[§6EM§f] ";
	public Config config;
	public Database database;
	public Commands commands;
	public ItemDb itemdb;
	public Events events;

	public VersionChecker versionChecker;
	
	public YML yml;
	public Localization localization;

	public String pluginName;
	public static Economy econ = null;
	public Logger log = Logger.getLogger("Minecraft"); // Minecraft log and
														// console
	private String stPluginEnabled = "§f%s §7v§f%s §7is enabled.";

	public void onEnable() {
		this.config = new Config(this);
		this.database = new Database(this);
		if (this.isEnabled() == false)
			return;

		this.commands = new Commands(this);
		this.itemdb = new ItemDb(this);
		this.versionChecker = new VersionChecker(this, "http://dev.bukkit.org/server-mods/exchangemarket/files.rss");
		
		this.yml = new YML(this);
		this.localization = new Localization(this);

		this.events = new Events(this);
		getServer().getPluginManager().registerEvents(this.events, this);

		getCommand("em").setExecutor(this.commands);

		pluginName = getDescription().getName();

		info(String.format(this.stPluginEnabled, pluginName, getDescription().getVersion()));

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
		}
	}
	
	public boolean hasPermission(CommandSender sender, String node) {
		if (!(sender instanceof Player)) {
			return true;
		}
		Player player = (Player) sender;
		// if (player.isOp()) {
		// return true;
		// }

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

	public void notifyBuyerOfExchange(String buyerName, int itemID, int itemDur, int amount, double price, String trader, Boolean dryrun) {
		Player player = findPlayerByName(buyerName);

		if (player != null) {
			String itemName = itemdb.getItemName(itemID, itemDur);
			String preview = "";
			if (dryrun == true)
				preview = L("preview");

			// sendMessage(player, "Your " +
			// itemName+"x"+amount+" just sold for $" +(price*amount) +
			// " ($"+price+"e)");
			sendMessage(player, F("youBought", itemName, amount, Round(price * amount, Config.priceRounding), Round(price, Config.priceRounding), trader));

		}
	}

	public void notifySellerOfExchange(String buyerName, int itemID, int itemDur, int amount, double price, String trader, Boolean dryrun) {
		Player player = findPlayerByName(buyerName);
		if (player != null) {
			String itemName = itemdb.getItemName(itemID, itemDur);

			String preview = "";
			if (dryrun == true)
				preview = L("preview");

			// sendMessage(player, "Your " +
			// itemName+"x"+amount+" just sold for $" +(price*amount) +
			// " ($"+price+"e)");
			sendMessage(player,
				preview + F("youSold", itemName, amount, Round(price * amount, Config.priceRounding), Round(price, Config.priceRounding), trader));

		}
	}

	// public void notifySellerOfExchange(String buyerName, int itemID, int
	// itemDur, int amount, double price, String trader){
	// notifySellerOfExchange(buyerName, itemID, itemDur, amount, price, trader,
	// false);
	// }

	private String F(String string, Object... args) {
		return Localization.F(string, args);
	}

	private String L(String string) {
		return Localization.L(string);
	}

	public boolean payPlayer(String pName, double amount) {
		pName = pName.toLowerCase();
		if (setupEconomy()) {
			// return econ.getBalance(pName);
			if (!econ.hasAccount(pName))
				econ.createPlayerAccount(pName);
			double balance = econ.getBalance(pName.toLowerCase());
			econ.depositPlayer(pName, amount);
			
			if (Config.logBalanceChangesToConsole == true)
				info("§aCrediting §f" + pName + "'s account. " + Round(balance, 2) + "+§a" + Round(amount, 2) + "§f=" + Round(econ.getBalance(pName), 2));

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

			if (Config.logBalanceChangesToConsole == true)
				info("§cDebting §f" + pName + "'s account. " + Round(balance, 2) + "-§c" + Round(amount, 2) + "§f="
					+ Round(econ.getBalance(pName.toLowerCase()), 2));

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

	
	public void announceNewOrder(int type, CommandSender sender, int itemID, int itemDur, String itemEnchants, int amount, double price) {

		Player player = (Player) sender;

		String itemName = itemdb.getItemName(itemID, itemDur);

		String sType = L("sell").toLowerCase();

		if (type == 2)
			sType = L("buy").toLowerCase();

		// info("announceNewOrder: " + type + ", " + sType);

		String msg = F("newOrder", player.getDisplayName(), sType, itemName, amount, Round(price * amount, Config.priceRounding),
			Round(price, Config.priceRounding));

		if (type == 1) {
			permMessage("exchangemarket.announceneworder.sell", msg, sender.getName());
		} else if (type == 2) {
			permMessage("exchangemarket.announceneworder.buy", msg, sender.getName());
		}

	}

	public void permMessage(String permissionNode, String message, String excludeName) {
		for (Player p : getServer().getOnlinePlayers()) {
			if (hasPermission(p, permissionNode) && !p.getName().equalsIgnoreCase(excludeName))
				// if (hasPermission(p, permissionNode))
				p.sendMessage(chatPrefix + message);
		}
	}

	public void permMessage(String permissionNode, String message) {
		permMessage(permissionNode, message, "");
	}
}
