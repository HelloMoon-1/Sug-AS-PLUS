package com.sug.survival.assistant.plus.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.sug.survival.assistant.plus.config.Configs;

import java.util.HashSet;
import java.util.Set;

public final class GhostHand {
    private static final Set<BlockPos> VISITED = new HashSet<>();

    private GhostHand() {
    }

    public static void tick(Minecraft client) {
        if (!Configs.GHOST_HAND.getBooleanValue() || client.player == null || client.level == null || client.gameMode == null) return;
        if (!client.options.keyUse.isDown() || client.player.isShiftKeyDown()) return;

        VISITED.clear();
        Vec3 eye = client.player.getEyePosition();
        Vec3 direction = client.player.getLookAngle().normalize().scale(0.1D);
        int steps = (int) (Configs.GHOST_HAND_RANGE.getDoubleValue() * 10.0D);

        for (int i = 1; i <= steps; i++) {
            BlockPos pos = BlockPos.containing(eye.add(direction.scale(i)));
            if (!VISITED.add(pos)) continue;

            BlockState state = client.level.getBlockState(pos);
            if (!state.hasBlockEntity() || isBlacklisted(state.getBlock())) continue;

            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, true);
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
            return;
        }
    }

    private static boolean isBlacklisted(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        for (String value : Configs.GHOST_HAND_BLACKLIST.getStrings()) {
            if (value.equals(id.toString())) return true;
        }
        return false;
    }
}
