package com.bhop4real.proton.client.module.impl.other;

import com.bhop4real.proton.client.event.PreUpdateEvent;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class SlotHandler extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private static Integer currentSlot = null;
    private static long lastSetCurrentSlotTime = -1;
    
    private EnumSetting mode = new EnumSetting("Mode", "Slot handler mode", "Default", "Default", "Silent");
    private DoubleSetting switchBackDelay = new DoubleSetting("Switch back delay", "Delay before switching back", 100.0, 0.0, 1000.0, 10.0);

    public SlotHandler() {
        super("SlotHandler", "Handles slot switching", Module.Category.MISC, 0);
        this.addSetting(mode);
        this.addSetting(switchBackDelay);
        // Always enabled but hidden
        this.setEnabled(true);
        this.setVisible(false);
    }

    public static int getCurrentSlot() {
        if (currentSlot != null)
            return currentSlot;
        return mc.thePlayer != null ? mc.thePlayer.inventory.currentItem : 0;
    }

    public static ItemStack getHeldItem() {
        if (mc.thePlayer == null) return null;
        final InventoryPlayer inventory = mc.thePlayer.inventory;
        if (currentSlot != null && currentSlot < 9 && currentSlot >= 0)
            return inventory.mainInventory[currentSlot];
        return getRenderHeldItem();
    }

    public static ItemStack getRenderHeldItem() {
        if (mc.thePlayer == null) return null;
        final InventoryPlayer inventory = mc.thePlayer.inventory;
        return inventory.currentItem < 9 && inventory.currentItem >= 0 ? inventory.mainInventory[inventory.currentItem] : null;
    }

    public static void setCurrentSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            currentSlot = slot;
            lastSetCurrentSlotTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;
        
        String modeValue = mode.getValue();
        if (modeValue.equals("Default")) {
            mc.thePlayer.inventory.currentItem = getCurrentSlot();
            currentSlot = null;
        } else if (modeValue.equals("Silent")) {
            if (currentSlot != null && System.currentTimeMillis() - lastSetCurrentSlotTime > switchBackDelay.getValue()) {
                currentSlot = null;
            }
        }
    }
}
