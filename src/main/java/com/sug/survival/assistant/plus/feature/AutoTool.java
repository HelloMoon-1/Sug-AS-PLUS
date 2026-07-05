package com.sug.survival.assistant.plus.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import com.sug.survival.assistant.plus.config.Configs;

public final class AutoTool {
    private static int previousSlot = -1;
    private static int swappedFromSlot = -1;
    private static int swappedHotbarSlot = -1;

    private AutoTool() {
    }

    public static void tick(Minecraft client) {
        if (!Configs.AUTO_TOOL.getBooleanValue() || client.player == null || client.level == null || client.screen != null) {
            reset();
            return;
        }

        if (GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            if (previousSlot != -1) stopMining();
            return;
        }

        if (client.hitResult instanceof BlockHitResult hitResult && client.hitResult.getType() == HitResult.Type.BLOCK) {
            beforeBlockAttack(hitResult.getBlockPos());
        }
    }

    public static void beforeBlockAttack(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (!Configs.AUTO_TOOL.getBooleanValue() || client.player == null || client.level == null || client.screen != null) return;

        int currentSlot = InventoryHelper.getSelectedHotbarSlot(client);
        if (previousSlot == -1) previousSlot = currentSlot;

        BlockState state = client.level.getBlockState(pos);
        if (swappedFromSlot != -1) {
            if (currentSlot != swappedHotbarSlot) InventoryHelper.selectHotbarSlot(client, swappedHotbarSlot);
            currentSlot = swappedHotbarSlot;
            if (findBestToolSlot(client, state, currentSlot) == currentSlot) return;
            restoreSwappedTool(client);
            swappedFromSlot = -1;
            swappedHotbarSlot = -1;
        }

        currentSlot = InventoryHelper.getSelectedHotbarSlot(client);
        int bestSlot = findBestToolSlot(client, state, currentSlot);
        if (bestSlot < 0 || bestSlot == currentSlot) return;

        if (bestSlot < 9) {
            if (currentSlot != bestSlot) InventoryHelper.selectHotbarSlot(client, bestSlot);
            return;
        }

        InventoryHelper.swapWithHotbar(client, bestSlot, currentSlot);
        swappedFromSlot = bestSlot;
        swappedHotbarSlot = currentSlot;
        InventoryHelper.selectHotbarSlot(client, swappedHotbarSlot);
    }

    public static void stopMining() {
        Minecraft client = Minecraft.getInstance();
        if (previousSlot == -1) return;
        if (Configs.AUTO_TOOL_SWITCH_BACK.getBooleanValue()) {
            restoreSwappedTool(client);
            InventoryHelper.selectHotbarSlot(client, previousSlot);
        }
        reset();
    }

    private static int findBestToolSlot(Minecraft client, BlockState state, int currentSlot) {
        Inventory inventory = client.player.getInventory();
        int bestSlot = currentSlot;
        float bestSpeed = getSpeed(inventory.getItem(bestSlot), state);

        int maxSlot = Configs.AUTO_TOOL_INVENTORY.getBooleanValue() ? 36 : 9;
        for (int i = 0; i < maxSlot; i++) {
            if (i == 40) continue;
            ItemStack stack = inventory.getItem(i);
            float speed = getSpeed(stack, state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static void restoreSwappedTool(Minecraft client) {
        if (client.player == null || swappedFromSlot == -1 || swappedHotbarSlot == -1) return;
        InventoryHelper.selectHotbarSlot(client, swappedHotbarSlot);
        InventoryHelper.swapWithHotbar(client, swappedFromSlot, swappedHotbarSlot);
    }

    private static void reset() {
        previousSlot = -1;
        swappedFromSlot = -1;
        swappedHotbarSlot = -1;
    }

    private static float getSpeed(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) return 1.0F;
        return stack.getDestroySpeed(state);
    }
}
