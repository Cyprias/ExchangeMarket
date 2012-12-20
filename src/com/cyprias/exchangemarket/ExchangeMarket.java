package com.cyprias.exchangemarket;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.MaterialUtil;

public class ExchangeMarket extends JavaPlugin {
	public static String chatPrefix = "§f[§6EM§f] ";
	public Config config;
	public Commands commands;
	public Events events;

	public static String pluginName;
	public static Economy econ = null;
	public Logger log = Logger.getLogger("Minecraft"); // Minecraft log and
														// console
	private String stPluginEnabled = "§f%s §7v§f%s §7is enabled.";
	private static Server server;
	private static File dataFolder;
	static JavaPlugin plugin;
	
	public void onEnable() {
		this.plugin = this;
		this.dataFolder = getDataFolder();
		this.server = getServer();
		
		this.config = new Config(this);
		
		try {
			Database.init();
		} catch (SQLException e1) {
			e1.printStackTrace();
			this.getPluginLoader().disablePlugin(this);
			return;
		}
		
		this.commands = new Commands(this);
		
		new ItemDb(this);

		if (Config.checkNewVersionOnStartup == true)
			VersionChecker.retreiveVersionInfo(this, "http://dev.bukkit.org/server-mods/exchangemarket/files.rss");

		this.events = new Events(this);
		getServer().getPluginManager().registerEvents(this.events, this);

		getCommand("em").setExecutor(this.commands);

		pluginName = getDescription().getName();

		loadLocales();
		

		
		log.info(String.format("%s v%s is enabled.", pluginName, this.getDescription().getVersion()));

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
		}
	}
	public static HashMap<String, String> locales = new HashMap<String, String>();
	static void loadLocales(){
		String localeDir =dataFolder.separator + "locales" +dataFolder.separator;

		//Copy existing locales into plugin dir, so admin knows what's available. 
		new YML(plugin.getResource("enUS.yml"), dataFolder, localeDir + "enUS.yml", true);
		
		
		
		//Copy any new locale strings to file on disk.
		YML resLocale = new YML(plugin.getResource("enUS.yml"));
		YML locale = new YML(plugin.getResource(Config.locale), dataFolder, localeDir+ Config.locale);
		for (String key : resLocale.getKeys(false)) {
			if (locale.get(key) == null){
				info("Adding new locale " + key + " = " + resLocale.getString(key).replaceAll("(?i)&([a-k0-9])", "\u00A7$1"));
				locale.set(key, resLocale.getString(key));
				locale.save();
			}
		}
		
		
		//Load locales into our hashmap. 
		locales.clear();
		for (String key : locale.getKeys(false)) {
			locales.put(key, locale.getString(key).replaceAll("(?i)&([a-k0-9])", "\u00A7$1"));// §
		}
	}
	
	
	public static boolean hasPermission(CommandSender sender, String node) {
		if (!(sender instanceof Player)) {
			return true;
		}
		Player player = (Player) sender;
		if (Config.grantOpsAllPermissions == true && player.isOp()) {
			return true;
		}

		if (player.isPermissionSet(node)) // in case admin purposely set the
											// node to false.
			return player.hasPermission(node);

		if (player.isPermissionSet(pluginName.toLowerCase() + ".*"))
			return player.hasPermission(pluginName.toLowerCase() + ".*");

		String[] temp = node.split("\\.");
		String wildNode = temp[0];
		for (int i = 1; i < (temp.length); i++) {
			wildNode = wildNode + "." + temp[i];

			if (player.isPermissionSet(wildNode + ".*"))
				// plugin.info("wildNode1 " + wildNode+".*");
				return player.hasPermission(wildNode + ".*");
		}

		return player.hasPermission(node);
	}

	public static void info(String msg) {
		server.getConsoleSender().sendMessage(chatPrefix + msg);
	}

	public static void sendMessage(CommandSender sender, String message, Boolean showConsole, Boolean sendPrefix) {
		if (sender instanceof Player && showConsole == true) {
			info("§e" + sender.getName() + "->§f" + message);
		}
		if (sendPrefix == true) {
			sender.sendMessage(chatPrefix + message);
		} else {
			sender.sendMessage(message);
		}
	}

	public static void sendMessage(CommandSender sender, String message, Boolean showConsole) {
		sendMessage(sender, message, showConsole, true);
	}

	public static void sendMessage(CommandSender sender, String message) {
		sendMessage(sender, message, true);
	}

	public static boolean setupEconomy() {
		if (server.getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = server.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public static double getBalance(String pName) {
		if (setupEconomy()) {
			return econ.getBalance(pName.toLowerCase());
		}
		return 0;
	}

	public static Player findPlayerByName(String name) {

		for (Player p : server.getOnlinePlayers()) {
			if (p.getName().equalsIgnoreCase(name))
				return p;

			if (p.getDisplayName().equalsIgnoreCase(name))
				return p;
		}

		return null;
	}

	public static void notifyBuyerOfExchange(String buyerName, int itemID, int itemDur, int amount, double price, String trader, Boolean dryrun) {
		Player player = findPlayerByName(buyerName);

		if (player != null) {
			String itemName = ItemDb.getItemName(itemID, itemDur);
			String preview = "";
			if (dryrun == true)
				preview = L("preview");

			// sendMessage(player, "Your " +
			// itemName+"x"+amount+" just sold for $" +(price*amount) +
			// " ($"+price+"e)");
			sendMessage(player, F("youBought", itemName, amount, Round(price * amount, Config.priceRounding), Round(price, Config.priceRounding), trader));

		}
	}

	public static void notifySellerOfExchange(String buyerName, int itemID, int itemDur, String itemEnchants, int amount, double price, String trader, Boolean dryrun) {
		Player player = findPlayerByName(buyerName);
		if (player != null) {
			String itemName = ItemDb.getItemName(itemID, itemDur);
			if (itemEnchants != null)
				itemName += "-" + itemEnchants;
			
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

	static public String L(String key) {
		if (locales.containsKey(key))
			return locales.get(key).toString();
		
		return "MISSING LOCALE: " + ChatColor.RED + key;
	}

	static public String F(String key, Object... args) {
		String value = L(key);
		try {
			if (value != null || args != null)
				value = String.format(value, args); // arg.toString()
		} catch (Exception e) {e.printStackTrace();
		}
		return value;
	}

	public static boolean payPlayer(String pName, double amount) {
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

	public static boolean debtPlayer(String pName, double amount) {
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

	public static String Round(double Rval, int Rpl) {
		String format = "#.";
		for (int i=1; i<=Rpl; i++)
			format += "#";
		
		DecimalFormat df = new DecimalFormat(format);
		return df.format(Rval);
	}

	public static void announceNewOrder(int type, CommandSender sender, int itemID, int itemDur, String itemEnchants, int amount, double price) {

		Player player = (Player) sender;

		String itemName = ItemDb.getItemName(itemID, itemDur);
		if (itemEnchants != null)
			itemName += "-" + itemEnchants;
		
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

	public static void permMessage(String permissionNode, String message, String excludeName) {
		for (Player p : server.getOnlinePlayers()) {
			if (hasPermission(p, permissionNode) && !p.getName().equalsIgnoreCase(excludeName))
				// if (hasPermission(p, permissionNode))
				p.sendMessage(chatPrefix + message);
		}
	}

	public void permMessage(String permissionNode, String message) {
		permMessage(permissionNode, message, "");
	}

	public boolean hasCommandPermission(CommandSender player, String permission) {
		if (hasPermission(player, permission)) {
			return true;
		}
		// sendMessage(player, F("stNoPermission", permission));
		sendMessage(player, F("noPermission", permission));

		return false;
	}

	public boolean isGear(Material mat) {
		try{
			if (mat.toString().contains("SWORD"))
				return true;
			if (mat.toString().contains("PICKAXE"))
				return true;
			if (mat.toString().contains("SPADE"))
				return true;
			if (mat.toString().contains("AXE"))
				return true;
			if (mat.toString().contains("HOE"))
				return true;
			if (mat.toString().contains("HELMET"))
				return true;
			if (mat.toString().contains("CHESTPLATE"))
				return true;
			if (mat.toString().contains("LEGGINGS"))
				return true;
			if (mat.toString().contains("BOOTS"))
				return true;
		} catch (Exception e) {e.printStackTrace();}

		return false;
	}

	public static int giveItemToPlayer(Player player, int itemID, short itemDur, String enchants, int totalAmount) {
		ItemStack itemStack = new ItemStack(itemID, 1);
		itemStack.setDurability(itemDur);

		itemStack.setAmount(totalAmount);


		if (enchants != null && !enchants.equalsIgnoreCase("")) {
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));
		}

		int amount;
		int gave = 0;
		while (totalAmount > 0){
			if (totalAmount > itemStack.getMaxStackSize()){
				amount = itemStack.getMaxStackSize();
			}else{
				amount = totalAmount;
			}
			itemStack.setAmount(amount);
			
			if (InventoryUtil.fits(itemStack, player.getInventory())) {
			
				InventoryUtil.add(itemStack, player.getInventory());
				
				totalAmount-=amount;
				gave += amount;
			}else{
				break;
			}
		}

		return gave;

	}
	
}
