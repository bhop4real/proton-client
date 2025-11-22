package com.bhop4real.proton.client.ui.music;

import com.bhop4real.proton.client.music.LocalLibraryScanner;
import com.bhop4real.proton.client.music.MusicService;
import com.bhop4real.proton.client.music.PlayOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MusicScreen extends GuiScreen {
	private GuiButton playButton;
	private GuiButton pauseButton;
	private GuiButton stopButton;
	private GuiButton prevButton;
	private GuiButton nextButton;
	private GuiButton volDownButton;
	private GuiButton volUpButton;
	private GuiButton orderButton;
	private GuiButton onlineButton;

	private List<Path> library;
	private int selectedIndex = -1;

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		buttonList.clear();
		final int cx = width / 2;
		final int baseY = height / 2 - 10;
		int id = 1;
		prevButton = new GuiButton(id++, cx - 150, baseY, 30, 20, "<<");
		playButton = new GuiButton(id++, cx - 115, baseY, 30, 20, "Play");
		pauseButton = new GuiButton(id++, cx - 80, baseY, 30, 20, "Pause");
		stopButton = new GuiButton(id++, cx - 45, baseY, 30, 20, "Stop");
		nextButton = new GuiButton(id++, cx - 10, baseY, 30, 20, ">>");
		volDownButton = new GuiButton(id++, cx + 30, baseY, 30, 20, "-");
		volUpButton = new GuiButton(id++, cx + 65, baseY, 30, 20, "+");
		orderButton = new GuiButton(id++, cx + 100, baseY, 80, 20, orderLabel());
		onlineButton = new GuiButton(id++, cx - 40, baseY + 26, 160, 20, "Online (Coming soon)");

		buttonList.add(prevButton);
		buttonList.add(playButton);
		buttonList.add(pauseButton);
		buttonList.add(stopButton);
		buttonList.add(nextButton);
		buttonList.add(volDownButton);
		buttonList.add(volUpButton);
		buttonList.add(orderButton);
		buttonList.add(onlineButton);

		if (library == null) {
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

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button == playButton) {
			if (selectedIndex >= 0) {
				MusicService.get().playIndex(selectedIndex);
			} else {
				MusicService.get().play();
			}
		} else if (button == pauseButton) {
			MusicService.get().pause();
		} else if (button == stopButton) {
			MusicService.get().stop();
		} else if (button == nextButton) {
			MusicService.get().next();
		} else if (button == prevButton) {
			MusicService.get().previous();
		} else if (button == volDownButton) {
			MusicService.get().setVolume(MusicService.get().getVolume() - 5);
		} else if (button == volUpButton) {
			MusicService.get().setVolume(MusicService.get().getVolume() + 5);
		} else if (button == orderButton) {
			final PlayOrder now = MusicService.get().getOrder();
			final PlayOrder next = nextOrder(now);
			MusicService.get().setOrder(next);
			orderButton.displayString = orderLabel();
		} else if (button == onlineButton) {
			// Placeholder: do nothing for now
		}
	}

	private PlayOrder nextOrder(final PlayOrder o) {
		if (o == PlayOrder.DEFAULT) return PlayOrder.REVERSE;
		if (o == PlayOrder.REVERSE) return PlayOrder.RANDOM;
		return PlayOrder.DEFAULT;
	}

	private String orderLabel() {
		final PlayOrder order = MusicService.get().getOrder();
		return I18n.format("Order: " + order.name());
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		final String title = "Proton Music Player";
		drawCenteredString(fontRendererObj, title, width / 2, 20, 0xFFFFFF);

		int y = 50;
		if (library != null && !library.isEmpty()) {
			for (int i = 0; i < Math.min(library.size(), 12); i++) {
				final Path p = library.get(i);
				final String name = p.getFileName().toString();
				int color = (i == selectedIndex) ? 0xFFEE66 : 0xDDDDDD;
				drawString(fontRendererObj, (i == selectedIndex ? "> " : "  ") + name, 20, y, color);
				y += 10;
			}
		} else {
			drawCenteredString(fontRendererObj, "Place music files in .minecraft/proton/music", width / 2, y, 0xAAAAAA);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		// Simple hit area for the list
		if (library == null || library.isEmpty()) return;
		int yStart = 50;
		for (int i = 0; i < Math.min(library.size(), 12); i++) {
			int y = yStart + i * 10;
			if (mouseY >= y && mouseY <= y + 10) {
				selectedIndex = i;
				break;
			}
		}
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}
}


