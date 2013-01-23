package com.cyprias.ExchangeMarket;

import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.Breeze.BlockUtil;
import com.cyprias.ExchangeMarket.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.Breeze.PriceUtil;
import com.cyprias.ExchangeMarket.Breeze.StringUtil;

/*
 * Our signs mostly emulate Acrobot's ChestShop implication except for top line, player name isn't important for us.
 * https://github.com/Acrobot/ChestShop-3/blob/master/com/Acrobot/ChestShop
 */

public class Signs {
	public static final byte NAME_LINE = 0;
	public static final byte QUANTITY_LINE = 1;
	public static final byte PRICE_LINE = 2;
	public static final byte ITEM_LINE = 3;
	
    public static boolean isValid(Sign sign) {
        return isValid(sign.getLines());
    }

    public static boolean isValid(String[] line) {
        return isValidPreparedSign(line) && (line[PRICE_LINE].toUpperCase().contains("B") || line[PRICE_LINE].toUpperCase().contains("S")) && !line[NAME_LINE].isEmpty();
    }

    public static boolean isValid(Block sign) {
        return BlockUtil.isSign(sign) && isValid((Sign) sign.getState());
    }
    
    
	public static String formatPriceLine(String thirdLine) {
		String line = thirdLine;
		String[] split = line.toUpperCase().split(":");

		if (PriceUtil.isPrice(split[0])) {
			line = "B " + line;
		}
		if (split.length == 2 && PriceUtil.isPrice(split[1])) {
			line += " S";
		}

		if (line.length() > 15) {
			line = line.replace(" ", "");
		}

		line = line.replace('b', 'B').replace('s', 'S');

		return (line.length() > 15 ? null : line);
	}

	public static final Pattern[] SHOP_SIGN_PATTERN = { Pattern.compile("^\\[Exchange\\]$"), Pattern.compile("^[1-9][0-9]*$"),
		Pattern.compile("(?i)^[\\d.bs(free) :]+$"), Pattern.compile("^[\\w : -]+$") };

	public static boolean isValidPreparedSign(String[] lines) {
		for (int i = 0; i < 4; i++) {
			if (!SHOP_SIGN_PATTERN[i].matcher(lines[i]).matches()) {
				return false;
			}
		}
		return lines[PRICE_LINE].indexOf(':') == lines[PRICE_LINE].lastIndexOf(':');
	}

	public static String formatItemLine(String line, ItemStack item) {
		if (MaterialUtil.Odd.getFromString(line) != null) {
			return line;
		}

		String formatted, data = "";
		String[] split = line.split(":|-", 2);

		if (MaterialUtil.ENCHANTMENT.matcher(line).matches()) {
			data = '-' + MaterialUtil.ENCHANTMENT.matcher(line).group();
		}

		String longItemName = MaterialUtil.getName(item, true);
		ItemStack formattedItem = MaterialUtil.getItem(longItemName + data);

		if (longItemName.length() < (15 - data.length()) && formattedItem != null && MaterialUtil.equals(formattedItem, item)) {
			return StringUtil.capitalizeFirstLetter(longItemName + data);
		}

		formatted = MaterialUtil.getName(item, false);
		data = (split.length == 2 ? split[1] : "");

		if (formatted.length() > (15 - 1 - data.length())) {
			formatted = formatted.substring(0, (15 - 1 - data.length()));
		}

		formattedItem = MaterialUtil.getItem(formatted);

		if (formattedItem == null || formattedItem.getType() != item.getType()) {
			formatted = String.valueOf(item.getTypeId());
		}

		if (split.length == 2) {
			int dataValuePos = line.indexOf(split[1], split[0].length());
			formatted += line.charAt(dataValuePos - 1) + split[1];
		}

		return StringUtil.capitalizeFirstLetter(formatted);
	}
}
