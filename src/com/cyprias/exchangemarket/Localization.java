package com.cyprias.exchangemarket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class Localization {
	private ExchangeMarket plugin;



	public static HashMap<String, String> Strings = new HashMap<String, String>();

	public Localization(ExchangeMarket plugin) {
		this.plugin = plugin;

		loadLocales();
		
	}

	public void loadLocales() {

		Strings.clear();
		
		FileConfiguration locales = plugin.yml.getYMLConfig(Config.locale, true); //

		if (locales != null){
			String value;
			for (String key : locales.getKeys(false)) {
				value = locales.getString(key);
				Strings.put(key, value.replaceAll("(?i)&([a-k0-9])", "\u00A7$1"));// §
			}
		}
		
		
		locales = plugin.yml.getYMLConfig("enUS.yml", true); //default to english.
		
		if (locales != null){
			String value;
			for (String key : locales.getKeys(false)) {
				if (!Strings.containsKey(key)){
					value = locales.getString(key);
					//plugin.info("Pulling " + key + " from english file.");
					Strings.put(key, value.replaceAll("(?i)&([a-k0-9])", "\u00A7$1"));// §
				}
			}
		}


	}

	static public String L(String key) {
		return Strings.get(key);
	}

	static public String F(String key, Object... args) {
		String value = Strings.get(key).toString();
		try {
			if (value != null || args != null)
				value = String.format(value, args); // arg.toString()
		} catch (Exception e) {e.printStackTrace();
		}
		return value;
	}


	
	
	





}
