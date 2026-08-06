package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoTool {
    private static int previousSlot = -1;
    private static int swappedFromSlot = -1;
    private static int swappedHotbarSlot = -1;
    private static int autoSelectedSlot = -1;
    private static BlockPos lastBlockPos;
    private static BlockState lastBlockState;

    private AutoTool() {
    }

    public static void tick(Minecraft client) {
        if (!Configs.AUTO_TOOL.getBooleanValue()
                || client.player == null
                || client.level == null
                || client.gui.screen() != null) {
            if (client.player != null && previousSlot != -1) {
                finishMining(client, true);
            } else {
                reset();
            }
            return;
        }

        if (!client.options.keyAttack.isDown() && previousSlot != -1) {
            stopMining();
        }
    }

    public static void beforeBlockAttack(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (!Configs.AUTO_TOOL.getBooleanValue()
                || client.player == null
                || client.level == null
                || client.gui.screen() != null) {
            return;
        }

        int currentSlot = InventoryHelper.getSelectedHotbarSlot(client);
        if (previousSlot != -1 && autoSelectedSlot != -1 && currentSlot != autoSelectedSlot) {
            finishMining(client, false, currentSlot);
            currentSlot = InventoryHelper.getSelectedHotbarSlot(client);
        }

        if (previousSlot == -1) {
            previousSlot = currentSlot;
            autoSelectedSlot = currentSlot;
        }

        BlockState state = client.level.getBlockState(pos);
        if (pos.equals(lastBlockPos) && state == lastBlockState) {
            return;
        }
        lastBlockPos = pos.immutable();
        lastBlockState = state;

        if (swappedFromSlot != -1) {
            if (currentSlot != swappedHotbarSlot) {
                InventoryHelper.selectHotbarSlot(client, swappedHotbarSlot);
            }
            currentSlot = swappedHotbarSlot;
            if (findBestToolSlot(client, state, currentSlot) == currentSlot) {
                return;
            }
            restoreSwappedTool(client);
            swappedFromSlot = -1;
            swappedHotbarSlot = -1;
        }

        currentSlot = InventoryHelper.getSelectedHotbarSlot(client);
        int bestSlot = findBestToolSlot(client, state, currentSlot);
        if (bestSlot < 0 || bestSlot == currentSlot) {
            return;
        }

        if (bestSlot < 9) {
            if (currentSlot != bestSlot) {
                InventoryHelper.selectHotbarSlot(client, bestSlot);
            }
            autoSelectedSlot = bestSlot;
            return;
        }

        InventoryHelper.swapWithHotbar(client, bestSlot, currentSlot);
        swappedFromSlot = bestSlot;
        swappedHotbarSlot = currentSlot;
        autoSelectedSlot = swappedHotbarSlot;
        InventoryHelper.selectHotbarSlot(client, swappedHotbarSlot);
    }

    public static void stopMining() {
        Minecraft client = Minecraft.getInstance();
        if (previousSlot == -1) {
            return;
        }
        finishMining(client, Configs.AUTO_TOOL_SWITCH_BACK.getBooleanValue());
    }

    private static void finishMining(Minecraft client, boolean restoreSelectedSlot) {
        finishMining(client, restoreSelectedSlot, -1);
    }

    private static void finishMining(Minecraft client, boolean restoreSelectedSlot, int preservedSlot) {
        restoreSwappedTool(client);
        if (client.player != null) {
            if (restoreSelectedSlot && previousSlot != -1) {
                InventoryHelper.selectHotbarSlot(client, previousSlot);
            } else if (preservedSlot != -1) {
                InventoryHelper.selectHotbarSlot(client, preservedSlot);
            }
        }
        reset();
    }

    private static int findBestToolSlot(Minecraft client, BlockState state, int currentSlot) {
        Inventory inventory = client.player.getInventory();
        int bestSlot = currentSlot;
        float bestSpeed = getSpeed(inventory.getItem(bestSlot), state);
        int maxSlot = Configs.AUTO_TOOL_INVENTORY.getBooleanValue() ? 36 : 9;
        for (int i = 0; i < maxSlot; i++) {
            if (i == 40) {
                continue;
            }
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
        if (client.player == null || swappedFromSlot == -1 || swappedHotbarSlot == -1) {
            return;
        }
        InventoryHelper.selectHotbarSlot(client, swappedHotbarSlot);
        InventoryHelper.swapWithHotbar(client, swappedFromSlot, swappedHotbarSlot);
    }

    private static void reset() {
        previousSlot = -1;
        swappedFromSlot = -1;
        swappedHotbarSlot = -1;
        autoSelectedSlot = -1;
        lastBlockPos = null;
        lastBlockState = null;
    }

    private static float getSpeed(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) {
            return 1.0F;
        }
        return stack.getDestroySpeed(state);
    }
}
