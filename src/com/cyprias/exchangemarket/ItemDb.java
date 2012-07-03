package com.cyprias.exchangemarket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.Material;

public class ItemDb {
	private ExchangeMarket plugin;

	private File file;

	public ItemDb(ExchangeMarket plugin) {
		this.plugin = plugin;

		file = new File(plugin.getDataFolder(), "items.csv");
		// if (!file.exists()) {
		file.getParentFile().mkdirs();
		copy(plugin.getResource("items.csv"), file);
		// }

		loadFile();
	}

	public void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public itemData getItemID(String itemName) {
		if (nameToID.containsKey(itemName))
			return nameToID.get(itemName);
		return null;
	}

	public String getItemName(int itemID, int itemDur) {
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
	HashMap<String, String> idToName = new HashMap<String, String>();

	public static ItemStack getItemStack(int itemID, short itemDur) {
		ItemStack is = new ItemStack(itemID, 1);
		is.setDurability(itemDur);

		return is;
	}

	public static ItemStack getItemStack(String id) {
		int itemid = 0;
		String itemname = null;
		short metaData = 0;
		
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

		if (itemid > 0){
			return getItemStack(itemid, metaData);
		}
		if (itemname != null){
			if (!nameToID.containsKey(itemname)) {
				return null;
			}
			itemData iD = nameToID.get(itemname);
			return getItemStack(iD.itemID, iD.itemDur);
		}
		
		return null;
	}

	private void loadFile() {
		try {
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

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// log.info("1 is " + idToName.get("1:0"));

	}

}