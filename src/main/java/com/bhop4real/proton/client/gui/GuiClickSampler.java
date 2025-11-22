package com.bhop4real.proton.client.gui;

import com.bhop4real.proton.client.learning.click.ClickMLService;
import com.bhop4real.proton.client.learning.click.ClickSample;
import com.bhop4real.proton.client.learning.click.ClickSampleRecorder;
import com.bhop4real.proton.client.module.modules.ClickSampler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * Lightweight GUI for capturing human click samples.
 */
public class GuiClickSampler extends GuiScreen
{
    private final ClickSampler owner;
    private final ClickSampleRecorder recorder;

    private GuiButton toggleButton;
    private GuiButton saveButton;
    private GuiButton cancelButton;

    private boolean recording;
    private boolean notifiedOwner;

    public GuiClickSampler(ClickSampler owner, ClickSampleRecorder recorder)
    {
        this.owner = owner;
        this.recorder = recorder;
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        int centerX = width / 2;
        int baseY = height - 80;

        toggleButton = new GuiButton(0, centerX - 100, baseY, 200, 20, getToggleLabel());
        saveButton = new GuiButton(1, centerX - 100, baseY + 25, 200, 20, "Save & Close");
        cancelButton = new GuiButton(2, centerX - 100, baseY + 50, 200, 20, "Cancel");

        buttonList.add(toggleButton);
        buttonList.add(saveButton);
        buttonList.add(cancelButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        switch (button.id)
        {
            case 0:
                recording = !recording;
                if (recording)
                {
                    if (!owner.startRecording())
                    {
                        recording = false;
                    }
                }
                else
                {
                    owner.stopRecording();
                }
                toggleButton.displayString = getToggleLabel();
                break;
            case 1:
                handleSaveAndClose();
                break;
            case 2:
                close(false, recorder.capturedClicks());
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();

        int centerX = width / 2;
        int top = height / 4;

        String title = EnumChatFormatting.LIGHT_PURPLE + "Click Sampler";
        drawCenteredString(fontRendererObj, title, centerX, top, 0xFFFFFF);

        drawCenteredString(fontRendererObj,
            recording
                ? EnumChatFormatting.GREEN + "Recording... left click in rhythm to capture your pattern."
                : EnumChatFormatting.GRAY + "Press Start Recording, then left click like you normally do.",
            centerX,
            top + 20,
            0xFFFFFF);

        List<Long> intervals = recorder.getIntervalsSnapshot();
        List<Long> holds = recorder.getHoldDurationsSnapshot();

        drawCenteredString(fontRendererObj,
            EnumChatFormatting.AQUA + "Captured intervals: " + intervals.size(),
            centerX,
            top + 40,
            0xFFFFFF);

        drawCenteredString(fontRendererObj,
            EnumChatFormatting.AQUA + "Captured hold durations: " + holds.size(),
            centerX,
            top + 55,
            0xFFFFFF);

        if (!intervals.isEmpty())
        {
            long last = intervals.get(intervals.size() - 1);
            drawCenteredString(fontRendererObj,
                EnumChatFormatting.YELLOW + "Last interval: " + last + " ms",
                centerX,
                top + 70,
                0xFFFFFF);
        }

        if (!holds.isEmpty())
        {
            long lastHold = holds.get(holds.size() - 1);
            drawCenteredString(fontRendererObj,
                EnumChatFormatting.YELLOW + "Last hold: " + lastHold + " ms",
                centerX,
                top + 85,
                0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        recorder.tick(Mouse.isButtonDown(0), System.currentTimeMillis());
    }

    @Override
    public void onGuiClosed()
    {
        if (recording)
        {
            owner.stopRecording();
            recording = false;
        }
        if (!notifiedOwner)
        {
            close(false, recorder.capturedClicks());
        }
    }

    private String getToggleLabel()
    {
        return recording ? "Stop Recording" : "Start Recording";
    }

    private void handleSaveAndClose()
    {
        ClickSample sample = owner.saveRecording();
        recording = false;
        if (sample != null)
        {
            ClickMLService.get().addSample(sample);
            close(true, sample.getHoldDurations().size());
        }
        else
        {
            close(false, recorder.capturedClicks());
        }
    }

    private void close(boolean saved, int capturedClicks)
    {
        notifiedOwner = true;
        recording = false;
        mc.displayGuiScreen(null);
        if (!saved)
        {
            owner.discardRecording();
        }
        if (owner != null)
        {
            owner.onSamplingClosed(saved, capturedClicks);
        }
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}

