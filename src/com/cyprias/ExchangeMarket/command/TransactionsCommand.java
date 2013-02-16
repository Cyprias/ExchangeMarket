package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.database.Order;
import com.cyprias.ExchangeMarket.database.Transaction;

public class TransactionsCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) throws SQLException {
		if (Plugin.hasPermission(sender, Perm.TRANSACTIONS))
			if (Plugin.database.getPlayerTransactionCount(sender) > 0)
				list.add("/%s transactions - Show transactions for your orders.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.TRANSACTIONS, "/%s transactions [page]", cmd);
	}

	public boolean hasValues() {
		return false;
	}
	
	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.TRANSACTIONS)) 
			return false;
		
		int page = -1; //Default to last page.
		if (args.length > 0) {// && args[1].equalsIgnoreCase("compact"))
			if (Plugin.isInt(args[0])) {
				page = Integer.parseInt(args[0]);
				if (page>0)
					page-=1;
			} else {
				ChatUtils.error(sender, "Invalid page: " +  args[0]);
				return true;
			}
		}
		
		
		List<Transaction> transactions = Plugin.database.listTransactions(sender, page);
		
		if (transactions == null || transactions.size() <= 0){
			ChatUtils.send(sender, "§7You have had no transactions.");
			return true;
		}
			
		String date;
		for (Transaction transaction :transactions){
			
			date = new SimpleDateFormat("MM/dd/yy").format(transaction.getTimestamp());
			
			if (transaction.getType() == Order.BUY_ORDER){
				ChatUtils.sendSpam(sender, String.format("§7Bought §f%s§7x§f%s §7from §f%s §7on §f%s§7.", transaction.getItemName(), transaction.getAmount(), transaction.getOrderer(), date));
			}else if (transaction.getType() == Order.SELL_ORDER){
				ChatUtils.sendSpam(sender, String.format("§7Sold §f%s§7x§f%s §7to §f%s §7on §f%s§7.", transaction.getItemName(), transaction.getAmount(), transaction.getOrderer(), date));
				
			}
		}
		return true;
	}
	
}
