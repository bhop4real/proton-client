package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.learning.click.ClickMLService;
import com.bhop4real.proton.client.learning.click.ClickTrainingResult;
import com.bhop4real.proton.client.learning.click.ClickTrainingSummary;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * Module responsible for training machine learning models based on collected samples.
 */
public class ClickModelTrainerModule extends Module
{
    private final EnumSetting mode;
    private final BooleanSetting clearSamples;

    public ClickModelTrainerModule()
    {
        super("Models", new String[]{"mlmodel"}, "Train ML models from recorded samples.", Category.MISC, 0);
        mode = new EnumSetting("Mode", "Model type", "Click", "Click");
        clearSamples = new BooleanSetting("Clear Samples", "Delete samples after training completes", false);

        addSetting(mode);
        addSetting(clearSamples);
    }

    @Override
    public void onEnable()
    {
        if (!"Click".equalsIgnoreCase(mode.getValue()))
        {
            sendMessage(EnumChatFormatting.RED + "Unsupported model mode: " + mode.getValue());
            setEnabled(false);
            return;
        }

        sendMessage(EnumChatFormatting.AQUA + "Training click model in background...");
        boolean clear = clearSamples.getValue();
        ClickMLService.get().trainAsync(clear).whenComplete((ClickTrainingResult result, Throwable throwable) -> {
            if (mc == null)
            {
                return;
            }
            mc.addScheduledTask(() -> {
                if (throwable != null)
                {
                    sendCompletionMessage(EnumChatFormatting.RED + "Click model training failed: " + throwable.getMessage());
                }
                else if (result == null)
                {
                    sendCompletionMessage(EnumChatFormatting.RED + "Click model training failed: no result returned.");
                }
                else
                {
                    if (!result.isSuccess())
                    {
                        sendCompletionMessage(EnumChatFormatting.RED + result.getMessage());
                        return;
                    }

                    ClickTrainingSummary summary = result.getSummary();
                    StringBuilder builder = new StringBuilder();
                    builder.append(EnumChatFormatting.GREEN)
                        .append("Click model trained: ")
                        .append(result.formatSuccessDetails());
                    if (summary != null && summary.getIntervalCount() == 0)
                    {
                        builder.append(EnumChatFormatting.YELLOW)
                            .append(" (training produced no usable intervals)");
                    }
                    if (result.didClearSamples())
                    {
                        builder.append(EnumChatFormatting.DARK_GRAY)
                            .append(" Samples cleared.");
                    }
                    sendCompletionMessage(builder.toString());
                }
            });
        });

        setEnabled(false, false);
    }

    private void sendMessage(String text)
    {
        if (mc != null && mc.thePlayer != null)
        {
            mc.thePlayer.addChatMessage(new ChatComponentText(text));
        }
    }

    private void sendCompletionMessage(String text)
    {
        if (mc != null && mc.thePlayer != null)
        {
            String mention = mc.thePlayer.getDisplayNameString();
            mc.thePlayer.addChatMessage(new ChatComponentText(
                text + EnumChatFormatting.GRAY + " @" + mention + EnumChatFormatting.RESET));
        }
        else
        {
            sendMessage(text);
        }
    }
}

