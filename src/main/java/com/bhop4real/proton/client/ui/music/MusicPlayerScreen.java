package com.bhop4real.proton.client.ui.music;

import com.bhop4real.proton.client.gui.config.ClickGuiPreferences;
import com.bhop4real.proton.client.gui.theme.ClickGuiTheme;
import com.bhop4real.proton.client.gui.theme.ClickGuiThemeInterface;
import com.bhop4real.proton.client.gui.util.GuiAnimation;
import com.bhop4real.proton.client.gui.util.GuiComponent;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.music.LocalLibraryScanner;
import com.bhop4real.proton.client.music.MusicService;
import com.bhop4real.proton.client.music.PlayOrder;
import com.bhop4real.proton.client.util.animation.Easing;
import com.bhop4real.proton.client.util.font.FontUtil;
import com.bhop4real.proton.client.util.render.RenderUtil;
import com.bhop4real.proton.client.util.render.blur.GaussianBlur;
import com.bhop4real.proton.client.util.render.blur.KawaseBlur;
import com.bhop4real.proton.client.util.render.rrect.RRectUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerScreen extends GuiScreen {
	private static boolean resourcesPreloaded;

	private static final int GUI_WIDTH = 520;
	private static final int GUI_HEIGHT = 320;
	private static final int HEADER_HEIGHT = 28;
	private static final int PANEL_PADDING = 8;
	private static final int PANEL_RADIUS = 8;
	private static final int ITEM_HEIGHT = 16;

	private int guiLeft;
	private int guiTop;

	private GuiAnimation fadeAnimation;
	private GuiAnimation zoomAnimation;
	private ClickGuiThemeInterface theme = ClickGuiTheme.MIDNIGHT;

	private List<Path> library = new ArrayList<Path>();
	private int selectedIndex = -1;
	private int listScroll = 0;
	private int volumeKnobDragging = -1; // -1 none, 0 dragging volume

	public static void preloadResources() {
		if (resourcesPreloaded) return;
		resourcesPreloaded = true;
		try {
			FontUtil.getFontHeight();
			GaussianBlur.preload();
			KawaseBlur.preload();
		} catch (Exception ignored) {
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		updateGuiPositions();

		this.fadeAnimation = new GuiAnimation(Easing.EASE_OUT_EXPO, 280L);
		this.fadeAnimation.setStartValue(0.0);
		this.fadeAnimation.animateTo(1.0);

		this.zoomAnimation = new GuiAnimation(Easing.EASE_OUT_EXPO, 280L);
		this.zoomAnimation.setStartValue(0.85);
		this.zoomAnimation.animateTo(1.0);

		if (library.isEmpty()) {
			final File mcDir = Minecraft.getMinecraft().mcDataDir;
			final File root = new File(mcDir, "proton/music");
			final LocalLibraryScanner scanner = new LocalLibraryScanner();
			library = scanner.scan(root.toPath());
			if (!library.isEmpty()) {
				selectedIndex = 0;
				MusicService.get().loadLocal(library);
			}
		}
	}

	private void updateGuiPositions() {
		int screenWidth = this.width;
		int screenHeight = this.height;
		if (screenWidth <= 0 || screenHeight <= 0) {
			ScaledResolution sr = new ScaledResolution(mc);
			screenWidth = sr.getScaledWidth();
			screenHeight = sr.getScaledHeight();
		}
		this.guiLeft = (screenWidth - GUI_WIDTH) / 2;
		this.guiTop = (screenHeight - GUI_HEIGHT) / 2;
		if (this.guiLeft < 0) this.guiLeft = 0;
		if (this.guiTop < 0) this.guiTop = 0;
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
		volumeKnobDragging = -1;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			this.mc.displayGuiScreen(null);
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		// Bottom transport controls
		int controlsTop = guiTop + GUI_HEIGHT - PANEL_PADDING - 36;
		int controlsLeft = guiLeft + PANEL_PADDING + 10;
		if (clickButton(mouseX, mouseY, controlsLeft, controlsTop, 40, 16)) {
			if (selectedIndex >= 0) MusicService.get().playIndex(selectedIndex); else MusicService.get().play();
			return;
		}
		if (clickButton(mouseX, mouseY, controlsLeft + 44, controlsTop, 40, 16)) {
			MusicService.get().pause();
			return;
		}
		if (clickButton(mouseX, mouseY, controlsLeft + 88, controlsTop, 40, 16)) {
			MusicService.get().stop();
			return;
		}
		if (clickButton(mouseX, mouseY, controlsLeft + 132, controlsTop, 36, 16)) {
			MusicService.get().previous();
			return;
		}
		if (clickButton(mouseX, mouseY, controlsLeft + 172, controlsTop, 36, 16)) {
			MusicService.get().next();
			return;
		}

		// Order toggle
		int orderLeft = guiLeft + GUI_WIDTH - PANEL_PADDING - 120;
		if (clickButton(mouseX, mouseY, orderLeft, controlsTop, 110, 16)) {
			PlayOrder now = MusicService.get().getOrder();
			MusicService.get().setOrder(nextOrder(now));
			return;
		}

		// Volume slider
		int sliderLeft = orderLeft;
		int sliderTop = controlsTop - 18;
		int sliderWidth = 110;
		if (mouseX >= sliderLeft && mouseX <= sliderLeft + sliderWidth &&
				mouseY >= sliderTop && mouseY <= sliderTop + 8) {
			volumeKnobDragging = 0;
			updateVolumeFromMouse(mouseX, sliderLeft, sliderWidth);
			return;
		}

		// Track list click
		int listLeft = guiLeft + PANEL_PADDING + 8;
		int listRight = guiLeft + GUI_WIDTH - PANEL_PADDING - 8;
		int listTop = guiTop + HEADER_HEIGHT + PANEL_PADDING + 8;
		int listBottom = guiTop + GUI_HEIGHT - PANEL_PADDING - 44;
		if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
			int y = listTop - listScroll;
			for (int i = 0; i < library.size(); i++) {
				int rowTop = y;
				int rowBottom = rowTop + ITEM_HEIGHT + 2;
				if (mouseY >= rowTop && mouseY < rowBottom) {
					selectedIndex = i;
					break;
				}
				y += ITEM_HEIGHT + 2;
			}
		}
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
		if (volumeKnobDragging == 0 && clickedMouseButton == 0) {
			int orderLeft = guiLeft + GUI_WIDTH - PANEL_PADDING - 120;
			updateVolumeFromMouse(mouseX, orderLeft, 110);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		volumeKnobDragging = -1;
	}

	private void updateVolumeFromMouse(int mouseX, int sliderLeft, int sliderWidth) {
		float ratio = (float)(mouseX - sliderLeft) / (float)sliderWidth;
		int vol = Math.max(0, Math.min(100, Math.round(ratio * 100.0f)));
		MusicService.get().setVolume(vol);
	}

	private boolean clickButton(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}

	private PlayOrder nextOrder(PlayOrder o) {
		if (o == PlayOrder.DEFAULT) return PlayOrder.REVERSE;
		if (o == PlayOrder.REVERSE) return PlayOrder.RANDOM;
		return PlayOrder.DEFAULT;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		preloadResources();
		updateGuiPositions();

		// Track active Click GUI preferences/theme
		this.theme = resolveTheme();

		// Animations
		if (fadeAnimation != null) fadeAnimation.animateTo(1.0);
		if (zoomAnimation != null) zoomAnimation.animateTo(1.0);

		// Blur honoring Click GUI preferences
		ClickGuiPreferences prefs = resolvePreferences();
		if (prefs.blurEnabled() && prefs.blurRadius() > 0) {
			switch (prefs.blurMode()) {
				case KAWASE:
					KawaseBlur.startBlur();
					RenderUtil.drawRect(0, 0, this.width, this.height, 0xFFFFFFFF);
					int iterations = Math.max(1, prefs.blurRadius() / 4);
					float offset = 0.75F + prefs.blurRadius() * 0.125F;
					KawaseBlur.endBlur(iterations, offset);
					break;
				case GAUSSIAN:
				default:
					GaussianBlur.startBlur();
					RenderUtil.drawRect(0, 0, this.width, this.height, 0xFFFFFFFF);
					GaussianBlur.endBlur(Math.min(prefs.blurRadius(), 32), 1.0f);
					break;
			}
		}

		// Overlay
		int overlay = applyAlpha(theme.overlay(), prefs.backgroundBrightness());
		RenderUtil.drawRect(0, 0, this.width, this.height, overlay);

		// Apply zoom transform
		if (zoomAnimation != null) {
			zoomAnimation.setDuration(prefs.openAnimationDuration());
		}
		if (fadeAnimation != null) {
			fadeAnimation.setDuration(prefs.openAnimationDuration());
		}
		float zoom = zoomAnimation != null ? (float) zoomAnimation.getValue() : 1.0F;
		int centerX = guiLeft + GUI_WIDTH / 2;
		int centerY = guiTop + GUI_HEIGHT / 2;
		GL11.glPushMatrix();
		GL11.glTranslatef(centerX, centerY, 0);
		GL11.glScalef(zoom, zoom, 1.0F);
		GL11.glTranslatef(-centerX, -centerY, 0);

		// Draw container
		drawContainer(mouseX, mouseY);

		GL11.glPopMatrix();

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawContainer(int mouseX, int mouseY) {
		int frameLeft = guiLeft;
		int frameTop = guiTop;
		int frameRight = guiLeft + GUI_WIDTH;

		RRectUtils.drawRoundOutline(frameLeft, frameTop, GUI_WIDTH, GUI_HEIGHT, PANEL_RADIUS,
				1.5F, new java.awt.Color(theme.container(), true), new java.awt.Color(applyAlpha(theme.outline(), 160), true));

		drawHeader(frameLeft, frameTop, frameRight);
		drawTrackList(mouseX, mouseY);
		drawControls(mouseX, mouseY);
	}

	private void drawHeader(int frameLeft, int frameTop, int frameRight) {
		int headerLeft = frameLeft + PANEL_PADDING;
		int headerRight = frameRight - PANEL_PADDING;
		int headerTop = frameTop + PANEL_PADDING / 2;
		int headerHeight = HEADER_HEIGHT - PANEL_PADDING / 2;

		RRectUtils.drawRoundOutline(headerLeft, headerTop, headerRight - headerLeft, headerHeight,
				PANEL_RADIUS, 1.0F, new java.awt.Color(theme.header(), true), new java.awt.Color(applyAlpha(theme.outline(), 120), true));

		String title = "Proton Music Player";
		FontUtil.drawString(title, headerLeft + 12, headerTop + 5, theme.textPrimary(), true);

		String order = "Order: " + MusicService.get().getOrder().name();
		int orderWidth = FontUtil.getStringWidth(order);
		FontUtil.drawString(order, headerRight - orderWidth - 12, headerTop + 5, theme.textSecondary(), true);
	}

	private void drawTrackList(int mouseX, int mouseY) {
		int listLeft = guiLeft + PANEL_PADDING + 8;
		int listRight = guiLeft + GUI_WIDTH - PANEL_PADDING - 8;
		int listTop = guiTop + HEADER_HEIGHT + PANEL_PADDING + 8;
		int listBottom = guiTop + GUI_HEIGHT - PANEL_PADDING - 44;

		RRectUtils.drawRoundOutline(listLeft - 6, listTop - 6, (listRight - listLeft) + 12, (listBottom - listTop) + 12,
				PANEL_RADIUS, 1.0F, new java.awt.Color(theme.moduleBackground(), true), new java.awt.Color(applyAlpha(theme.outline(), 120), true));

		int clipLeft = Math.max(listLeft, guiLeft);
		int clipRight = Math.min(listRight, guiLeft + GUI_WIDTH);
		int clipTop = Math.max(listTop, guiTop);
		int clipBottom = Math.min(listBottom, guiTop + GUI_HEIGHT);
		boolean clip = pushScissor(clipLeft, clipTop, clipRight - clipLeft, clipBottom - clipTop);

		int y = listTop - listScroll;
		if (library.isEmpty()) {
			String hint = "Place files in .minecraft/proton/music";
			int w = FontUtil.getStringWidth(hint);
			FontUtil.drawString(hint, (listLeft + listRight - w) / 2.0F, listTop + 10, theme.textSecondary(), true);
		} else {
			for (int i = 0; i < library.size(); i++) {
				Path p = library.get(i);
				String name = p.getFileName().toString();
				int rowTop = y;
				int rowBottom = y + ITEM_HEIGHT + 2;
				if (rowBottom > listTop && rowTop < listBottom) {
					boolean hovered = mouseX >= listLeft && mouseX <= listRight && mouseY >= rowTop && mouseY < rowBottom;
					int base = (i == selectedIndex) ? applyAlpha(theme.categorySelected(), 220)
							: (hovered ? applyAlpha(theme.moduleHover(), 160) : 0);
					if (base != 0) {
						RenderUtil.drawRect(listLeft, rowTop, listRight, rowBottom, base);
					}
					// Truncate text to fit within panel
					int maxWidth = (listRight - listLeft) - 10;
					if (maxWidth > 10) {
						int textWidth = FontUtil.getStringWidth(name);
						if (textWidth > maxWidth) {
							String ellipsis = "...";
							int ellipsisWidth = FontUtil.getStringWidth(ellipsis);
							int allowed = maxWidth - ellipsisWidth;
							if (allowed > 0) {
								while (name.length() > 0 && FontUtil.getStringWidth(name) > allowed) {
									name = name.substring(0, name.length() - 1);
								}
								name = name + ellipsis;
							}
						}
					}
					FontUtil.drawString(name, listLeft + 4, rowTop + 3, theme.textPrimary(), true);
				}
				y += ITEM_HEIGHT + 2;
			}
		}

		if (clip) popScissor();

		// Scroll
		int wheel = Mouse.getDWheel();
		if (wheel != 0 && mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
			int totalHeight = (ITEM_HEIGHT + 2) * Math.max(0, library.size());
			int visibleHeight = listBottom - listTop;
			int maxScroll = Math.max(0, totalHeight - visibleHeight);
			listScroll = Math.max(0, Math.min(listScroll + (wheel > 0 ? -12 : 12), maxScroll));
		}
	}

	private void drawControls(int mouseX, int mouseY) {
		int controlsTop = guiTop + GUI_HEIGHT - PANEL_PADDING - 36;
		int controlsLeft = guiLeft + PANEL_PADDING + 10;

		drawButton(controlsLeft, controlsTop, 40, 16, "Play", mouseX, mouseY);
		drawButton(controlsLeft + 44, controlsTop, 40, 16, "Pause", mouseX, mouseY);
		drawButton(controlsLeft + 88, controlsTop, 40, 16, "Stop", mouseX, mouseY);
		drawButton(controlsLeft + 132, controlsTop, 36, 16, "<<", mouseX, mouseY);
		drawButton(controlsLeft + 172, controlsTop, 36, 16, ">>", mouseX, mouseY);

		// Volume
		int sliderLeft = guiLeft + GUI_WIDTH - PANEL_PADDING - 120;
		int sliderTop = controlsTop - 18;
		int sliderWidth = 110;
		int sliderHeight = 8;
		int vol = MusicService.get().getVolume();
		float progress = Math.max(0.0F, Math.min(1.0F, vol / 100.0F));

		// Click GUI-styled slider
		boolean sliderHovered = mouseX >= sliderLeft && mouseX <= sliderLeft + sliderWidth &&
				mouseY >= sliderTop && mouseY <= sliderTop + sliderHeight;
		boolean sliderDragging = volumeKnobDragging == 0;
		GuiComponent.drawSlider(sliderLeft, sliderTop, sliderWidth, sliderHeight, progress, sliderHovered, sliderDragging, theme);

		// Volume text (keep inside right bound)
		String volText = "Vol " + vol + "%";
		int volTextX = sliderLeft + sliderWidth + 6;
		if (volTextX + FontUtil.getStringWidth(volText) > guiLeft + GUI_WIDTH - PANEL_PADDING) {
			volTextX = guiLeft + GUI_WIDTH - PANEL_PADDING - FontUtil.getStringWidth(volText);
		}
		FontUtil.drawString(volText, volTextX, sliderTop - 2, theme.textSecondary(), true);

		// Order toggle
		String order = I18n.format("Order: " + MusicService.get().getOrder().name());
		// Keep button within right bound
		int orderWidth = Math.min(sliderWidth, Math.max(60, FontUtil.getStringWidth(order) + 14));
		drawButton(sliderLeft, controlsTop, orderWidth, 16, order, mouseX, mouseY);
	}

	private void drawButton(int x, int y, int w, int h, String label, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
		int base = hovered ? applyAlpha(theme.moduleHover(), 200) : applyAlpha(theme.settingBackground(), 220);
		int outline = applyAlpha(theme.outline(), hovered ? 200 : 160);
		RRectUtils.drawRoundOutline(x, y, w, h, 4, 1.0F, new java.awt.Color(base, true), new java.awt.Color(outline, true));
		int textColor = theme.textPrimary();
		int textWidth = FontUtil.getStringWidth(label);
		FontUtil.drawString(label, x + (w - textWidth) / 2.0F, y + 4, textColor, true);
	}

	private static int applyAlpha(int color, int alpha) {
		return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
	}

	private ClickGuiPreferences resolvePreferences() {
		try {
			com.bhop4real.proton.client.module.ModuleManager mm = ProtonClient.getInstance().getModuleManager();
			if (mm != null) {
				Module m = mm.getModuleByName("ClickGUI");
				if (m instanceof com.bhop4real.proton.client.module.modules.ClickGUI) {
					return ((com.bhop4real.proton.client.module.modules.ClickGUI) m).snapshotPreferences();
				}
			}
		} catch (Throwable ignored) {
		}
		return ClickGuiPreferences.defaults();
	}

	private ClickGuiThemeInterface resolveTheme() {
		ClickGuiPreferences prefs = resolvePreferences();
		return prefs.theme() != null ? prefs.theme() : ClickGuiTheme.MIDNIGHT;
	}

	private boolean pushScissor(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) return false;
		ScaledResolution sr = new ScaledResolution(mc);
		int scale = sr.getScaleFactor();
		int scissorX = x * scale;
		int scissorY = mc.displayHeight - (y + height) * scale;
		int scissorWidth = width * scale;
		int scissorHeight = height * scale;
		if (scissorWidth <= 0 || scissorHeight <= 0) return false;
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
		return true;
	}

	private void popScissor() {
		if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}


