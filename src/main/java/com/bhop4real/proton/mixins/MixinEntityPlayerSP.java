package com.bhop4real.proton.mixins;

import com.bhop4real.proton.client.event.*;
import com.bhop4real.proton.client.util.rotation.RotationUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityPlayerSP.class, priority = 999)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    @Shadow
    @org.spongepowered.asm.mixin.Final
    public NetHandlerPlayClient sendQueue;

    @Shadow
    public MovementInput movementInput;

    @Shadow
    protected int sprintToggleTimer;

    @Shadow
    public int sprintingTicksLeft;

    @Shadow
    private boolean serverSprintState;

    @Shadow
    private boolean serverSneakState;

    @Shadow
    private double lastReportedPosX;

    @Shadow
    private double lastReportedPosY;

    @Shadow
    private double lastReportedPosZ;

    @Shadow
    private float lastReportedYaw;

    @Shadow
    private float lastReportedPitch;

    @Shadow
    private int positionUpdateTicks;

    @Shadow
    protected abstract boolean isCurrentViewEntity();

    protected MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    /**
     * @author bhop4real
     * @reason mixin on update to post PreUpdateEvent and PostUpdateEvent
     */
    @Overwrite
    public void onUpdate() {
        if (this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0, this.posZ))) {
            RotationUtils.prevRenderPitch = RotationUtils.renderPitch;
            RotationUtils.prevRenderYaw = RotationUtils.renderYaw;

            MinecraftForge.EVENT_BUS.post(new PreUpdateEvent());

            super.onUpdate();

            if (this.isRiding()) {
                this.sendQueue.addToSendQueue(
                        new C03PacketPlayer.C05PacketPlayerLook(this.rotationYaw, this.rotationPitch, this.onGround));
                this.sendQueue.addToSendQueue(new C0CPacketInput(this.movementInput.moveStrafe,
                        this.movementInput.moveForward, this.movementInput.jump, this.movementInput.sneak));
            } else {
                this.onUpdateWalkingPlayer();
            }

            MinecraftForge.EVENT_BUS.post(new PostUpdateEvent());
        }
    }

    /**
     * @author bhop4real
     * @reason mixin on update walking player to post PreMotionEvent and
     *         PostMotionEvent, handles rotation
     */
    @Overwrite
    public void onUpdateWalkingPlayer() {
        // Use RotationUtils rotations if available for silent rotation
        RotationUtils.onUpdateEvent();

        float yaw = this.rotationYaw;
        float pitch = this.rotationPitch;

        if (RotationUtils.currentRotation != null) {
            yaw = RotationUtils.currentRotation.getYaw();
            pitch = RotationUtils.currentRotation.getPitch();
        }

        PreMotionEvent preMotionEvent = new PreMotionEvent(
                this.posX,
                this.getEntityBoundingBox().minY,
                this.posZ,
                yaw,
                pitch,
                this.onGround,
                this.isSprinting(),
                this.isSneaking());

        MinecraftForge.EVENT_BUS.post(preMotionEvent);

        boolean flag = preMotionEvent.isSprinting();
        if (flag != this.serverSprintState) {
            if (flag) {
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }
            this.serverSprintState = flag;
        }

        boolean flag1 = preMotionEvent.isSneaking();
        if (flag1 != this.serverSneakState) {
            if (flag1) {
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SNEAKING));
            } else {
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SNEAKING));
            }
            this.serverSneakState = flag1;
        }

        if (this.isCurrentViewEntity()) {
            if (PreMotionEvent.setRenderYaw()) {
                RotationUtils.setRenderYaw(preMotionEvent.getYaw());
                preMotionEvent.setRenderYaw(false);
            }

            RotationUtils.renderPitch = preMotionEvent.getPitch();
            RotationUtils.renderYaw = preMotionEvent.getYaw();

            double d0 = preMotionEvent.getPosX() - this.lastReportedPosX;
            double d1 = preMotionEvent.getPosY() - this.lastReportedPosY;
            double d2 = preMotionEvent.getPosZ() - this.lastReportedPosZ;
            double d3 = preMotionEvent.getYaw() - this.lastReportedYaw;
            double d4 = preMotionEvent.getPitch() - this.lastReportedPitch;
            boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4 || this.positionUpdateTicks >= 20;
            boolean flag3 = d3 != 0.0 || d4 != 0.0;

            if (this.ridingEntity == null) {
                if (flag2 && flag3) {
                    C03PacketPlayer.C06PacketPlayerPosLook packet = new C03PacketPlayer.C06PacketPlayerPosLook(
                            preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(),
                            preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround());
                    RotationUtils.onPacketSend(packet);
                    this.sendQueue.addToSendQueue(packet);
                } else if (flag2) {
                    C03PacketPlayer.C04PacketPlayerPosition packet = new C03PacketPlayer.C04PacketPlayerPosition(
                            preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(),
                            preMotionEvent.isOnGround());
                    RotationUtils.onPacketSend(packet);
                    this.sendQueue.addToSendQueue(packet);
                } else if (flag3) {
                    C03PacketPlayer.C05PacketPlayerLook packet = new C03PacketPlayer.C05PacketPlayerLook(
                            preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround());
                    RotationUtils.onPacketSend(packet);
                    this.sendQueue.addToSendQueue(packet);
                } else {
                    C03PacketPlayer packet = new C03PacketPlayer(preMotionEvent.isOnGround());
                    RotationUtils.onPacketSend(packet);
                    this.sendQueue.addToSendQueue(packet);
                }
            } else {
                C03PacketPlayer.C06PacketPlayerPosLook packet = new C03PacketPlayer.C06PacketPlayerPosLook(this.motionX,
                        -999.0D, this.motionZ, preMotionEvent.getYaw(), preMotionEvent.getPitch(),
                        preMotionEvent.isOnGround());
                RotationUtils.onPacketSend(packet);
                this.sendQueue.addToSendQueue(packet);
                flag2 = false;
            }

            ++this.positionUpdateTicks;

            if (flag2) {
                this.lastReportedPosX = preMotionEvent.getPosX();
                this.lastReportedPosY = preMotionEvent.getPosY();
                this.lastReportedPosZ = preMotionEvent.getPosZ();
                this.positionUpdateTicks = 0;
            }

            if (flag3) {
                this.lastReportedYaw = preMotionEvent.getYaw();
                this.lastReportedPitch = preMotionEvent.getPitch();
            }
        }

        MinecraftForge.EVENT_BUS.post(new PostMotionEvent());
    }
}
