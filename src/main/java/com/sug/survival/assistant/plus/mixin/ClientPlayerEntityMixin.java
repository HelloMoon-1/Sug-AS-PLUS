package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.HaoQiChongTian;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void sug_survival_assistant_plus$sendPosition(CallbackInfo ci) {
        if (HaoQiChongTian.isFrozen()) {
            ci.cancel();
        }
    }
}
