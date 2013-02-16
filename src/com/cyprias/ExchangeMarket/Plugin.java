package com.cyprias.ExchangeMarket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import org.xml.sax.SAXException;

import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.command.BuyCommand;
import com.cyprias.ExchangeMarket.command.BuyListCommand;
import com.cyprias.ExchangeMarket.command.BuyOrderCommand;
import com.cyprias.ExchangeMarket.command.CancelCommand;
import com.cyprias.ExchangeMarket.command.CollectCommand;
import com.cyprias.ExchangeMarket.command.CommandManager;
import com.cyprias.ExchangeMarket.command.ConfirmCommand;
import com.cyprias.ExchangeMarket.command.InfBuyCommand;
import com.cyprias.ExchangeMarket.command.InfSellCommand;
import com.cyprias.ExchangeMarket.command.OrderInfoCommand;
import com.cyprias.ExchangeMarket.command.ItemInfoCommand;
import com.cyprias.ExchangeMarket.command.ListCommand;
import com.cyprias.ExchangeMarket.command.OrdersCommand;
import com.cyprias.ExchangeMarket.command.PriceCommand;
import com.cyprias.ExchangeMarket.command.ReloadCommand;
import com.cyprias.ExchangeMarket.command.RemoveCommand;
import com.cyprias.ExchangeMarket.command.ReturnCommand;
import com.cyprias.ExchangeMarket.command.SearchCommand;
import com.cyprias.ExchangeMarket.command.SellCommand;
import com.cyprias.ExchangeMarket.command.SellListCommand;
import com.cyprias.ExchangeMarket.command.SellOrderCommand;
import com.cyprias.ExchangeMarket.command.SetPriceCommand;
import com.cyprias.ExchangeMarket.command.TestCommand;
import com.cyprias.ExchangeMarket.command.TransactionsCommand;
import com.cyprias.ExchangeMarket.command.VersionCommand;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.configuration.YML;
import com.cyprias.ExchangeMarket.database.Database;
import com.cyprias.ExchangeMarket.database.MySQL;
import com.cyprias.ExchangeMarket.database.Order;
import com.cyprias.ExchangeMarket.database.SQLite;
import com.cyprias.ExchangeMarket.listeners.PlayerListener;
import com.cyprias.ExchangeMarket.listeners.SignListener;

public class Plugin extends JavaPlugin {
	private static Plugin instance = null;

	// public void onLoad() {}

	public static Database database;
	public static HashMap<String, String> aliases = new HashMap<String, String>();

	public void onEnable() {
		instance = this;

		if (!(new File(getDataFolder(), "config.yml").exists())) {
			Logger.info("Copying config.yml to disk.");
			try {
				YML.toFile(getResource("config.yml"), getDataFolder(), "config.yml");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			// getConfig().options().copyDefaults(true);
			// saveConfig();
		}
		if (Config.migrateConfig())
			saveConfig();

		try {
			Config.checkForMissingProperties();
		} catch (IOException e4) {
			e4.printStackTrace();
		} catch (InvalidConfigurationException e4) {
			e4.printStackTrace();
		}

		if (!Econ.setupEconomy()) {
			Logger.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		if (Config.getString("properties.db-type").equalsIgnoreCase("mysql")) {
			database = new MySQL();
		} else if (Config.getString("properties.db-type").equalsIgnoreCase("sqlite")) {
			database = new SQLite();
		} else {
			Logger.severe("No database selected (" + Config.getString("properties.db-type") + "), unloading plugin...");
			instance.getPluginLoader().disablePlugin(instance);
			return;
		}

		try {
			if (!database.init()) {
				Logger.severe("Failed to initilize database, unloading plugin...");
				instance.getPluginLoader().disablePlugin(instance);
				return;
			}
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		} catch (InvalidConfigurationException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		loadPermissions();

		CommandManager cm = new CommandManager().registerCommand("reload", new ReloadCommand());
		cm.registerCommand("version", new VersionCommand());
		cm.registerCommand("sellorder", new SellOrderCommand());
		cm.registerCommand("search", new SearchCommand());
		cm.registerCommand("list", new ListCommand());
		cm.registerCommand("buyorder", new BuyOrderCommand());
		cm.registerCommand("buy", new BuyCommand());
		cm.registerCommand("confirm", new ConfirmCommand());
		cm.registerCommand("sell", new SellCommand());
		cm.registerCommand("collect", new CollectCommand());
		cm.registerCommand("price", new PriceCommand());
		cm.registerCommand("orders", new OrdersCommand());
		cm.registerCommand("cancel", new CancelCommand());
		cm.registerCommand("return", new ReturnCommand());
		cm.registerCommand("remove", new RemoveCommand());
		cm.registerCommand("infsell", new InfSellCommand());
		cm.registerCommand("infbuy", new InfBuyCommand());
		cm.registerCommand("transactions", new TransactionsCommand());
		cm.registerCommand("orderinfo", new OrderInfoCommand());
		cm.registerCommand("iteminfo", new ItemInfoCommand());
		cm.registerCommand("test", new TestCommand());
		cm.registerCommand("buylist", new BuyListCommand());
		cm.registerCommand("selllist", new SellListCommand());
		cm.registerCommand("setprice", new SetPriceCommand());
		
		
		this.getCommand("em").setExecutor(cm);

		try {
			YML yml = new YML(getResource("aliases.yml"), getDataFolder(), "aliases.yml");
			for (String key : yml.getKeys(false)) {
				aliases.put(key, yml.getString(key));
			}
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		} catch (InvalidConfigurationException e2) {
			e2.printStackTrace();
		}

		try {
			loadItemIds();
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		registerListeners(new PlayerListener(), new SignListener());

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
		}

		/*
		 * if (!Econ.setupEconomy()) {
		 * Logger.warning("Unable to find economy plugin!"); }
		 */

		if (Config.getBoolean("properties.check-new-version"))
			checkVersion();

		Logger.info("enabled.");
	}

	private void loadPermissions() {
		PluginManager pm = Bukkit.getPluginManager();
		for (Perm permission : Perm.values()) {
			permission.loadPermission(pm);
		}
	}

	private void checkVersion() {
		getServer().getScheduler().runTaskAsynchronously(instance, new Runnable() {
			public void run() {
				try {
					VersionChecker version = new VersionChecker("http://dev.bukkit.org/server-mods/exchangemarket/files.rss");
					VersionChecker.versionInfo info = (version.versions.size() > 0) ? version.versions.get(0) : null;
					if (info != null) {
						String curVersion = getDescription().getVersion();
						if (VersionChecker.compareVersions(curVersion, info.getTitle()) < 0) {
							Logger.warning("We're running v" + curVersion + ", v" + info.getTitle() + " is available");
							Logger.warning(info.getLink());
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}

			}
		});
	}

	private void registerListeners(Listener... listeners) {
		PluginManager manager = getServer().getPluginManager();

		for (Listener listener : listeners) {
			manager.registerEvents(listener, this);
		}
	}

	public void onDisable() {

		PluginManager pm = Bukkit.getPluginManager();
		for (Perm permission : Perm.values()) {
			// permission.loadPermission(pm);
			permission.unloadPermission(pm);
		}

		CommandManager.unregisterCommands();
		this.getCommand("em").setExecutor(null);

		instance.getServer().getScheduler().cancelAllTasks();

		PlayerListener.unregisterEvents(instance);
		SignListener.unregisterEvents(instance);

		instance = null;
		Logger.info("disabled.");
	}

	public static void reload() {
		instance.reloadConfig();
	}

	public static void disable() {
		instance.getServer().getPluginManager().disablePlugin(instance);
	}

	static public boolean hasPermission(CommandSender sender, Perm permission) {
		if (sender != null) {
			if (sender instanceof ConsoleCommandSender)
				return true;

			if (sender.hasPermission(permission.getPermission())) {
				return true;
			} else {
				Perm parent = permission.getParent();
				return (parent != null) ? hasPermission(sender, parent) : false;
			}
		}
		return false;
	}

	public static boolean checkPermission(CommandSender sender, Perm permission) {
		if (!hasPermission(sender, permission)) {
			String mess = permission.getErrorMessage();
			if (mess == null)
				mess = Perm.DEFAULT_ERROR_MESSAGE;
			ChatUtils.error(sender, mess);
			return false;
		}
		return true;
	}

	public static final Plugin getInstance() {
		return instance;
	}

	public static double getUnixTime() {
		return (System.currentTimeMillis() / 1000D);
	}

	public static String getFinalArg(final String[] args, final int start) {
		final StringBuilder bldr = new StringBuilder();
		for (int i = start; i < args.length; i++) {
			if (i != start) {
				bldr.append(" ");
			}
			bldr.append(args[i]);
		}
		return bldr.toString();
	}

	public static boolean isInt(final String sInt) {
		try {
			Integer.parseInt(sInt);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static boolean isDouble(final String sDouble) {
		try {
			Double.parseDouble(sDouble);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static String Round(double Rval, int Rpl) {
		String format = "#";
		if (Rpl > 0)
			format += ".";
		for (int i = 1; i <= Rpl; i++)
			format += "#";

		return new DecimalFormat(format).format(Rval);
	}

	public static String Round(double Rval) {
		return Round(Rval, 0);
	}

	
	public static double dRound(double Rval, int Rpl) {
		double p = (double) Math.pow(10, Rpl);
		Rval = Rval * p;
		double tmp = Math.round(Rval);
		return (double) tmp / p;
	}

	public static ItemStack getItemStack(int itemID, short itemDur, int amount, String enchants) {
		ItemStack itemStack = new ItemStack(itemID, amount);
		itemStack.setDurability(itemDur);

		if (enchants != null && !enchants.equalsIgnoreCase(""))
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));

		return itemStack;
	}

	public static ItemStack getItemStack(int itemID, short itemDur, String enchants) {
		return getItemStack(itemID, itemDur, 1, enchants);
	}

	public static ItemStack getItemStack(int itemID, short itemDur) {
		return getItemStack(itemID, itemDur, 1, null);
	}

	public static ItemStack getItemStack(ItemStack stock, String enchants) {
		if (enchants != null && !enchants.equalsIgnoreCase(""))
			stock.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));
		return stock;
	}

	public static ItemStack getItemStack(String id) {
		int itemid = 0;
		String itemname = null;
		short metaData = 0;

		String[] split = id.trim().split("-");
		String enchant = null;
		if (split.length > 1) {
			id = split[0];
			enchant = split[1];
		}
		if (id.matches("^\\d+[:+',;.]\\d+$")) {
			itemid = Integer.parseInt(id.split("[:+',;.]")[0]);
			metaData = Short.parseShort(id.split("[:+',;.]")[1]);
		} else if (id.matches("^\\d+$")) {
			itemid = Integer.parseInt(id);
		} else if (id.matches("^[^:+',;.]+[:+',;.]\\d+$")) {
			itemname = id.split("[:+',;.]")[0].toLowerCase(Locale.ENGLISH);
			metaData = Short.parseShort(id.split("[:+',;.]")[1]);
		} else {
			itemname = id.toLowerCase(Locale.ENGLISH);
		}

		if (itemid > 0) {
			return getItemStack(itemid, metaData, enchant);
		}
		if (itemname != null) {
		//	Logger.info("getItemStack", itemname, enchant);
			if (nameToStack.containsKey(itemname)) 
				return getItemStack(nameToStack.get(itemname), enchant);
			
			ItemStack mat = MaterialUtil.getItem(id);
			if (mat != null){
				if (enchant != null)
					mat.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchant));
				
				return mat;
			}
		}

		return null;
	}

	// static HashMap<String, String> idToName = new HashMap<String, String>();
	static HashMap<String, ItemStack> nameToStack = new HashMap<String, ItemStack>();
	static HashMap<String, String> stockToName = new HashMap<String, String>();

	private void loadItemIds() throws NumberFormatException, IOException {

		File file = new File(instance.getDataFolder(), "items.csv");
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			copy(getResource("items.csv"), file);
		}
		@SuppressWarnings("resource")
		BufferedReader r = new BufferedReader(new FileReader(file));

		String line;

		int l = 0;
		ItemStack stock;
		String id_dur;
		while ((line = r.readLine()) != null) {
			l = l + 1;
			if (l > 3) {
				String[] values = line.split(",");
				stock = getItemStack(Integer.parseInt(values[1]), Short.parseShort(values[2]));
				nameToStack.put(values[0], stock);

				id_dur = String.valueOf(stock.getTypeId());
				if (stock.getDurability() > 0)
					id_dur += ":" + stock.getDurability();

				if (!stockToName.containsKey(id_dur))
					stockToName.put(id_dur, values[0]);
				/*
				 * sID = values[1];// + ":" + values[2]; if
				 * (!values[2].equalsIgnoreCase("0")) sID+=values[2];
				 * 
				 * if (!idToName.containsKey(sID)) idToName.put(sID, values[0]);
				 */
			}
		}

	}

	public static String getItemName(ItemStack stock) {
		/*
		 * String id_dur = String.valueOf(stock.getTypeId()); if
		 * (stock.getDurability() > 0) id_dur += ";" + stock.getDurability();
		 * 
		 * if (stockToName.containsKey(id_dur)) return stockToName.get(id_dur);
		 * 
		 * return stock.getType().name();
		 */
		return getItemName(stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock));
	}

	public static String getItemName(int itemId, short itemDur, String itemEnchant) {
		String id_dur = String.valueOf(itemId);
		if (itemDur > 0)
			id_dur += ":" + itemDur;

		//Logger.debug("getItemName itemDur: " + itemDur);
		//Logger.debug("getItemName id_dur: " + id_dur);
		
		String name = null;
		if (stockToName.containsKey(id_dur))
			name = stockToName.get(id_dur);

		//Logger.debug("getItemName name: " + name);
		
		if (name != null) {
			if (itemEnchant != null && !itemEnchant.equals(""))
				name += "-" + itemEnchant;
			return name;
		}

		return id_dur + ((itemEnchant != null) ? itemEnchant : "");
	}

	public static String getItemName(int itemId, short itemDur) {
		return getItemName(itemId, itemDur, null);
	}

	public static String getItemName(int itemId) {
		return getItemName(itemId, (short) 0);
	}

	public void copy(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		in.close();
	}

	public static boolean isGear(Material mat) {
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

		return false;
	}

	public static int getFitAmount(ItemStack itemStack, PlayerInventory inventory) {
		return getFitAmount(itemStack, 64 * 36, inventory);//36 slots in a player inventory.
	}
	
	public static int getFitAmount(ItemStack itemStack, int amount, PlayerInventory inventory) {
		for (int i = amount; i > 0; i--) {
			itemStack.setAmount(i);
			if (InventoryUtil.fits(itemStack, inventory) == true) {
				return i;
			}
		}
		return 0;
	}

	public static double getEstimatedBuyPrice(ItemStack stock) throws SQLException, IOException, InvalidConfigurationException {
		return getEstimatedBuyPrice(stock, stock.getAmount());
	}

	public static double getEstimatedBuyPrice(ItemStack stock, int amount) throws SQLException, IOException, InvalidConfigurationException {
		List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER);

		if (orders.size() <= 0)
			return 0;

		Order o;
		int canTrade, traded;
		double moneySpent = 0.0;
		for (int i = 0; i < orders.size(); i++) {
			if (amount <= 0)
				break;

			o = orders.get(i);

			canTrade = amount;
			if (!o.isInfinite())
				canTrade = Math.min(o.getAmount(), amount);

			if (canTrade <= 0)
				break;

			traded = canTrade;

			double spend = (traded * o.getPrice());
			moneySpent += spend;

			amount -= traded;

		}

		return moneySpent;
	}

	public static double getEstimatedSellPrice(ItemStack stock) throws SQLException, IOException, InvalidConfigurationException {
		return getEstimatedSellPrice(stock, stock.getAmount());
	}

	public static double getEstimatedSellPrice(ItemStack stock, int amount) throws SQLException, IOException, InvalidConfigurationException {
		List<Order> orders = Plugin.database.search(stock, Order.BUY_ORDER);

		if (orders.size() <= 0)
			return 0;

		Order o;
		int canTrade, traded;
		double moneySpent = 0.0;
		for (int i = (orders.size() - 1); i >= 0; i--) {
			if (amount <= 0)
				break;

			o = orders.get(i);

			canTrade = amount;
			if (!o.isInfinite())
				canTrade = Math.min(o.getAmount(), amount);

			if (canTrade <= 0)
				break;

			traded = canTrade;

			double spend = (traded * o.getPrice());
			moneySpent += spend;

			amount -= traded;

		}

		return moneySpent;
	}

}
