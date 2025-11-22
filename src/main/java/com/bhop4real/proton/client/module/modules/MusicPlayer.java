package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.music.MusicService;
import com.bhop4real.proton.client.music.PlayOrder;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.DoubleSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.SeparatorSetting;
import com.bhop4real.proton.client.ui.music.MusicPlayerScreen;
import net.minecraft.client.Minecraft;

public class MusicPlayer extends Module
{
	private final DoubleSetting volumeSetting;
	private final EnumSetting orderSetting;

	public MusicPlayer()
	{
		super("MusicPlayer", new String[]{"music", "mp"}, "In-game music player", Module.Category.MISC, 0);
		addSetting(new SeparatorSetting("Controls in GUI"));
		volumeSetting = new DoubleSetting("Volume", "Playback volume (0-100)", 50.0D, 0.0D, 100.0D, 1.0D);
		orderSetting = new EnumSetting("Order", "Playback order", "DEFAULT", "DEFAULT", "REVERSE", "RANDOM");
		addSetting(volumeSetting);
		addSetting(orderSetting);
	}

	@Override
	public void onEnable()
	{
		if (mc.thePlayer == null || mc.theWorld == null) return;
		try {
			syncSettingsToService();
			Minecraft.getMinecraft().displayGuiScreen(new MusicPlayerScreen());
		} catch (Throwable ignored) {
			// If music stack is unavailable, fail silently
		}
		// Do not remain enabled – act as a launcher only
		super.setEnabled(false, false);
	}

	@Override
	public void onDisable()
	{
		// Stop playback when module is disabled
		try {
			MusicService.get().stop();
		} catch (Exception ignored) {
		}
		// Close GUI if open
		if (mc.currentScreen instanceof MusicPlayerScreen) {
			mc.displayGuiScreen(null);
		}
	}

	@Override
	public void toggle()
	{
		// Open GUI instead of toggling persistent state
		if (mc.thePlayer != null && mc.theWorld != null)
		{
			try {
				syncSettingsToService();
				Minecraft.getMinecraft().displayGuiScreen(new MusicPlayerScreen());
			} catch (Throwable ignored) {
			}
		}
	}

	@Override
	public void setEnabled(boolean enabled, boolean showFeedback)
	{
		// Prevent enabling; just open GUI on attempts to enable
		if (enabled)
		{
			if (mc.thePlayer != null && mc.theWorld != null)
			{
				try {
					syncSettingsToService();
					Minecraft.getMinecraft().displayGuiScreen(new MusicPlayerScreen());
				} catch (Throwable ignored) {
				}
			}
			return;
		}
		// Allow disable path to stop audio/close GUI
		super.setEnabled(false, showFeedback);
	}

	@Override
	public void onUpdate()
	{
		// Continuously reflect settings to the service to persist user changes
		try {
			syncSettingsToService();
		} catch (Throwable ignored) {
		}
	}

	private void syncSettingsToService() {
		MusicService.get().setVolume((int) Math.round(volumeSetting.getValue()));
		final String mode = orderSetting.getValue();
		try {
			MusicService.get().setOrder(PlayOrder.valueOf(mode));
		} catch (IllegalArgumentException ignored) {
		}
	}
}


