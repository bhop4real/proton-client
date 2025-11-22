package com.bhop4real.proton.client.gui;

import com.bhop4real.proton.client.hud.HudLayoutManager;
import com.bhop4real.proton.client.util.font.FontUtil;
import com.bhop4real.proton.client.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

/**
 * Simple HUD designer allowing drag-and-drop placement of HUD components.
 * Persists into hud_layout.json when closed.
 */
public class GuiHudDesigner extends GuiScreen
{
	private static final int HANDLE_WIDTH = 110;
	private static final int HANDLE_HEIGHT = 16;
	private static final int SNAP_THRESHOLD = 8;
	private static final float SMOOTHING = 0.25F; // 0..1, higher = faster

	private enum Handle
	{
		WATERMARK, ARRAYLIST, COORDINATES, MUSIC
	}

	private Handle dragging = null;
	private int dragOffsetX, dragOffsetY;
	private HudLayoutManager.Layout layout;
	// Smoothed presentation positions
	private float waterX, waterY, arrX, arrY, coordX, coordY, musicX, musicY;

	@Override
	public void initGui()
	{
		this.layout = HudLayoutManager.get();
		this.dragging = null;
		// Initialize smoothed positions
		ScaledResolution sr = new ScaledResolution(mc);
		waterX = layout.watermarkX;
		waterY = layout.watermarkY;
		arrX = sr.getScaledWidth() - layout.arrayListRightMargin - HANDLE_WIDTH;
		arrY = layout.arrayListTop;
		coordX = layout.coordinatesX;
		coordY = sr.getScaledHeight() - layout.coordinatesBottomOffset - HANDLE_HEIGHT;
		musicX = layout.musicX;
		musicY = layout.musicY;
	}

	@Override
	public void onGuiClosed()
	{
		HudLayoutManager.save(layout);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton != 0) return;
		Handle handle = hitTest(mouseX, mouseY);
		if (handle != null)
		{
			dragging = handle;
			int hx = getHandleX(handle);
			int hy = getHandleY(handle);
			dragOffsetX = mouseX - hx;
			dragOffsetY = mouseY - hy;
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state)
	{
		dragging = null;
	}

	@Override
	public void updateScreen()
	{
		if (dragging != null && Mouse.isButtonDown(0))
		{
			int mx = Mouse.getX() * this.width / this.mc.displayWidth;
			int my = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
			int nx = mx - dragOffsetX;
			int ny = my - dragOffsetY;
			applyDrag(dragging, nx, ny);
		}
		// Smoothly move presentation handles towards logical positions
		waterX = smooth(waterX, getHandleX(Handle.WATERMARK));
		waterY = smooth(waterY, getHandleY(Handle.WATERMARK));
		arrX = smooth(arrX, getHandleX(Handle.ARRAYLIST));
		arrY = smooth(arrY, getHandleY(Handle.ARRAYLIST));
		coordX = smooth(coordX, getHandleX(Handle.COORDINATES));
		coordY = smooth(coordY, getHandleY(Handle.COORDINATES));
		musicX = smooth(musicX, getHandleX(Handle.MUSIC));
		musicY = smooth(musicY, getHandleY(Handle.MUSIC));
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		drawDefaultBackground();
		ScaledResolution sr = new ScaledResolution(mc);

		// Grid
		int gridColor = 0x22111111;
		for (int x = 0; x < sr.getScaledWidth(); x += 10)
		{
			RenderUtil.drawVerticalLine(x, 0, sr.getScaledHeight(), gridColor);
		}
		for (int y = 0; y < sr.getScaledHeight(); y += 10)
		{
			RenderUtil.drawHorizontalLine(0, sr.getScaledWidth(), y, gridColor);
		}

		// Draw handles
		drawHandle(Handle.WATERMARK, "Watermark");
		drawHandle(Handle.ARRAYLIST, "ArrayList (top-right)");
		drawHandle(Handle.COORDINATES, "Coordinates (bottom-left)");
		drawHandle(Handle.MUSIC, "Music Info");

		int hintColor = 0x7FAAAAAA; // alpha 127
		int lineHeight = FontUtil.getFontHeight() + 2;
		// Snap disable hint
		FontUtil.drawString("Hold Ctrl to disable snap", 6, sr.getScaledHeight() - lineHeight * 2 - 4, hintColor, true);
		// Existing ESC hint
		FontUtil.drawString("ESC to save & close", 6, sr.getScaledHeight() - lineHeight - 4, hintColor, true);
	}

	private void drawHandle(Handle type, String label)
	{
		int x = getHandleXSmoothed(type);
		int y = getHandleYSmoothed(type);
		int bg = 0xCC222A36;
		int outline = 0x88556677;
		RenderUtil.drawBorderedRect(x, y, HANDLE_WIDTH, HANDLE_HEIGHT, 1.0F, outline, bg);
		FontUtil.drawString(label, x + 4, y + 3, 0xFFE6EEF9, true);
	}

	private Handle hitTest(int x, int y)
	{
		for (Handle h : Handle.values())
		{
			int hx = getHandleX(h);
			int hy = getHandleY(h);
			if (x >= hx && x <= hx + HANDLE_WIDTH && y >= hy && y <= hy + HANDLE_HEIGHT)
			{
				return h;
			}
		}
		return null;
	}

	private int getHandleX(Handle handle)
	{
		ScaledResolution sr = new ScaledResolution(mc);
		switch (handle)
		{
			case WATERMARK: return layout.watermarkX;
			case ARRAYLIST: return sr.getScaledWidth() - layout.arrayListRightMargin - HANDLE_WIDTH;
			case COORDINATES: return layout.coordinatesX;
			case MUSIC: return layout.musicX;
		}
		return 0;
	}

	private int getHandleY(Handle handle)
	{
		ScaledResolution sr = new ScaledResolution(mc);
		switch (handle)
		{
			case WATERMARK: return layout.watermarkY;
			case ARRAYLIST: return layout.arrayListTop;
			case COORDINATES: return sr.getScaledHeight() - layout.coordinatesBottomOffset - HANDLE_HEIGHT;
			case MUSIC: return layout.musicY;
		}
		return 0;
	}

	private int getHandleXSmoothed(Handle handle)
	{
		switch (handle)
		{
			case WATERMARK: return Math.round(waterX);
			case ARRAYLIST: return Math.round(arrX);
			case COORDINATES: return Math.round(coordX);
			case MUSIC: return Math.round(musicX);
		}
		return 0;
	}

	private int getHandleYSmoothed(Handle handle)
	{
		switch (handle)
		{
			case WATERMARK: return Math.round(waterY);
			case ARRAYLIST: return Math.round(arrY);
			case COORDINATES: return Math.round(coordY);
			case MUSIC: return Math.round(musicY);
		}
		return 0;
	}

	private void applyDrag(Handle handle, int x, int y)
	{
		ScaledResolution sr = new ScaledResolution(mc);
		x = Math.max(0, Math.min(sr.getScaledWidth() - HANDLE_WIDTH, x));
		y = Math.max(0, Math.min(sr.getScaledHeight() - HANDLE_HEIGHT, y));
		boolean disableSnap = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
		switch (handle)
		{
			case WATERMARK:
				layout.watermarkX = disableSnap ? x : snapX(x, sr);
				layout.watermarkY = disableSnap ? y : snapY(y, sr);
				break;
			case ARRAYLIST:
				layout.arrayListTop = disableSnap ? y : snapY(y, sr);
				int right = Math.max(0, sr.getScaledWidth() - x - HANDLE_WIDTH);
				layout.arrayListRightMargin = disableSnap ? right : snapRightMargin(right, sr);
				break;
			case COORDINATES:
				layout.coordinatesX = disableSnap ? x : snapX(x, sr);
				int bottomOffset = Math.max(0, sr.getScaledHeight() - y - HANDLE_HEIGHT);
				layout.coordinatesBottomOffset = disableSnap ? bottomOffset : snapBottomOffset(bottomOffset, sr);
				break;
			case MUSIC:
				layout.musicX = disableSnap ? x : snapX(x, sr);
				layout.musicY = disableSnap ? y : snapY(y, sr);
				break;
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode)
	{
		if (keyCode == 1) // ESC
		{
			mc.displayGuiScreen(null);
		}
	}

	private int snapX(int x, ScaledResolution sr)
	{
		// Snap to left or right edges
		if (Math.abs(x - 4) <= SNAP_THRESHOLD) return 4;
		int rightX = sr.getScaledWidth() - HANDLE_WIDTH - 4;
		if (Math.abs(x - rightX) <= SNAP_THRESHOLD) return rightX;
		return x;
	}

	private int snapY(int y, ScaledResolution sr)
	{
		// Snap to top or bottom edges
		if (Math.abs(y - 4) <= SNAP_THRESHOLD) return 4;
		int bottomY = sr.getScaledHeight() - HANDLE_HEIGHT - 4;
		if (Math.abs(y - bottomY) <= SNAP_THRESHOLD) return bottomY;
		return y;
	}

	private int snapRightMargin(int rightMargin, ScaledResolution sr)
	{
		// Snap to small margins
		if (Math.abs(rightMargin - 4) <= SNAP_THRESHOLD) return 4;
		return rightMargin;
	}

	private int snapBottomOffset(int bottomOffset, ScaledResolution sr)
	{
		// Snap to small offsets
		if (Math.abs(bottomOffset - 4) <= SNAP_THRESHOLD) return 4;
		return bottomOffset;
	}

	private float smooth(float current, int target)
	{
		return current + (target - current) * SMOOTHING;
	}
}


