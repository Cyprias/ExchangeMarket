package com.cyprias.ExchangeMarket.database;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.Plugin;

public class Parcel {
// id player itemI itemDur itemEnchant amount time;
	
	private int id;
	private String player;
	private int itemId;
	private short itemDur;
	private String itemEnchant;
	private int amount;
	private Timestamp time;

	public Parcel(int id, String player, int itemId, short itemDur, String itemEnchant, int amount, Timestamp time) {
		this.id = id;
		this.player = player;
		this.itemId = itemId;
		this.itemDur = itemDur;
		this.itemEnchant = itemEnchant;
		this.amount = amount;
		this.time = time;
	}

	public int getId(){
		return id;
	}
	public String getPlayer(){
		return player;
	}
	public int getItemId(){
		return itemId;
	}
	public short getItemDur(){
		return itemDur;
	}
	public String getItemEnchant(){
		return itemEnchant;
	}
	public int getAmount(){
		return amount;
	}
	public Timestamp getTime(){
		return time;
	}

	public ItemStack getItemStack(){
		return Plugin.getItemStack(itemId, itemDur, itemEnchant);
	}

	public boolean setAmount(int amount) throws SQLException, IOException, InvalidConfigurationException {
		boolean success = Plugin.database.setPackageAmount(id, amount);
		if (success)
			this.amount = amount;
		return success;
	}
	
}
