package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.sug.survival.assistant.plus.feature.HaoQiChongTian;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$sendPosition(CallbackInfo ci) {
        if (HaoQiChongTian.isFrozen()) ci.cancel();
    }
}
