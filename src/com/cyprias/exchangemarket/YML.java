package com.cyprias.exchangemarket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class YML {
	HashMap<String, File> Files = new HashMap<String, File>();
	HashMap<String, FileConfiguration> FileConfigs = new HashMap<String, FileConfiguration>();
	private ExchangeMarket plugin;
	
	public YML(ExchangeMarket plugin2) {
		this.plugin = plugin2;
	}
	public boolean reloadYMLConfig(String file) {
		if (!FileConfigs.containsKey(file)){
			FileConfigs.put(file, new YamlConfiguration());
		}
		
		try {
			Files.put(file, new File(plugin.getDataFolder(), file));

			if (!Files.get(file).exists()) {
				InputStream r = plugin.getResource(file);
				if (r == null)
					return false;
				
				Files.get(file).getParentFile().mkdirs();
				copy(plugin.getResource(file), Files.get(file));
			}

			FileConfigs.get(file).load(Files.get(file));
		} catch (Exception e) {e.printStackTrace();
		}
		return true;
	}
	
	public static void copy(InputStream in, File file) {
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

	public FileConfiguration getYMLConfig(String file, Boolean loadNewKeys) {
		if (!Files.containsKey(file)) {
			if (reloadYMLConfig(file) == false)
				return null;
		}
		
		if (loadNewKeys == true)
			copyNewKeysToDisk(file);
		
		return FileConfigs.get(file);
	}

	public FileConfiguration getYMLConfig(String file) {
		return getYMLConfig(file, false);
	}
	
	public void saveYMLFile(String fileName) {
		if (!FileConfigs.containsKey(fileName)) {
			FileConfigs.put(fileName, new YamlConfiguration());
		}

		try {
			FileConfigs.get(fileName).save(Files.get(fileName));
		} catch (IOException e) {e.printStackTrace();
		}
	}
	
	public void copyNewKeysToDisk(String fileName){
		InputStream in = plugin.getResource(fileName);

		if (in == null)//File isn't in our jar, exit.
			return;
		
		//Load the stream to a ymlconfig object.
		YamlConfiguration locales = new YamlConfiguration();
		try {
			locales.load(in);
		} catch (IOException e1) {e1.printStackTrace();
		} catch (InvalidConfigurationException e1) {e1.printStackTrace();
		}
		
		//Load the file from disk.
		FileConfiguration targetConfig = plugin.yml.FileConfigs.get(fileName);
		Boolean save = false;
		String value;
		for (String key : locales.getKeys(false)) {
			value = locales.getString(key);

			if (targetConfig.getString(key) == null){
				//Our stream has a key the file on disk doesn't have, copy the new key to file. 
				
				plugin.info("Copying new locale key [" + key + "]=[" + value + "] to " + fileName+".");
				
				targetConfig.set(key, value);
				save = true; //Only save if we make changes.
			}
			
		}

		if (save == true){
			//Save changes to disk.
			try {
				targetConfig.save(plugin.yml.Files.get(fileName));
			} catch (IOException e) {e.printStackTrace();
			}
		}
	}
	
}
