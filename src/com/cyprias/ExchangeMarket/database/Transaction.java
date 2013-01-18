package com.cyprias.ExchangeMarket.database;

import java.sql.Timestamp;
import java.util.Date;

import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.Plugin;

public class Transaction {

	//private int id;
	private int type;
	private String orderer;
	private int itemId;
	private short itemDur;
	private String itemEnchants;
	private int amount;
	//private double price;
	//private String owner;
	private Timestamp timestamp;

	public Transaction(int id, int type, String orderer, int itemId, short itemDur, String itemEnchants, int amount, double price, String owner, Timestamp timestamp) {
		//this.id = id;
		this.type = type;
		this.orderer = orderer;
		this.itemId = itemId;
		this.itemDur = itemDur;
		this.itemEnchants = itemEnchants;
		this.amount = amount;
		//this.price = price;
		//this.owner = owner;
		this.timestamp = timestamp;
	}

	public ItemStack getItemStack(){
		return Plugin.getItemStack(itemId, itemDur, itemEnchants);
	}
	
	public String getItemName(){
		return Plugin.getItemName(itemId, itemDur, itemEnchants);
	}

	public int getType() {
		return type;
	}

	public Object getAmount() {
		return amount;
	}

	public Date getTimestamp() {
		return timestamp;
	}
	
	public String getOrderer(){
		return orderer;
	}
	
}
