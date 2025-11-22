package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.ColorSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.util.render.RenderUtil;
import com.bhop4real.proton.client.util.shader.BlurUtil;
import com.bhop4real.proton.client.gui.config.ClickGuiPreferences;
import com.bhop4real.proton.client.gui.theme.ClickGuiThemeInterface;
import com.bhop4real.proton.client.music.MusicService;
import com.bhop4real.proton.client.hud.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.StringUtils;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Simple customizable HUD.
 * Features:
 * - Watermark with client name and FPS
 * - ArrayList of enabled modules
 * - Coordinates display
 * - Music info display
 * - Color customization with Static/Rainbow/Theme modes
 */
public class HUD extends Module
{
	private final BooleanSetting showWatermark = new BooleanSetting("Watermark", "Show Proton watermark", true);
	private final BooleanSetting showFps = new BooleanSetting("FPS", "Show FPS in watermark", true);
	private final BooleanSetting showArrayList = new BooleanSetting("ArrayList", "Show enabled modules", true);
	private final BooleanSetting showCoordinates = new BooleanSetting("Coordinates", "Show XYZ coordinates", true);
	private final BooleanSetting showMusic = new BooleanSetting("Music Info", "Show now playing track", true);
	private final BooleanSetting shadow = new BooleanSetting("Shadow", "Draw text with shadow", true);

	// ArrayList background customization (exclusive options)
	private final EnumSetting arrayListBackground = new EnumSetting(
			"ArrayList Background", "Background mode for ArrayList", "Off", "Off", "Solid", "Blur");
	private final ColorSetting arrayListBackgroundColor = new ColorSetting(
			"ArrayList BG Color", "Color for solid background", 0xAA111318);
	private final IntSetting arrayListBlurRadius = new IntSetting(
			"ArrayList Blur", "Gaussian blur radius", 8, 0, 32);
	private final IntSetting arrayListPaddingX = new IntSetting(
			"ArrayList Pad X", "Horizontal padding", 3, 0, 12);
	private final IntSetting arrayListPaddingY = new IntSetting(
			"ArrayList Pad Y", "Vertical padding", 1, 0, 8);

	private final EnumSetting colorMode = new EnumSetting("Color Mode", "Coloring of HUD text", "Static", "Static", "Rainbow", "Theme");
	private final ColorSetting staticColor = new ColorSetting("Static Color", "Color for Static mode", 0xFFFFFFFF);

	// Positions
	private final IntSetting watermarkX = new IntSetting("Watermark X", "Watermark horizontal position", 4, 0, 4000);
	private final IntSetting watermarkY = new IntSetting("Watermark Y", "Watermark vertical position", 4, 0, 4000);
	private final IntSetting arrayListMarginRight = new IntSetting("ArrayList Right", "Right margin for ArrayList", 4, 0, 4000);
	private final IntSetting arrayListTop = new IntSetting("ArrayList Top", "Top offset for ArrayList", 4, 0, 4000);
	private final IntSetting coordinatesX = new IntSetting("Coords X", "Coordinates horizontal position", 4, 0, 4000);
	private final IntSetting coordinatesY = new IntSetting("Coords Y", "Coordinates vertical position", 0, 0, 4000);
	private final IntSetting musicX = new IntSetting("Music X", "Music info horizontal position", 4, 0, 4000);
	private final IntSetting musicY = new IntSetting("Music Y", "Music info vertical position", 18, 0, 4000);

	public HUD()
	{
		super("HUD", "Displays heads-up display information", Category.RENDER, 0);

		addSetting(showWatermark);
		addSetting(showFps);
		addSetting(showArrayList);
		addSetting(showCoordinates);
		addSetting(showMusic);
		addSetting(shadow);
		addSetting(colorMode);
		addSetting(staticColor);
		addSetting(watermarkX);
		addSetting(watermarkY);
		addSetting(arrayListMarginRight);
		addSetting(arrayListTop);
		addSetting(arrayListBackground);
		addSetting(arrayListBackgroundColor);
		addSetting(arrayListBlurRadius);
		addSetting(arrayListPaddingX);
		addSetting(arrayListPaddingY);
		addSetting(coordinatesX);
		addSetting(coordinatesY);
		addSetting(musicX);
		addSetting(musicY);

		// Visibility: show only relevant options per mode
		arrayListBackgroundColor.setVisibilitySupplier(() -> "Solid".equalsIgnoreCase(arrayListBackground.getValue()));
		arrayListBlurRadius.setVisibilitySupplier(() -> "Blur".equalsIgnoreCase(arrayListBackground.getValue()));
		arrayListPaddingX.setVisibilitySupplier(showArrayList::getValue);
		arrayListPaddingY.setVisibilitySupplier(showArrayList::getValue);
	}

	@Override
	public boolean isDefaultEnabled()
	{
		return true;
	}

	@Override
	public void onRender()
	{
		// Do not render HUD while ClickGUI is open to avoid blur/state conflicts
		if (mc.currentScreen instanceof com.bhop4real.proton.client.gui.GuiClickGUI)
		{
			return;
		}
		// Also skip while Music Player UI is open
		if (mc.currentScreen instanceof com.bhop4real.proton.client.ui.music.MusicPlayerScreen)
		{
			return;
		}

		ScaledResolution sr = new ScaledResolution(mc);
		HudLayoutManager.Layout layout = HudLayoutManager.get();

		if (showWatermark.getValue())
		{
			int x = layout != null ? layout.watermarkX : watermarkX.getValue();
			int y = layout != null ? layout.watermarkY : watermarkY.getValue();
			drawWatermark(x, y);
		}

		if (showArrayList.getValue())
		{
			int top = layout != null ? layout.arrayListTop : arrayListTop.getValue();
			int rightMargin = layout != null ? layout.arrayListRightMargin : arrayListMarginRight.getValue();
			drawArrayList(sr, top, rightMargin);
		}

		if (showCoordinates.getValue())
		{
			int x = layout != null ? layout.coordinatesX : coordinatesX.getValue();
			int bottomOffset = layout != null ? layout.coordinatesBottomOffset : coordinatesY.getValue();
			drawCoordinates(sr, x, bottomOffset);
		}

		if (showMusic.getValue())
		{
			int x = layout != null ? layout.musicX : musicX.getValue();
			int y = layout != null ? layout.musicY : musicY.getValue();
			drawMusicInfo(x, y);
		}
	}

	private void drawWatermark(int x, int y)
	{
		String name = "Proton";
		String extra = "";
		if (showFps.getValue())
		{
			extra = " §7(" + Minecraft.getDebugFPS() + " FPS)";
		}
		String text = name + extra;
		int color = resolveColor(y);
		ScaledResolution sr = new ScaledResolution(mc);
		int textWidth = RenderUtil.getStringWidth(text);
		int drawX = x;
		// Auto alignment: if handle is on right half, right-align text using x as right margin anchor
		if (x > sr.getScaledWidth() / 2)
		{
			int rightMargin = Math.max(4, sr.getScaledWidth() - x - 16);
			drawX = sr.getScaledWidth() - rightMargin - textWidth;
		}
		RenderUtil.drawString(text, drawX, y, color, shadow.getValue());
	}

	private void drawArrayList(ScaledResolution sr, int top, int rightMargin)
	{
		ProtonClient client = ProtonClient.getInstance();
		if (client == null || client.getModuleManager() == null) return;

		String bgMode = arrayListBackground.getValue();
		boolean bgSolid = "Solid".equalsIgnoreCase(bgMode);
		boolean bgBlur = "Blur".equalsIgnoreCase(bgMode);

		List<Module> enabled = new ArrayList<>();
		for (Module module : client.getModuleManager().getModules())
		{
			if (module.isEnabled() && module.isVisible() && module.getCategory() != Category.THEMES && module != this)
			{
				enabled.add(module);
			}
		}

		enabled.sort(Comparator.comparingInt(m ->
		{
			String label = m.getName();
			return -RenderUtil.getStringWidth(label);
		}));

		int y = top;
		int lineHeight = Math.max(RenderUtil.getFontHeight(), 10) + 2;
		int screenWidth = sr.getScaledWidth();
		int screenRight = screenWidth - rightMargin;
		// Determine alignment by where the designer handle would be
		int handleX = screenWidth - rightMargin - 110; // HANDLE_WIDTH in designer
        boolean alignLeft = handleX < screenWidth / 2;

		for (int i = 0; i < enabled.size(); i++)
		{
			Module m = enabled.get(i);
			String label = m.getName();
			int width = RenderUtil.getStringWidth(label);
			int padX = arrayListPaddingX.getValue();
			int padY = arrayListPaddingY.getValue();
			int xText = alignLeft ? Math.max(4, handleX) : screenRight - width;
			int xBg = alignLeft ? xText - padX : screenRight - width - padX;
			int bgWidth = width + padX * 2;
			int bgHeight = lineHeight - 2 + padY * 2;
			int yBg = y - 1 - padY;

			// Background behind this row only (match text width)
			if (bgSolid)
			{
				com.bhop4real.proton.client.util.render.RenderUtil.drawRect(xBg, yBg, xBg + bgWidth, yBg + bgHeight, arrayListBackgroundColor.getColor());
			}
			else if (bgBlur && arrayListBlurRadius.getValue() > 0)
			{
				drawBlurRegion(xBg, yBg, bgWidth, yBg + bgHeight - yBg, arrayListBlurRadius.getValue());
			}
			int color = resolveColor(y + i * 10);

			// Optional right-side accent bar
			int accent = (color & 0xFFFFFF) | 0xAA000000;
			if (!alignLeft)
			{
				com.bhop4real.proton.client.util.render.RenderUtil.drawRect(screenRight + 2, y - 1, screenRight + 3, y + lineHeight - 3, accent);
			}

			RenderUtil.drawString(label, xText, y, color, shadow.getValue());
			y += lineHeight;
		}
	}

	private void drawCoordinates(ScaledResolution sr, int x, int yOffset)
	{
		if (mc.thePlayer == null) return;

		double px = round(mc.thePlayer.posX, 1);
		double py = round(mc.thePlayer.posY, 1);
		double pz = round(mc.thePlayer.posZ, 1);

		String coords = String.format("XYZ §7%s", StringUtils.ticksToElapsedTime((int) (mc.theWorld != null ? mc.theWorld.getTotalWorldTime() : 0)));
		String pos = String.format("X: %.1f Y: %.1f Z: %.1f", px, py, pz);

		int color = resolveColor(yOffset);
		RenderUtil.drawString(pos, x, sr.getScaledHeight() - RenderUtil.getFontHeight() * 2 - 6 - yOffset, color, shadow.getValue());
		RenderUtil.drawString(coords, x, sr.getScaledHeight() - RenderUtil.getFontHeight() - 4 - yOffset, color, shadow.getValue());
	}

	private int resolveColor(int indexOffset)
	{
		String mode = colorMode.getValue();
		if ("Rainbow".equalsIgnoreCase(mode))
		{
			return rainbow(indexOffset, 6.0F, 0.75F, 1.0F, 1.0F);
		}
		if ("Theme".equalsIgnoreCase(mode))
		{
			ClickGuiThemeInterface theme = resolveActiveTheme();
			// Use theme accent for HUD emphasis
			return theme != null ? theme.accent() : staticColor.getColor();
		}
		return staticColor.getColor();
	}

	private ClickGuiThemeInterface resolveActiveTheme()
	{
		ProtonClient client = ProtonClient.getInstance();
		if (client == null || client.getModuleManager() == null) return null;
		Module mod = client.getModuleManager().getModuleByName("ClickGUI");
		if (mod instanceof ClickGUI)
		{
			ClickGuiPreferences prefs = ((ClickGUI) mod).snapshotPreferences();
			return prefs != null ? prefs.theme() : null;
		}
		return null;
	}

	private static double round(double value, int places)
	{
		double factor = Math.pow(10, places);
		return Math.round(value * factor) / factor;
	}

	private int rainbow(int position, float speed, float saturation, float brightness, float alpha)
	{
		float hue = (System.currentTimeMillis() % (int)(360 * speed)) / (360.0F * speed);
		// Slight offset per line/item to create a gradient down the list
		hue = (hue + (position % 360) / 360.0F) % 1.0F;

		int rgb = java.awt.Color.HSBtoRGB(hue, clamp01(saturation), clamp01(brightness));
		int a = (int) (clamp01(alpha) * 255.0F) & 0xFF;
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	private float clamp01(float v)
	{
		if (v < 0.0F) return 0.0F;
		if (v > 1.0F) return 1.0F;
		return v;
	}

	private void drawMusicInfo(int x, int y)
	{
		try
		{
			MusicService.NowPlayingInfo now = MusicService.get().getNowPlaying();
			if (now == null || (now.title.isEmpty() && now.author.isEmpty()))
			{
				return;
			}
			String time = formatTime(now.positionMs) + "/" + formatTime(now.durationMs);
			String label = (now.title.isEmpty() ? "" : now.title) +
					(now.author.isEmpty() ? "" : " §7- " + now.author) +
					" §7[" + time + "]";
			int color = resolveColor(y);
			ScaledResolution sr = new ScaledResolution(mc);
			int textWidth = RenderUtil.getStringWidth(label);
			int drawX = x;
			// Auto alignment: right-align when handle placed on right half
			if (x > sr.getScaledWidth() / 2)
			{
				int rightMargin = Math.max(4, sr.getScaledWidth() - x - 16);
				drawX = sr.getScaledWidth() - rightMargin - textWidth;
			}
			RenderUtil.drawString(label, drawX, y, color, shadow.getValue());
		}
		catch (Throwable t)
		{
			// If audio stack is unavailable (e.g., Lavaplayer missing), silently skip music info
		}
	}

	private String formatTime(long ms)
	{
		long totalSeconds = Math.max(0L, ms / 1000L);
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return String.format("%d:%02d", minutes, seconds);
	}

	/**
	 * Draws a gaussian blur only within the specified region using scissor test.
	 * Falls back gracefully if shaders are unavailable.
	 */
	private void drawBlurRegion(int x, int y, int width, int height, int radius)
	{
		if (radius <= 0) return;

		net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getMinecraft();
		if (minecraft == null) return;

		ScaledResolution sr = new ScaledResolution(minecraft);
		int scale = sr.getScaleFactor();

		// Convert to framebuffer pixels and flip Y for scissor
		int scissorX = x * scale;
		int scissorY = minecraft.displayHeight - (y + height) * scale;
		int scissorW = Math.max(0, width * scale);
		int scissorH = Math.max(0, height * scale);

		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

		// Use BlurUtil to apply gaussian blur; scissor confines to our rect
		BlurUtil.applyBlur(Math.min(10.0f, Math.max(0.5f, radius / 3.0f)));

		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		GL11.glPopAttrib();
	}
}


