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

public class Info {
	private ExchangeMarket plugin;

	public Info(ExchangeMarket plugin) {
		this.plugin = plugin;
		// TODO Auto-generated constructor stub
	}

	/**/
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (!plugin.commands.hasCommandPermission(sender, "exchangemarket.info")) {
			return true;
		}
		
		Player player = (Player) sender;
		
		ItemStack item = player.getItemInHand();
		
		if (args.length>1)
			item = ItemDb.getItemStack(args[1]);
		
		
		
		int amount = item.getAmount();
		String itemEnchants = MaterialUtil.Enchantment.encodeEnchantment(item);
		String itemName = ItemDb.getItemName(item.getTypeId(), item.getDurability());
		
		if (itemEnchants != null){
			sender.sendMessage(itemName + "-" + itemEnchants + " x " + amount);
		}else{
			sender.sendMessage(itemName + " x " + amount);
		}
		
		Map<Enchantment, Integer> ench = item.getEnchantments();
		
		
		
		if (ench.size() > 0){
			for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : ench.entrySet()) {
				
				sender.sendMessage("enchant: " + entry.toString());
				
			}
			
		}
		
		return false;
	}
	
	
}
