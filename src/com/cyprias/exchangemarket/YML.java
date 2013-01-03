package com.cyprias.exchangemarket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class YML extends YamlConfiguration {
	private static File file = null;

	public YML(InputStream fileStream) throws IOException, InvalidConfigurationException {
		// load yml from resources.
		load(fileStream);
	}

	public YML(File pluginDur, String fileName) throws FileNotFoundException, IOException, InvalidConfigurationException {
		// Load yml from directory.
		YML.file = new File(pluginDur, fileName);

		load(YML.file);
	}

	public YML(InputStream fileStream, File pluginDur, String fileName) throws FileNotFoundException, IOException, InvalidConfigurationException {
		// Copy yml resource to directory then load it.

		YML.file = new File(pluginDur, fileName);
		if (!YML.file.exists())
			YML.file = toFile(fileStream, pluginDur, fileName);

		load(YML.file);
	}

	public YML(InputStream fileStream, File pluginDur, String fileName, Boolean noLoad) throws IOException {
		// Just copy the stream to directory, no loading as YML.
		YML.file = new File(pluginDur, fileName);
		if (!YML.file.exists())
			YML.file = toFile(fileStream, pluginDur, fileName);
	}

	// Write a stream to file on disk, return the file object.
	private static File toFile(InputStream in, File pluginDur, String fileName) throws IOException {
		File file = new File(pluginDur, fileName);
		file.getParentFile().mkdirs();
		OutputStream out = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		in.close();
		return file;
	}

	public void save() throws IOException {
		save(file);
	}

}