package com.bhop4real.proton.client.hud;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Loads and saves HUD layout to a separate JSON file (hud_layout.json).
 * Provides simple getters for component positions with sensible defaults.
 */
public final class HudLayoutManager
{
	private static final String FILE_NAME = "hud_layout.json";
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static volatile Layout current = null;

	public static final class Layout
	{
		// Stored in screen coordinates
		public int watermarkX = 4;
		public int watermarkY = 4;

		// ArrayList uses top offset and right margin
		public int arrayListTop = 4;
		public int arrayListRightMargin = 4;

		// Coordinates use top-left position relative to bottom via offset
		public int coordinatesX = 4;
		public int coordinatesBottomOffset = 0;

		// Music label absolute
		public int musicX = 4;
		public int musicY = 18;
	}

	private HudLayoutManager() {}

	public static synchronized Layout get()
	{
		if (current == null)
		{
			current = loadInternal();
		}
		return current;
	}

	public static synchronized void save(Layout layout)
	{
		if (layout == null)
		{
			return;
		}
		try
		{
			ProtonClient client = ProtonClient.getInstance();
			if (client == null || client.getSettingsManager() == null)
			{
				return;
			}
			File dir = client.getSettingsManager().getConfigDirectory();
			File file = new File(dir, FILE_NAME);
			try (FileWriter writer = new FileWriter(file))
			{
				gson.toJson(layout, writer);
			}
			current = layout;
		}
		catch (Exception e)
		{
			Proton.logger.error("Failed to save HUD layout", e);
		}
	}

	private static Layout loadInternal()
	{
		try
		{
			ProtonClient client = ProtonClient.getInstance();
			if (client == null || client.getSettingsManager() == null)
			{
				return new Layout();
			}
			File dir = client.getSettingsManager().getConfigDirectory();
			File file = new File(dir, FILE_NAME);
			if (!file.exists())
			{
				return new Layout();
			}
			try (FileReader reader = new FileReader(file))
			{
				JsonObject obj = gson.fromJson(reader, JsonObject.class);
				if (obj == null) return new Layout();
				Layout l = new Layout();
				l.watermarkX = getInt(obj, "watermarkX", l.watermarkX);
				l.watermarkY = getInt(obj, "watermarkY", l.watermarkY);
				l.arrayListTop = getInt(obj, "arrayListTop", l.arrayListTop);
				l.arrayListRightMargin = getInt(obj, "arrayListRightMargin", l.arrayListRightMargin);
				l.coordinatesX = getInt(obj, "coordinatesX", l.coordinatesX);
				l.coordinatesBottomOffset = getInt(obj, "coordinatesBottomOffset", l.coordinatesBottomOffset);
				l.musicX = getInt(obj, "musicX", l.musicX);
				l.musicY = getInt(obj, "musicY", l.musicY);
				return l;
			}
		}
		catch (Exception e)
		{
			Proton.logger.error("Failed to load HUD layout", e);
			return new Layout();
		}
	}

	private static int getInt(JsonObject obj, String key, int def)
	{
		return obj.has(key) ? safeInt(obj.get(key), def) : def;
	}

	private static int safeInt(com.google.gson.JsonElement el, int def)
	{
		try
		{
			return el.getAsInt();
		}
		catch (Exception ignored)
		{
			return def;
		}
	}
}


