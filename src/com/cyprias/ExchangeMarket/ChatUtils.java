package com.cyprias.ExchangeMarket;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.cyprias.ExchangeMarket.configuration.Config;

public class ChatUtils {

	public static void broadcast(String format, Object... args) {
		broadcast(String.format(format, args));
	}

	public static void broadcast(String message) {
		message = replaceColorCodes(message);
		String[] messages;
		messages = message.split(Config.getString("properties.line-separator"));
		String prefix = getChatPrefix();
		for (int cntr = 0; cntr < messages.length; cntr++)
			messages[cntr] = prefix + messages[cntr];
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(messages);
		}
		Bukkit.getConsoleSender().sendMessage(messages);

	}

	public static void broadcastRaw(String format, Object... args) {
		broadcastRaw(String.format(format, args));
	}

	public static void broadcast(Perm permission, String message) {
		message = replaceColorCodes(message);
		String[] messages;
		messages = message.split(Config.getString("properties.line-separator"));
		String prefix = getChatPrefix();
		for (int cntr = 0; cntr < messages.length; cntr++)
			messages[cntr] = prefix + messages[cntr];
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (Plugin.hasPermission(player, permission))
				player.sendMessage(messages);
		}
		Bukkit.getConsoleSender().sendMessage(messages);

	}

	public static void broadcastRaw(String message) {
		message = replaceColorCodes(message);
		String[] messages;
		messages = message.split(Config.getString("properties.line-separator"));
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(messages);
		}
		Bukkit.getConsoleSender().sendMessage(messages);

	}

	public static void notify(CommandSender sender, String message) {
		send(sender, getChatPrefix() + message);
	}

	public static void sendSpam(CommandSender sender, String message) {
		message = replaceColorCodes(message);
		String[] messages;
		messages = message.split(Config.getString("properties.line-separator"));
		sender.sendMessage(messages);

	}

	public static void send(CommandSender sender, String message) {
		sendSpam(sender, message);
		if (Config.getBoolean("properties.show-plugin-replies") && !(sender instanceof ConsoleCommandSender))
			Logger.infoRaw(ChatColor.stripColor(getChatPrefix() + sender.getName() + " > " + message));
	}

	public static void send(CommandSender sender, ChatColor color, String format, Object... args) {
		sender.sendMessage(color + getChatPrefix() + String.format(format, args));
	}

	public static void sendRaw(CommandSender sender, ChatColor color, String format, Object... args) {
		sender.sendMessage(color + String.format(format, args));
	}

	public static void sendCommandHelp(CommandSender sender, String line, org.bukkit.command.Command cmd) {
		sendCommandHelp(sender, "- ", line, cmd);
	}

	public static void sendCommandHelp(CommandSender sender, String prefix, String line, org.bukkit.command.Command cmd) {
		sendRaw(sender, ChatColor.GRAY, prefix + line, cmd.getLabel());
	}

	public static boolean sendCommandHelp(CommandSender sender, Perm permission, String line, org.bukkit.command.Command cmd) {
		return sendCommandHelp(sender, permission, "- ", line, cmd);
	}

	public static boolean sendCommandHelp(CommandSender sender, Perm permission, String prefix, String line, org.bukkit.command.Command cmd) {
		if (!Plugin.hasPermission(sender, permission))
			return false;
		sendRaw(sender, ChatColor.GRAY, prefix + line, cmd.getLabel());
		return true;
	}

	public static void error(CommandSender sender, String format, Object... args) {
		sender.sendMessage(getChatPrefix() + ChatColor.RED + String.format(format, args));
	}

	public static void errorRaw(CommandSender sender, String format, Object... args) {
		sender.sendMessage(ChatColor.RED + String.format(format, args));
	}

	public static final String getChatPrefix() {
		return replaceColorCodes(Config.getString("properties.chat-prefix"));
	}

	// replace color codes with the colors
	public static final String replaceColorCodes(String mess) {
		return mess.replaceAll("(&([" + colorCodes + "]))", "\u00A7$2");
	}

	// get rid of color codes
	public static final String cleanColorCodes(String mess) {
		return mess.replaceAll("(&([" + colorCodes + "]))", "");
	}

	private static final String colorCodes;

	static {
		String string = "";
		for (ChatColor color : ChatColor.values()) {
			char c = color.getChar();
			if (!Character.isLetter(c)) {
				string += c;
			} else {
				string += Character.toUpperCase(c);
				string += Character.toLowerCase(c);
			}
		}
		colorCodes = string;
	}
}