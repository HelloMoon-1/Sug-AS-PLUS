package com.sug.survival.assistant.plus.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.sug.survival.assistant.plus.config.Configs;

public final class AutoEat {
    private static int previousSlot = -1;

    private AutoEat() {
    }

    public static void tick(Minecraft client) {
        if (!Configs.AUTO_EAT.getBooleanValue() || client.player == null || client.gameMode == null) return;
        if (client.screen != null) return;

        boolean shouldEat = client.player.getFoodData().getFoodLevel() <= Configs.AUTO_EAT_HUNGER.getIntegerValue()
                || client.player.getHealth() <= Configs.AUTO_EAT_HEALTH.getDoubleValue();

        if (!shouldEat) {
            if (previousSlot != -1 && !client.options.keyUse.isDown()) {
                InventoryHelper.selectHotbarSlot(client, previousSlot);
                previousSlot = -1;
            }
            return;
        }

        int foodSlot = findFoodSlot(client);
        if (foodSlot == -1) return;

        if (previousSlot == -1) previousSlot = InventoryHelper.getSelectedHotbarSlot(client);
        InventoryHelper.selectHotbarSlot(client, foodSlot);
        client.options.keyUse.setDown(true);
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
    }

    private static int findFoodSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.get(DataComponents.FOOD) != null) return i;
        }
        return -1;
    }
}
