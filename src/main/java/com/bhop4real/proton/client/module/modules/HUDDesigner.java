package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import net.minecraft.client.Minecraft;

public class HUDDesigner extends Module
{
	public HUDDesigner()
	{
		super("HUDDesigner", new String[]{"hudeditor", "hud"}, "Open the HUD designer", Category.RENDER, 0);
	}

	@Override
	public void onEnable()
	{
		if (mc.thePlayer != null && mc.theWorld != null)
		{
			Minecraft.getMinecraft().displayGuiScreen(new com.bhop4real.proton.client.gui.GuiHudDesigner());
		}
		// Do not remain enabled
		super.setEnabled(false, false);
	}
}


