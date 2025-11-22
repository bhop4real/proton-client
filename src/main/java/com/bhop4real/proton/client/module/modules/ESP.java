package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.module.settings.ColorSetting;
import com.bhop4real.proton.client.util.render.ESPUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.util.List;

/**
 * ESP (Extra Sensory Perception) module
 * Highlights players, entities, blocks, chests, items, etc.
 */
public class ESP extends Module
{
    public BooleanSetting players;
    public BooleanSetting entities;
    public BooleanSetting chests;
    public BooleanSetting enderChests;
    public BooleanSetting items;
    public BooleanSetting blocks;
    public BooleanSetting tracers;
    public BooleanSetting filled;
    public IntSetting lineWidth;
    public ColorSetting playerColor;
    public ColorSetting entityColor;
    public ColorSetting chestColor;
    public ColorSetting itemColor;
    public ColorSetting blockColor;
    
    public ESP()
    {
        super("ESP", new String[]{"esp"}, "Highlights entities and blocks", Module.Category.RENDER, 0);
        
        // Toggle settings
        players = new BooleanSetting("Players", "Show player ESP", true);
        entities = new BooleanSetting("Entities", "Show entity ESP", false);
        chests = new BooleanSetting("Chests", "Show chest ESP", true);
        enderChests = new BooleanSetting("Ender Chests", "Show ender chest ESP", true);
        items = new BooleanSetting("Items", "Show item ESP", false);
        blocks = new BooleanSetting("Blocks", "Show block ESP", false);
        tracers = new BooleanSetting("Tracers", "Draw tracer lines", false);
        filled = new BooleanSetting("Filled", "Fill ESP boxes", false);
        
        // Appearance settings
        lineWidth = new IntSetting("Line Width", "ESP line width", 2, 1, 5);
        playerColor = new ColorSetting("Player Color", "Player ESP color", 0xFF00FF00);
        entityColor = new ColorSetting("Entity Color", "Entity ESP color", 0xFFFF0000);
        chestColor = new ColorSetting("Chest Color", "Chest ESP color", 0xFFFFFF00);
        itemColor = new ColorSetting("Item Color", "Item ESP color", 0xFFFF00FF);
        blockColor = new ColorSetting("Block Color", "Block ESP color", 0xFF00FFFF);
        
        // Add all settings
        addSetting(players);
        addSetting(entities);
        addSetting(chests);
        addSetting(enderChests);
        addSetting(items);
        addSetting(blocks);
        addSetting(tracers);
        addSetting(filled);
        addSetting(lineWidth);
        addSetting(playerColor);
        addSetting(entityColor);
        addSetting(chestColor);
        addSetting(itemColor);
        addSetting(blockColor);
    }
    
    @Override
    public void onRender()
    {
        // ESP rendering is done in onRender3D
    }
    
    @Override
    public void onRender3D(float partialTicks)
    {
        if (mc.theWorld == null || mc.thePlayer == null)
        {
            return;
        }
        
        render3D(partialTicks);
    }
    
    private void render3D(float partialTicks)
    {
        float width = (float) lineWidth.getValue();
        
        // Render players
        if (players.getValue())
        {
            List<EntityPlayer> playerList = mc.theWorld.playerEntities;
            for (EntityPlayer player : playerList)
            {
                if (player != mc.thePlayer && !player.isInvisible())
                {
                    int color = playerColor.getColor();
                    AxisAlignedBB bb = ESPUtil.getInterpolatedBoundingBox(player, partialTicks);
                    
                    if (bb != null)
                    {
                        renderBoundingBox(bb, player, color, width, partialTicks);
                    }
                }
            }
        }
        
        // Render entities
        if (entities.getValue())
        {
            List<Entity> entityList = mc.theWorld.loadedEntityList;
            for (Entity entity : entityList)
            {
                if (entity != mc.thePlayer && !(entity instanceof EntityPlayer) && !(entity instanceof EntityItem))
                {
                    int color = entityColor.getColor();
                    AxisAlignedBB bb = ESPUtil.getInterpolatedBoundingBox(entity, partialTicks);
                    
                    if (bb != null)
                    {
                        renderBoundingBox(bb, entity, color, width, partialTicks);
                    }
                }
            }
        }
        
        // Render items
        if (items.getValue())
        {
            List<Entity> entityList = mc.theWorld.loadedEntityList;
            for (Entity entity : entityList)
            {
                if (entity instanceof EntityItem)
                {
                    int color = itemColor.getColor();
                    AxisAlignedBB bb = ESPUtil.getInterpolatedBoundingBox(entity, partialTicks);
                    
                    if (bb != null)
                    {
                        renderBoundingBox(bb, entity, color, width, partialTicks);
                    }
                }
            }
        }
        
        // Render chests (tile entities don't need interpolation as they're static)
        if (chests.getValue() || enderChests.getValue())
        {
            List<TileEntity> tileEntityList = mc.theWorld.loadedTileEntityList;
            for (TileEntity tileEntity : tileEntityList)
            {
                BlockPos pos = tileEntity.getPos();
                int color;
                
                if (tileEntity instanceof TileEntityChest && chests.getValue())
                {
                    color = chestColor.getColor();
                }
                else if (tileEntity instanceof TileEntityEnderChest && enderChests.getValue())
                {
                    color = chestColor.getColor();
                }
                else
                {
                    continue;
                }
                
                AxisAlignedBB bb = new AxisAlignedBB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
                );
                
                double distance = mc.thePlayer.getDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                int outlineColor = applyDistanceAlpha(color, distance);
                
                if (filled.getValue())
                {
                    ESPUtil.drawFilledBox(bb, deriveFillColor(outlineColor));
                }
                ESPUtil.drawBox(bb, outlineColor, width);
            }
        }
        
        // Render blocks (placeholder - can be extended for specific block types)
        if (blocks.getValue())
        {
            // This would require additional logic to find and highlight specific blocks
            // For now, this is a placeholder for future expansion
        }
    }
    
    
    private void renderBoundingBox(AxisAlignedBB bb, Entity entity, int baseColor, float width, float partialTicks)
    {
        // Interpolate player position for distance calculation
        double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double playerY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        
        // Interpolate entity position
        double entityX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double entityY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double entityZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        
        double distance = Math.sqrt(
            Math.pow(entityX - playerX, 2) +
            Math.pow(entityY - playerY, 2) +
            Math.pow(entityZ - playerZ, 2)
        );
        int outlineColor = applyDistanceAlpha(baseColor, distance);
        
        if (filled.getValue())
        {
            ESPUtil.drawFilledBox(bb, deriveFillColor(outlineColor));
        }
        ESPUtil.drawBox(bb, outlineColor, width);
        
        if (tracers.getValue())
        {
            ESPUtil.drawTracer(entity, outlineColor, width, partialTicks);
        }
    }
    
    private int applyDistanceAlpha(int color, double distance)
    {
        double normalized = 1.0D - Math.min(distance, 64.0D) / 64.0D;
        float alpha = (float)(0.35D + normalized * 0.55D);
        alpha = Math.max(0.2F, Math.min(1.0F, alpha));
        return withAlpha(color, alpha);
    }
    
    private int deriveFillColor(int outlineColor)
    {
        float outlineAlpha = ((outlineColor >> 24) & 0xFF) / 255.0F;
        float fillAlpha = Math.min(0.35F, outlineAlpha * 0.45F + 0.1F);
        return withAlpha(outlineColor, fillAlpha);
    }
    
    private int withAlpha(int color, float alpha)
    {
        int alphaInt = (int)(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) & 0xFF;
        return (color & 0x00FFFFFF) | (alphaInt << 24);
    }
}
