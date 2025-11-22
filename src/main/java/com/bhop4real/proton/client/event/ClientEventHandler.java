package com.bhop4real.proton.client.event;

import com.bhop4real.proton.client.module.ModuleManager;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientEventHandler
{
    private final ModuleManager moduleManager;
    
    public ClientEventHandler(ModuleManager moduleManager)
    {
        this.moduleManager = moduleManager;
    }
    
    @SubscribeEvent
    public void onClientChatReceived(ClientChatReceivedEvent event)
    {
        if (event.message == null)
        {
            return;
        }
        
        String message = event.message.getUnformattedText();
        if (message == null || message.isEmpty())
        {
            return;
        }
        
        // Chat listeners removed - no longer needed
    }
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event)
    {
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT)
        {
            // Render module overlays here
            for (com.bhop4real.proton.client.module.Module module : moduleManager.getModules())
            {
                if (module.isEnabled())
                {
                    module.onRender();
                }
            }
            // Render notifications unless a GUI handles it explicitly
            if (!(net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof com.bhop4real.proton.client.gui.GuiClickGUI))
            {
                com.bhop4real.proton.client.gui.NotificationManager.render(event.partialTicks);
            }
        }
    }
    
    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        // Reserved for post-overlay elements if needed
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        // Render 3D/world-space overlays
        for (com.bhop4real.proton.client.module.Module module : moduleManager.getModules())
        {
            if (module.isEnabled())
            {
                module.onRender3D(event.partialTicks);
            }
        }
    }

    // Apply reach immediately on mouse click to mirror other client implementations
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseInput(MouseEvent event)
    {
        if (event.button >= 0 && event.buttonstate)
        {
            com.bhop4real.proton.client.module.Module reachModule = moduleManager.getModuleByName("Reach");
            if (reachModule instanceof com.bhop4real.proton.client.module.modules.Reach && reachModule.isEnabled())
            {
                try
                {
                    ((com.bhop4real.proton.client.module.modules.Reach) reachModule).applyReachIfEligible();
                }
                catch (Throwable ignored) {}
            }
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        // Client tick handling
    }

    // Stop music when leaving a world (disconnect or world unload)
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        try {
            com.bhop4real.proton.client.music.MusicService.get().stop();
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        // Only care about client worlds
        try {
            if (event.world != null && event.world.isRemote) {
                com.bhop4real.proton.client.music.MusicService.get().stop();
            }
        } catch (Throwable ignored) {
        }
    }
}

