package com.bhop4real.proton.mixins;

import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.modules.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient
{
    @Inject(
        method = "handleEntityVelocity(Lnet/minecraft/network/play/server/S12PacketEntityVelocity;)V",
        at = @At("RETURN")
    )
    private void proton$onHandleEntityVelocity(S12PacketEntityVelocity packet, CallbackInfo ci)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null)
        {
            return;
        }

        Entity entity = mc.theWorld.getEntityByID(packet.getEntityID());
        if (entity != mc.thePlayer)
        {
            return;
        }

        ProtonClient client = ProtonClient.getInstance();
        if (client == null || client.getModuleManager() == null)
        {
            return;
        }

        Module module = client.getModuleManager().getModuleByName("Velocity");
        if (!(module instanceof Velocity) || !module.isEnabled())
        {
            return;
        }

        double motionX = packet.getMotionX() / 8000.0D;
        double motionY = packet.getMotionY() / 8000.0D;
        double motionZ = packet.getMotionZ() / 8000.0D;
        ((Velocity) module).onVelocityPacket(motionX, motionY, motionZ);
    }
}

