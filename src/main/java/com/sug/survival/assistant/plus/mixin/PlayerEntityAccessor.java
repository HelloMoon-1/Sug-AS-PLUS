package com.sug.survival.assistant.plus.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Avatar.class)
public interface PlayerEntityAccessor {
    @Accessor("DATA_PLAYER_MODE_CUSTOMISATION")
    static EntityDataAccessor<Byte> sug_survival_assistant_plus$getPlayerModelParts() {
        throw new AssertionError();
    }
}
