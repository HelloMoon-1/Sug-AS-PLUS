package com.sug.survival.assistant.plus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.sug.survival.assistant.plus.config.Configs;
import com.sug.survival.assistant.plus.feature.Freecam;
import com.sug.survival.assistant.plus.feature.HaoQiChongTian;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "belowNameDisplay", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$belowNameDisplay(CallbackInfoReturnable<Component> cir) {
        if (Configs.NAMETAGS_HIDE_ENTITY_HEALTH.getBooleanValue() && !(((Object) this) instanceof Player)) cir.setReturnValue(null);
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$turn(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (HaoQiChongTian.shouldCancelLook(client.player) && (Object) this == client.player) {
            ci.cancel();
            return;
        }
        if (!Freecam.isActive() || (Object) this != client.player) return;
        Freecam.changeLookDirection(cursorDeltaX * 0.15D, cursorDeltaY * 0.15D);
        ci.cancel();
    }

    @ModifyExpressionValue(method = {"getBlockSpeedFactor", "getBlockJumpFactor"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;", remap = false), remap = false)
    private Block sug_survival_assistant_plus$modifyNoSlowBlock(Block block) {
        if (Configs.NO_SLOW.getBooleanValue() && (Object) this == Minecraft.getInstance().player && block == Blocks.HONEY_BLOCK) return Blocks.STONE;
        return block;
    }
}
