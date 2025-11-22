package com.bhop4real.proton.mixins;

import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.modules.KillAura;
import com.bhop4real.proton.client.util.rotation.RotationUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for EntityLivingBase to support realistic rotation mode
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {
    @Shadow
    public float rotationYawHead;

    @Shadow
    public float renderYawOffset;

    /**
     * Inject head yaw rotation modification for realistic rotation mode
     * Based on FDPClient's implementation
     */
    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateEntityActionState()V", shift = At.Shift.AFTER))
    private void hookHeadRotations(CallbackInfo ci) {
        // Only apply to the player
        // noinspection ConstantValue
        if (!((EntityLivingBase) (Object) this instanceof EntityPlayerSP)) {
            return;
        }

        // Check if KillAura is enabled with silent rotation
        ProtonClient client = ProtonClient.getInstance();
        if (client == null) {
            return;
        }

        Module killAuraModule = client.getModuleManager().getModuleByName("KillAura");
        if (!(killAuraModule instanceof KillAura)) {
            return;
        }

        KillAura killAura = (KillAura) killAuraModule;
        if (!killAura.isEnabled() || !killAura.isSilentRotationEnabled()) {
            return;
        }

        // Apply realistic rotation if we have a current rotation
        if (RotationUtils.currentRotation != null) {
            this.rotationYawHead = RotationUtils.currentRotation.getYaw();
            this.renderYawOffset = RotationUtils.currentRotation.getYaw();
        }
    }
}
