package com.cyprias.ExchangeMarket;

import java.util.HashMap;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import com.cyprias.ExchangeMarket.configuration.Config;

public enum Perm {
	
	SEARCH("exchangemarket.search"),
	PRICE("exchangemarket.price"),
	ORDERS("exchangemarket.orders"),
	LIST("exchangemarket.list"),
	TRANSACTIONS("exchangemarket.transactions"),
	LOGIN_PENDING_COLLECT("exchangemarket.loginPendingCollection"),
	ITEM_INFO("exchangemarket.iteminfo"),
	PASSWORD("exchangemarket.password"),
	VERSION("exchangemarket.version"),
	NOTIFIED_SELLORDER("exchangemarket.announceneworder.sell"),
	NOTIFIED_BUYORDER("exchangemarket.announceneworder.buy"),
	INFO("exchangemarket.info"),
	SET_PRICE("exchangemarket.setprice"),
	PARENT_INFO("exchangemarket.user-safe", SET_PRICE, INFO, NOTIFIED_SELLORDER, NOTIFIED_BUYORDER, VERSION, SEARCH, PRICE,ORDERS, TRANSACTIONS, LIST, LOGIN_PENDING_COLLECT, ITEM_INFO,PASSWORD),
	
	SELL("exchangemarket.sell"),
	BUY("exchangemarket.buy"),
	SELL_ORDER("exchangemarket.sellorder"),
	BUY_ORDER("exchangemarket.buyorder"),
	COLLECT("exchangemarket.collect"),
	SELL_HAND("exchangemarket.sellhand"),
	CANCEL("exchangemarket.cancel"),
	CONFIRM("exchangemarket.confirm"),
	RETURN("exchangemarket.return"),
	USE_EXCHANGE_SIGN("exchangemarket.use-exchange-sign"),
	
	PARENT_USER("exchangemarket.user", RETURN, USE_EXCHANGE_SIGN, PARENT_INFO, SELL,BUY,SELL_ORDER,BUY_ORDER,COLLECT,SELL_HAND,CANCEL,CONFIRM),
	
	INF_BUY("exchangemarket.infbuy"),
	INF_SELL("exchangemarket.infsell"),
	RELOAD("exchangemarket.reload"),
	REMOVE("exchangemarket.remove"),
	LOGIN_NEW_VERSION("exchangemarket.loginNewVersion"),
	PARENT_ADMIN("exchangemarket.admin", PARENT_USER, INF_BUY,INF_SELL,RELOAD,REMOVE,LOGIN_NEW_VERSION),
	
	TEST("exchangemarket.test"),
	
	PARENT_ALL("exchangemarket.*", PARENT_ADMIN);

	private Perm(String value, Perm... childrenArray) {
		this(value, String.format(DEFAULT_ERROR_MESSAGE, value), childrenArray);
	}

	private Perm(String perm, String errorMess) {
		this.permission = perm;
		this.errorMessage = errorMess;
		this.bukkitPerm = new Permission(permission, PermissionDefault.getByName(Config.getString("properties.permission-default")));
	}

	private Perm(String value, String errorMess, Perm... childrenArray) {
		this(value, String.format(DEFAULT_ERROR_MESSAGE, value));
		for (Perm child : childrenArray) {
			child.setParent(this);
		}
	}

	public static HashMap<String, PermissionAttachment> permissions = new HashMap<String, PermissionAttachment>();

	private final String permission;
	public static final String DEFAULT_ERROR_MESSAGE = "You do not have access to %s";

	public Perm getParent() {
		return parent;
	}

	private Permission bukkitPerm;
	private Perm parent;
	private final String errorMessage;

	private void setParent(Perm parentValue) {
		if (this.parent != null)
			return;
		this.parent = parentValue;
	}

	public String getPermission() {
		return permission;
	}

	public void loadPermission(PluginManager pm) {
		pm.addPermission(bukkitPerm);
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void unloadPermission(PluginManager pm) {
		pm.removePermission(bukkitPerm);
	}
}
