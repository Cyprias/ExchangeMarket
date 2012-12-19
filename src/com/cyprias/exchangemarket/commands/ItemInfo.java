package com.cyprias.exchangemarket.commands;

import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.cyprias.exchangemarket.Commands;
import com.cyprias.exchangemarket.ExchangeMarket;
import com.cyprias.exchangemarket.ItemDb;

public class ItemInfo {
	private ExchangeMarket plugin;

	public ItemInfo(ExchangeMarket plugin) {
		this.plugin = plugin;
		// TODO Auto-generated constructor stub
	}
	private String F(String string, Object... args) {
		return ExchangeMarket.F(string, args);
	}

	private String L(String string) {
		return ExchangeMarket.L(string);
	}
	
	/**/
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (!plugin.commands.hasCommandPermission(sender, "exchangemarket.iteminfo")) {
			return true;
		}
		
		Player player = (Player) sender;
		
		ItemStack item = player.getItemInHand();
		
		if (args.length>1)
			item = ItemDb.getItemStack(args[1]);

		plugin.sendMessage(sender, F("itemInfomation"));
		String itemName = ItemDb.getItemName(item.getTypeId(), item.getDurability());
		String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
		
		if (itemEnchants != null){
			plugin.sendMessage(sender, F("itemNameAndID", itemName, item.getTypeId() + "-" + itemEnchants) );
		}else{
			plugin.sendMessage(sender, F("itemNameAndID", itemName, item.getTypeId()) );
		}
		
		
		
		Map<Enchantment, Integer> ench = item.getEnchantments();

		if (ench.size() > 0){
			for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : ench.entrySet()) {
				plugin.sendMessage(sender, entry.getKey().getName() + " " + entry.getValue());
				
				
				
			}
		}
		
		return false;
	}
	
	
}
