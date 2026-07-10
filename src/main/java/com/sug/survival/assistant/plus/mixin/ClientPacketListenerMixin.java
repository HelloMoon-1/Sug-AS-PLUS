package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.ZhouLi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique
    private boolean sug_survival_assistant_plus$zhouLiProcessing;

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$sendChat(String message, CallbackInfo ci) {
        if (!ZhouLi.isActive() || sug_survival_assistant_plus$zhouLiProcessing) return;
        if (ZhouLi.shouldSkip(message)) return;

        ci.cancel();
        sug_survival_assistant_plus$zhouLiProcessing = true;
        String original = message;
        ClientPacketListener self = (ClientPacketListener) (Object) this;

        ZhouLi.rewriteAsync(original).whenCompleteAsync((rewritten, error) -> {
            try {
                self.sendChat(error == null && rewritten != null ? rewritten : original);
            } finally {
                sug_survival_assistant_plus$zhouLiProcessing = false;
            }
        }, Minecraft.getInstance());
    }
}
