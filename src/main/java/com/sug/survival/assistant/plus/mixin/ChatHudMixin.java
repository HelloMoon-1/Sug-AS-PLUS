package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.BetterChat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin {
    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private void refreshTrimmedMessages() {
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sug_survival_assistant_plus$addMessage(
            Component message,
            MessageSignature signature,
            GuiMessageSource source,
            GuiMessageTag indicator,
            CallbackInfo ci
    ) {
        int ticks = this.minecraft.gui.hud.getGuiTicks();
        if (BetterChat.handleMessage(
                this.allMessages,
                this::refreshTrimmedMessages,
                message,
                signature,
                source,
                indicator,
                ticks
        )) {
            ci.cancel();
        }
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void sug_survival_assistant_plus$clearMessages(boolean clearHistory, CallbackInfo ci) {
        BetterChat.clear();
    }
}
