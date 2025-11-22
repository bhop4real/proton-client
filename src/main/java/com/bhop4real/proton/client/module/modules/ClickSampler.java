package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.gui.GuiClickSampler;
import com.bhop4real.proton.client.learning.click.ClickSample;
import com.bhop4real.proton.client.learning.click.ClickSampleRecorder;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * Module that opens the click sampling GUI for recording human click data.
 */
public class ClickSampler extends Module
{
    private final EnumSetting mode;
    private final ClickSampleRecorder recorder = new ClickSampleRecorder();

    public ClickSampler()
    {
        super("Sampler", new String[]{"sample"}, "Record input data for machine learning features.", Category.MISC, 0);
        mode = new EnumSetting("Mode", "Sampling target", "Aim", "Aim");
        addSetting(mode);
    }

    @Override
    public void onEnable()
    {
        String currentMode = mode.getValue();
        if (!"Aim".equalsIgnoreCase(currentMode))
        {
            sendClientMessage(EnumChatFormatting.RED + "Unsupported sampler mode: " + currentMode);
            setEnabled(false);
            return;
        }

        sendClientMessage(EnumChatFormatting.YELLOW + "Aim sampling is a placeholder and not available yet.");
        setEnabled(false, false);
    }

    @Override
    public void onDisable()
    {
        recorder.shutdown();
        if (mc != null && mc.currentScreen instanceof GuiClickSampler)
        {
            mc.displayGuiScreen(null);
        }
    }

    public void onSamplingClosed(boolean saved, int capturedClicks)
    {
        if (saved)
        {
            sendClientMessage(EnumChatFormatting.GREEN + "Saved click sample with " + capturedClicks + " clicks.");
        }
        else if (capturedClicks > 0)
        {
            sendClientMessage(EnumChatFormatting.YELLOW + "Discarded click sample.");
        }

        if (isEnabled())
        {
            setEnabled(false, false);
        }
    }

    public boolean startRecording()
    {
        boolean started = recorder.start();
        if (!started)
        {
            sendClientMessage(EnumChatFormatting.RED + "Unable to start click recorder. Check logs.");
        }
        return started;
    }

    public void stopRecording()
    {
        recorder.stop();
    }

    public ClickSample saveRecording()
    {
        recorder.stop();
        return recorder.buildSampleAndReset();
    }

    public void discardRecording()
    {
        recorder.stop();
        recorder.reset();
    }

    private void sendClientMessage(String text)
    {
        if (mc != null && mc.thePlayer != null)
        {
            mc.thePlayer.addChatMessage(new ChatComponentText(text));
        }
    }
}

