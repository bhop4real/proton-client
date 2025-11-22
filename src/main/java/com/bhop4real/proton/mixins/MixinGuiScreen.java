package com.bhop4real.proton.mixins;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for customizing loading screens and other GuiScreen instances
 * This can catch startup/loading screens that extend GuiScreen
 */
@Mixin(value = GuiScreen.class, remap = false)
public class MixinGuiScreen
{
    private static final net.minecraft.util.ResourceLocation PROTON_LOGO = new net.minecraft.util.ResourceLocation("proton", "textures/gui/proton_logo.png");
    
    /**
     * Inject into the drawScreen method to detect and customize loading screens
     */
    @Inject(method = "drawScreen(IIF)V", at = @At("HEAD"), cancellable = false, remap = false)
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        // Check if this is a loading screen (classes like GuiScreenWorking or similar)
        GuiScreen screen = (GuiScreen)(Object)this;
        String className = screen.getClass().getName().toLowerCase();
        
        // Check for common loading screen class names
        if (className.contains("loading") || className.contains("startup") || 
            className.contains("splash") || className.contains("working"))
        {
            // This might be a loading screen - draw custom loading screen
            drawCustomLoadingScreen(screen);
        }
    }
    
    /**
     * Draws custom loading screen with picture and white progress bar at bottom
     */
    private void drawCustomLoadingScreen(GuiScreen screen)
    {
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        // Draw background picture (full screen)
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        try
        {
            screen.mc.getTextureManager().bindTexture(PROTON_LOGO);
            // Draw full screen background
            screen.drawTexturedModalRect(0, 0, 0, 0, screenWidth, screenHeight);
        }
        catch (Exception e)
        {
            // If texture not found, draw solid dark background
            net.minecraft.client.gui.Gui.drawRect(0, 0, screenWidth, screenHeight, 0xFF1a1a1a);
        }
        
        // Draw white progress bar at the very bottom
        int progressBarHeight = 3;
        int progressBarY = screenHeight - progressBarHeight;
        
        // Background of progress bar (dark)
        net.minecraft.client.gui.Gui.drawRect(0, progressBarY, screenWidth, screenHeight, 0xFF333333);
        
        // Get loading progress
        float progress = getLoadingProgress();
        int progressWidth = (int)(screenWidth * progress);
        
        // White progress bar
        net.minecraft.client.gui.Gui.drawRect(0, progressBarY, progressWidth, screenHeight, 0xFFFFFFFF);
    }
    
    /**
     * Gets the actual loading progress
     */
    private float getLoadingProgress()
    {
        try
        {
            Class<?> splashProgressClass = Class.forName("net.minecraftforge.fml.client.SplashProgress");
            java.lang.reflect.Field barField = splashProgressClass.getDeclaredField("bar");
            barField.setAccessible(true);
            Object barObj = barField.get(null);
            
            if (barObj != null)
            {
                java.lang.reflect.Method getProgressMethod = barObj.getClass().getMethod("getProgress");
                Object progressObj = getProgressMethod.invoke(barObj);
                if (progressObj instanceof Number)
                {
                    return ((Number) progressObj).floatValue();
                }
            }
        }
        catch (Exception e)
        {
            // Return 0 if we can't get progress
        }
        
        return 0.0F;
    }
}

