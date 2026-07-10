package com.sug.survival.assistant.plus.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.sug.survival.assistant.plus.config.Configs;

public final class AutoEat {
    private static int previousSlot = -1;
    private static int foodSlot = -1;
    private static boolean autoUsing;
    private static boolean userUseDown;
    private static boolean updatingUseKey;

    private AutoEat() {
    }

    public static void tick(Minecraft client) {
        if (!Configs.AUTO_EAT.getBooleanValue() || client.player == null || client.gameMode == null || client.screen != null) {
            stop(client);
            return;
        }

        boolean shouldEat = client.player.getFoodData().getFoodLevel() <= Configs.AUTO_EAT_HUNGER.getIntegerValue()
                || client.player.getHealth() <= Configs.AUTO_EAT_HEALTH.getDoubleValue();
        if (!shouldEat) {
            stop(client);
            return;
        }

        int nextFoodSlot = findFoodSlot(client);
        if (nextFoodSlot == -1) {
            stop(client);
            return;
        }

        if (!autoUsing) {
            previousSlot = InventoryHelper.getSelectedHotbarSlot(client);
            userUseDown = client.options.keyUse.isDown();
            autoUsing = true;
        }
        if (foodSlot != nextFoodSlot || InventoryHelper.getSelectedHotbarSlot(client) != nextFoodSlot) {
            foodSlot = nextFoodSlot;
            InventoryHelper.selectHotbarSlot(client, foodSlot);
        }
        setUseKey(client, true);
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
    }

    public static boolean handleUseKey(boolean pressed) {
        if (!autoUsing || updatingUseKey) return false;
        userUseDown = pressed;
        return true;
    }

    private static void stop(Minecraft client) {
        if (!autoUsing) return;
        setUseKey(client, userUseDown);
        if (client.player != null && previousSlot != -1) {
            InventoryHelper.selectHotbarSlot(client, previousSlot);
        }
        previousSlot = -1;
        foodSlot = -1;
        autoUsing = false;
        userUseDown = false;
    }

    private static void setUseKey(Minecraft client, boolean pressed) {
        updatingUseKey = true;
        client.options.keyUse.setDown(pressed);
        updatingUseKey = false;
    }

    private static int findFoodSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.get(DataComponents.FOOD) != null) return i;
        }
        return -1;
    }
}
