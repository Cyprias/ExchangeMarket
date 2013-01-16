package com.cyprias.ExchangeMarket;

import com.cyprias.ExchangeMarket.configuration.Config;

public class Logger {
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("AdminNotes");
	private static final Plugin plugin = Plugin.getInstance();

	public static void debug(String mess) {
		if (Config.getBoolean("properties.debugging"))
			logger.info(getLogPrefix() +"[Debug] "+ mess);
	}
	
	public static void info(String mess) {
		logger.info(getLogPrefix() + mess);
	}

	public static void info(String format, Object... args) {
		logger.info(getLogPrefix() + String.format(format, args));
	}

	public static void info(Object... args) {
		String msg = "%s";
		for (int i=1; i<args.length; i++){
			msg += ", %s";
		}
		info(msg, args);
	}
	
	public static void infoRaw(String mess) {
		logger.info(mess);
	}

	public static void infoRaw(String format, Object... args) {
		logger.info(String.format(format, args));
	}

	public static void warning(String mess) {
		logger.warning(getLogPrefix() + mess);
	}

	public static void warning(String format, Object... args) {
		logger.warning(getLogPrefix() + String.format(format, args));
	}

	public static void warningRaw(String mess) {
		logger.warning(mess);
	}

	public static void warningRaw(String format, Object... args) {
		logger.warning(String.format(format, args));
	}

	public static void severe(String mess) {
		logger.severe(getLogPrefix() + mess);
	}

	public static void severe(String format, Object... args) {
		logger.severe(getLogPrefix() + String.format(format, args));
	}

	public static void severeRaw(String mess) {
		logger.severe(mess);
	}

	public static void severeRaw(String format, Object... args) {
		logger.severe(String.format(format, args));
	}

	public static final String getLogPrefix() {
		return String.format("[%s] v%s: ", plugin.getName(), plugin.getDescription().getVersion());
	}
}