package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.Freecam;
import com.sug.survival.assistant.plus.feature.HaoQiChongTian;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void sug_survival_assistant_plus$afterAlignWithEntity(DeltaTracker tickCounter, CallbackInfo ci) {
        if (!HaoQiChongTian.isFrozen() && !Freecam.isActive()) {
            return;
        }

        Camera self = (Camera) (Object) this;
        float tickDelta = self.getCameraEntityPartialTicks(tickCounter);
        CameraAccessor accessor = (CameraAccessor) (Object) this;

        if (HaoQiChongTian.isFrozen()) {
            accessor.sug_survival_assistant_plus$setPosition(
                    HaoQiChongTian.getCameraX(tickDelta),
                    HaoQiChongTian.getCameraY(tickDelta),
                    HaoQiChongTian.getCameraZ(tickDelta)
            );
            accessor.sug_survival_assistant_plus$setRotation(
                    HaoQiChongTian.getCameraYaw(tickDelta),
                    HaoQiChongTian.getCameraPitch()
            );
            return;
        }

        accessor.sug_survival_assistant_plus$setPosition(
                Freecam.getX(tickDelta),
                Freecam.getY(tickDelta),
                Freecam.getZ(tickDelta)
        );
        accessor.sug_survival_assistant_plus$setRotation(
                Freecam.getYaw(tickDelta),
                Freecam.getPitch(tickDelta)
        );
    }

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float sug_survival_assistant_plus$modifyGetMaxZoom(float desiredCameraDistance) {
        return Freecam.isActive() || HaoQiChongTian.isFrozen() ? 0.0F : desiredCameraDistance;
    }
}
