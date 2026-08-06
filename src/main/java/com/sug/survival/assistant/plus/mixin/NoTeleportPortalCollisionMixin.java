package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.NoTeleport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class NoTeleportPortalCollisionMixin {
    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sug_survival_assistant_plus$getCollisionShape(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (sug_survival_assistant_plus$isActiveNetherPortal()) {
            cir.setReturnValue(Shapes.block());
        }
    }

    @Inject(method = "hasLargeCollisionShape", at = @At("HEAD"), cancellable = true)
    private void sug_survival_assistant_plus$hasLargeCollisionShape(CallbackInfoReturnable<Boolean> cir) {
        if (sug_survival_assistant_plus$isActiveNetherPortal()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean sug_survival_assistant_plus$isActiveNetherPortal() {
        return NoTeleport.isActive()
                && ((BlockBehaviour.BlockStateBase) (Object) this).is(Blocks.NETHER_PORTAL);
    }
}
