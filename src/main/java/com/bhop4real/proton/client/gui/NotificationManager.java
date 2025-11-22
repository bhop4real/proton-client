package com.bhop4real.proton.client.gui;

import com.bhop4real.proton.client.util.animation.Animation;
import com.bhop4real.proton.client.util.animation.AnimationUtils;
import com.bhop4real.proton.client.util.animation.Easing;
import com.bhop4real.proton.client.util.font.FontUtil;
import com.bhop4real.proton.client.util.render.RenderUtil;
import com.bhop4real.proton.client.util.render.RoundedRectUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Windows 10 style notification manager
 * Notifications slide in from the left side of the screen to the bottom-left corner
 */
public class NotificationManager
{
    private static final List<Notification> notifications = new LinkedList<>();
    private static final int MAX_NOTIFICATIONS = 5;
    
    /**
     * Show a notification
     */
    public static void show(String title, String message, NotificationType type)
    {
        Notification notification = new Notification(title, message, type);
        notifications.add(0, notification);

        while (notifications.size() > MAX_NOTIFICATIONS)
        {
            notifications.remove(notifications.size() - 1);
        }
    }
    
    /**
     * Show a module toggle notification
     */
    public static void showModuleToggle(String moduleName, boolean enabled)
    {
        NotificationType type = enabled ? NotificationType.SUCCESS : NotificationType.INFO;
        String message = enabled ? "Enabled" : "Disabled";
        show(moduleName, message, type);
    }
    
    /**
     * Render all notifications (called every frame with partial ticks for smooth animation)
     */
    public static void render(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.currentScreen instanceof GuiClickGUI)
        {
            return; // Rendered directly by the GUI to ensure it sits on top
        }
        renderInternal(partialTicks, false);
    }

    public static void renderOnGui(float partialTicks)
    {
        renderInternal(partialTicks, true);
    }

    private static void renderInternal(float partialTicks, boolean forGui)
    {
        if (notifications.isEmpty())
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null)
        {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        final int spacing = Notification.NOTIFICATION_HEIGHT + 12;
        final int margin = 12;

        int baseX = sr.getScaledWidth() - Notification.NOTIFICATION_WIDTH - margin;
        int baseY = sr.getScaledHeight() - margin;

        if (forGui)
        {
            baseY = sr.getScaledHeight() - margin;
        }

        Iterator<Notification> iterator = notifications.iterator();
        int index = 0;
        while (iterator.hasNext())
        {
            Notification notification = iterator.next();
            notification.update();

            if (notification.isExpired())
            {
                iterator.remove();
                continue;
            }

            int y = baseY - (index * spacing) - Notification.NOTIFICATION_HEIGHT;
            notification.render(baseX, y, partialTicks, forGui);
            index++;
        }
    }
    
    /**
     * Update notification animations (called every tick)
     */
    public static void update()
    {
        // No-op retained for backwards compatibility (animation updated during render cycle)
    }
    
    /**
     * Individual notification
     */
    public static class Notification
    {
        private static final int NOTIFICATION_WIDTH = 214;
        private static final int NOTIFICATION_HEIGHT = 48;
        private static final double SLIDE_DISTANCE = 16.0D;
        private static final long ANIMATION_DURATION = 220L;
        private static final long DISPLAY_TIME = 3200L;

        private final String title;
        private final String message;
        private final NotificationType type;
        private final long createdTime;
        private boolean closing;

        private final Animation slideAnimation = new Animation(Easing.EASE_OUT_QUINT, ANIMATION_DURATION);
        private final Animation alphaAnimation = new Animation(Easing.EASE_OUT_QUINT, ANIMATION_DURATION);
        private final AnimationUtils heightAnimation = new AnimationUtils(0.0D);

        public Notification(String title, String message, NotificationType type)
        {
            this.title = title;
            this.message = message;
            this.type = type;
            this.createdTime = System.currentTimeMillis();
            this.slideAnimation.setStartValue(0.0D);
            this.alphaAnimation.setStartValue(0.0D);
        }

        public void update()
        {
            long elapsed = System.currentTimeMillis() - createdTime;
            if (!closing && elapsed >= DISPLAY_TIME)
            {
                closing = true;
                slideAnimation.reset();
                alphaAnimation.reset();
            }

            slideAnimation.run(closing ? 0.0D : 1.0D);
            alphaAnimation.run(closing ? 0.0D : 1.0D);
        }

        public void render(int targetX, int y, float partialTicks, boolean forGui)
        {
            double progress = slideAnimation.getValue();
            double alpha = alphaAnimation.getValue();
            heightAnimation.setAnimation(progress, 20.0D);

            double easedProgress = heightAnimation.getValue();
            double renderX = targetX + SLIDE_DISTANCE * (1.0D - easedProgress);

            float opacity = (float) Math.min(Math.max(alpha, 0.0D), 1.0D);

            int shadowAlpha = (int) (opacity * 90);
            int shadowColor = (shadowAlpha << 24);
            RenderUtil.drawRect((int) renderX - 3, y + 4, (int) renderX + NOTIFICATION_WIDTH + 3, y + NOTIFICATION_HEIGHT + 6, shadowColor);

            int bgAlpha = (int) (opacity * 225);
            int bgColor = (bgAlpha << 24) | 0x111317;

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            RoundedRectUtil.drawRoundedRect(renderX, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 6, bgColor);

            int accent = type.getAccentColor();
            int accentColor = ((int) (opacity * 255) << 24) | (accent & 0x00FFFFFF);
            RenderUtil.drawRect((int) renderX + 3, y + 5, (int) renderX + 5, y + NOTIFICATION_HEIGHT - 5, accentColor);

            int titleColor = ((int) (opacity * 255) << 24) | 0xF7FAFF;
            int messageColor = ((int) (opacity * 230) << 24) | 0xFFFFFF;

            float textBaseX = (float) renderX + 12;
            float titleY = y + 10;
            float messageY = y + 26;

            try
            {
                GlStateManager.enableTexture2D();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                FontUtil.drawString(title, textBaseX, titleY, titleColor, true);
                FontUtil.drawString(message, textBaseX, messageY, messageColor, true);
            }
            catch (Exception ignored)
            {
            }

            int borderColor = ((int) (opacity * 50) << 24) | 0xFFFFFF;
            RenderUtil.drawBorderedRect((int) renderX, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 1, borderColor, bgColor);

            GL11.glDisable(GL11.GL_BLEND);
        }

        public boolean isExpired()
        {
            return closing && slideAnimation.isFinished();
        }
    }
    
    /**
     * Notification type with accent color
     */
    public enum NotificationType
    {
        INFO(0x4A90E2),      // Blue
        SUCCESS(0x4CAF50),   // Green
        WARNING(0xF5A623),   // Orange
        ERROR(0xE53935);     // Red

        private final int accentColor;

        NotificationType(int accentColor)
        {
            this.accentColor = accentColor;
        }

        public int getAccentColor()
        {
            return accentColor;
        }
    }
}
