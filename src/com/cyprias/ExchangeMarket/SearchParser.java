package com.cyprias.ExchangeMarket;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class SearchParser {
	public CommandSender player = null;
	public List<String> players = new ArrayList<String>();
	public List<ItemStack> items = new ArrayList<ItemStack>();

	public String searchString;

	public SearchParser(CommandSender player, String[] args) throws IllegalArgumentException {
		this.player = player;

		String lastParam = "";
		boolean paramSet = false;

		searchString = Plugin.getFinalArg(args, 0);

		String arg;
		for (int i = 0; i < args.length; i++) {
			arg = args[i];
			if (arg.isEmpty())
				continue;

			Logger.info("arg1: " + arg);
			ItemStack stock;
			
			if (!paramSet) {
				if (arg.length() < 2)
					throw new IllegalArgumentException("Invalid argument format: &7" + arg);
				if (!arg.substring(1, 2).equals(":")) {
					if (arg.contains(":"))
						throw new IllegalArgumentException("Invalid argument format: &7" + arg);

					// No arg specified, treat as player
					
					stock = Plugin.getItemStack(arg);
					if (stock == null || stock.getTypeId() == 0)
						throw new IllegalArgumentException("Invalid item: &7" + arg);
					items.add(stock);
					
					continue;
				}

				lastParam = arg.substring(0, 1).toLowerCase();
				paramSet = true;

				if (arg.length() == 2) {
					if (i == (args.length - 1)) // No values specified
						throw new IllegalArgumentException("Invalid argument format: &7" + arg);
					else
						// User put a space between the colon and value
						continue;
				}

				// Get values out of argument
				arg = arg.substring(2);
			}

			Logger.info("arg2: " + arg);
			
			if (paramSet) {
				if (arg.isEmpty()) {
					throw new IllegalArgumentException("Invalid argument format: &7" + lastParam + ":");
				}

				String[] values = arg.split(",");

				// Players
				if (lastParam.equals("p"))
					for (String p : values)
						players.add(p);

				if (lastParam.equals("i"))
					for (String p : values) {

						stock = Plugin.getItemStack(p);
						if (stock == null || stock.getTypeId() == 0)
							throw new IllegalArgumentException("Invalid item: &7" + p);
						items.add(stock);
					}

				Logger.info("arg3: " + arg);
				
				/*
				 * // Writer if (lastParam.equals("w")) for (String p : values)
				 * writers.add(p);
				 * 
				 * // Keyword if (lastParam.equals("k")) for (String p : values)
				 * keywords.add(p);
				 */
			} else
				throw new IllegalArgumentException("Invalid parameter supplied: &7" + lastParam);

			paramSet = false;
		}

	}

}
