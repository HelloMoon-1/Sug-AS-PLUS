package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.sug.survival.assistant.plus.feature.BetterChat;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin {
    @Shadow(remap = false) @Final private List<GuiMessage> allMessages;
    @Shadow(remap = false) @Final private Minecraft minecraft;
    @Shadow(remap = false) private void refreshTrimmedMessages() {}

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$addMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci) {
        if (BetterChat.handleMessage(allMessages, this::refreshTrimmedMessages, message, signature, source, indicator, minecraft.gui.getGuiTicks())) ci.cancel();
    }

    @Inject(method = "clearMessages", at = @At("HEAD"), remap = false)
    private void sug_survival_assistant_plus$clearMessages(boolean clearHistory, CallbackInfo ci) {
        BetterChat.clear();
    }
}
