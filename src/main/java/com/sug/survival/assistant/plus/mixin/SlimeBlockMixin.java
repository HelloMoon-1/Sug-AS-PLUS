package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.sug.survival.assistant.plus.config.Configs;

@Mixin(SlimeBlock.class)
public abstract class SlimeBlockMixin {
    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$stepOn(Level world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if (Configs.NO_SLOW.getBooleanValue() && entity == Minecraft.getInstance().player) ci.cancel();
    }
}
