package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.sug.survival.assistant.plus.feature.Freecam;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$renderItemInHand(CameraRenderState camera, float tickProgress, Matrix4fc positionMatrix, CallbackInfo ci) {
        if (!Freecam.renderHands()) ci.cancel();
    }
}
