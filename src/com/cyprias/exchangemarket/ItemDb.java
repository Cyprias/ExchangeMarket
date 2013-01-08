package com.cyprias.exchangemarket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.cyprias.Utils.MaterialUtil;

public class ItemDb {
	private File file;

	public ItemDb(JavaPlugin plugin) throws IOException {
		file = new File(plugin.getDataFolder(), "items.csv");
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			copy(plugin.getResource("items.csv"), file);
		}

		loadFile();
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

	public itemData getItemID(String itemName) {
		if (nameToID.containsKey(itemName))
			return nameToID.get(itemName);
		return null;
	}


	
	public static String getItemName(ItemStack stock, Boolean includeEnchants) {
		String itemName = getItemName(stock.getTypeId(), stock.getDurability());
		if (stock.getEnchantments().size() > 0)
			itemName += "-"+MaterialUtil.Enchantment.encodeEnchantment(stock);
		
		return itemName;
	}
	
	public static String getItemName(ItemStack stock) {
		return getItemName(stock, false);
	}	
	
	public static String getItemName(int itemID, short itemDur) {
		if (idToName.containsKey(itemID + ":" + itemDur))
			return idToName.get(itemID + ":" + itemDur);

		return itemID + ":" + itemDur;
	}

	static class itemData {
		String itemName;
		int itemID;
		short itemDur;

		public itemData(String string, int itemid2, short metaData) {
			itemName = string;
			itemID = itemid2; // Integer.parseInt(itemid2);
			itemDur = metaData; // Short.parseShort(metaData);
		}
	}

	static HashMap<String, itemData> nameToID = new HashMap<String, itemData>();
	static HashMap<String, String> idToName = new HashMap<String, String>();

	public static ItemStack getItemStack(int itemID, short itemDur, String enchants) {
		ItemStack itemStack = new ItemStack(itemID, 1);
		itemStack.setDurability(itemDur);

		if (enchants != null && !enchants.equalsIgnoreCase("")) {
			itemStack.addEnchantments(MaterialUtil.Enchantment.getEnchantments(enchants));
		}

		return itemStack;
	}

	public static ItemStack getItemStack(int itemID, short itemDur, String enchants, int amount) {
		ItemStack itemStack = getItemStack(itemID, itemDur, enchants);
		itemStack.setAmount(amount);
		return itemStack;
	}
	
	
	public static Logger log = Logger.getLogger("Minecraft");

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
			if (!nameToID.containsKey(itemname)) {
				return null;
			}
			itemData iD = nameToID.get(itemname);
			return getItemStack(iD.itemID, iD.itemDur, enchant);
		}

		return null;
	}

	private void loadFile() throws NumberFormatException, IOException {
		@SuppressWarnings("resource")
		BufferedReader r = new BufferedReader(new FileReader(file));

		String line;

		int l = 0;
		String sID;
		while ((line = r.readLine()) != null) {
			l = l + 1;
			if (l > 3) {
				String[] values = line.split(",");
				nameToID.put(values[0], new itemData(values[0], Integer.parseInt(values[1]), Short.parseShort(values[2])));

				sID = values[1] + ":" + values[2];
				if (!idToName.containsKey(sID))
					idToName.put(sID, values[0]);

			}
		}


	}

}
