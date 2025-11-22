package com.bhop4real.proton.client.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * ESP (Extra Sensory Perception) rendering utilities
 * Provides methods for drawing boxes, lines, and other 3D shapes in world space
 */
public final class ESPUtil
{
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private ESPUtil() {} // Prevent instantiation
    
    /**
     * Draws a 3D box around an entity
     */
    public static void drawEntityBox(Entity entity, int color, float lineWidth)
    {
        if (entity == null || mc.getRenderManager() == null)
        {
            return;
        }
        
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        drawBox(bb, color, lineWidth);
    }
    
    /**
     * Draws a 3D box at a block position
     */
    public static void drawBlockBox(BlockPos pos, int color, float lineWidth)
    {
        if (pos == null || mc.theWorld == null)
        {
            return;
        }
        
        AxisAlignedBB bb = mc.theWorld.getBlockState(pos).getBlock().getSelectedBoundingBox(mc.theWorld, pos);
        if (bb != null)
        {
            drawBox(bb, color, lineWidth);
        }
    }
    
    /**
     * Draws a custom 3D box using an AxisAlignedBB
     */
    public static void drawBox(AxisAlignedBB bb, int color, float lineWidth)
    {
        if (bb == null || mc.getRenderManager() == null)
        {
            return;
        }
        
        double minX = bb.minX;
        double minY = bb.minY;
        double minZ = bb.minZ;
        double maxX = bb.maxX;
        double maxY = bb.maxY;
        double maxZ = bb.maxZ;
        
        // Calculate relative to camera
        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;
        
        minX -= renderX;
        minY -= renderY;
        minZ -= renderZ;
        maxX -= renderX;
        maxY -= renderY;
        maxZ -= renderZ;
        
        // Extract color components
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        
        // Slightly expand to reduce z-fighting
        final double expand = 0.0025D;
        minX -= expand;
        minY -= expand;
        minZ -= expand;
        maxX += expand;
        maxY += expand;
        maxZ += expand;

        // Setup GL state
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(red, green, blue, alpha);
        
        // Draw box outline
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        // Bottom face
        worldRenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        worldRenderer.pos(minX, minY, minZ).endVertex();
        worldRenderer.pos(maxX, minY, minZ).endVertex();
        worldRenderer.pos(maxX, minY, maxZ).endVertex();
        worldRenderer.pos(minX, minY, maxZ).endVertex();
        tessellator.draw();

        // Top face
        worldRenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        worldRenderer.pos(minX, maxY, minZ).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).endVertex();
        tessellator.draw();

        // Vertical edges
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldRenderer.pos(minX, minY, minZ).endVertex();
        worldRenderer.pos(minX, maxY, minZ).endVertex();

        worldRenderer.pos(maxX, minY, minZ).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).endVertex();

        worldRenderer.pos(maxX, minY, maxZ).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).endVertex();

        worldRenderer.pos(minX, minY, maxZ).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).endVertex();
        tessellator.draw();
        
        // Restore GL state
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
    
    /**
     * Draws a filled 3D box
     */
    public static void drawFilledBox(AxisAlignedBB bb, int color)
    {
        if (bb == null || mc.getRenderManager() == null)
        {
            return;
        }
        
        double minX = bb.minX;
        double minY = bb.minY;
        double minZ = bb.minZ;
        double maxX = bb.maxX;
        double maxY = bb.maxY;
        double maxZ = bb.maxZ;
        
        // Calculate relative to camera
        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;
        
        minX -= renderX;
        minY -= renderY;
        minZ -= renderZ;
        maxX -= renderX;
        maxY -= renderY;
        maxZ -= renderZ;
        
        // Extract color components
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        
        // Slightly shrink to avoid z-fighting with outline
        final double shrink = 0.0015D;
        minX += shrink;
        minY += shrink;
        minZ += shrink;
        maxX -= shrink;
        maxY -= shrink;
        maxZ -= shrink;

        // Setup GL state
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting();
        GlStateManager.color(red, green, blue, alpha);
        
        // Offset polygons slightly
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1.0F, -1100000.0F);

        // Draw filled box faces
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        
        // Bottom face
        worldRenderer.pos(minX, minY, minZ).endVertex();
        worldRenderer.pos(maxX, minY, minZ).endVertex();
        worldRenderer.pos(maxX, minY, maxZ).endVertex();
        worldRenderer.pos(minX, minY, maxZ).endVertex();
        
        // Top face
        worldRenderer.pos(minX, maxY, minZ).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).endVertex();
        
        // North face
        worldRenderer.pos(minX, minY, minZ).endVertex();
        worldRenderer.pos(minX, maxY, minZ).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).endVertex();
        worldRenderer.pos(maxX, minY, minZ).endVertex();
        
        // South face
        worldRenderer.pos(minX, minY, maxZ).endVertex();
        worldRenderer.pos(maxX, minY, maxZ).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).endVertex();
        
        // West face
        worldRenderer.pos(minX, minY, minZ).endVertex();
        worldRenderer.pos(minX, minY, maxZ).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).endVertex();
        worldRenderer.pos(minX, maxY, minZ).endVertex();
        
        // East face
        worldRenderer.pos(maxX, minY, minZ).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).endVertex();
        worldRenderer.pos(maxX, minY, maxZ).endVertex();
        
        tessellator.draw();
        
        // Restore GL state
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
    
    /**
     * Draws a line from one 3D point to another
     */
    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color, float lineWidth)
    {
        if (mc.getRenderManager() == null)
        {
            return;
        }
        
        // Calculate relative to camera
        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;
        
        x1 -= renderX;
        y1 -= renderY;
        z1 -= renderZ;
        x2 -= renderX;
        y2 -= renderY;
        z2 -= renderZ;
        
        // Extract color components
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        
        // Setup GL state
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting();
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(red, green, blue, alpha);
        
        // Draw line
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1, z1).endVertex();
        worldRenderer.pos(x2, y2, z2).endVertex();
        tessellator.draw();
        
        // Restore GL state
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
    
    /**
     * Draws a tracer line from camera to entity
     */
    public static void drawTracer(Entity entity, int color, float lineWidth)
    {
        drawTracer(entity, color, lineWidth, 0.0F);
    }
    
    /**
     * Draws a tracer line from camera to entity with interpolated position
     */
    public static void drawTracer(Entity entity, int color, float lineWidth, float partialTicks)
    {
        if (entity == null || mc.getRenderManager() == null || mc.thePlayer == null)
        {
            return;
        }
        
        // Interpolate entity position
        double entityX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double entityY = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks) + entity.height / 2.0D;
        double entityZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        
        // Interpolate player position
        double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double playerY = (mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks) + mc.thePlayer.getEyeHeight();
        double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        
        drawLine(playerX, playerY, playerZ, entityX, entityY, entityZ, color, lineWidth);
    }
    
    /**
     * Gets interpolated bounding box for an entity using partialTicks
     */
    public static AxisAlignedBB getInterpolatedBoundingBox(Entity entity, float partialTicks)
    {
        if (entity == null)
        {
            return null;
        }
        
        // Interpolate entity position
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        
        // Get bounding box dimensions
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double width = (bb.maxX - bb.minX) / 2.0D;
        double height = bb.maxY - bb.minY;
        double depth = (bb.maxZ - bb.minZ) / 2.0D;
        
        // Create interpolated bounding box
        return new AxisAlignedBB(
            x - width, y, z - depth,
            x + width, y + height, z + depth
        );
    }
    
    /**
     * Draws a small 3D box (dot) at a point in world space
     */
    public static void drawDot(double x, double y, double z, double size, int color)
    {
        double halfSize = size / 2.0D;
        AxisAlignedBB bb = new AxisAlignedBB(
            x - halfSize, y - halfSize, z - halfSize,
            x + halfSize, y + halfSize, z + halfSize
        );
        drawFilledBox(bb, color);
        drawBox(bb, color, 1.5F);
    }
}
