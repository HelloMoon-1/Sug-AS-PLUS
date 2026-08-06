package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class AutoTotem {
    private AutoTotem() {
    }

    public static void tick(Minecraft client) {
        if (!Configs.AUTO_TOTEM.getBooleanValue() || client.player == null) {
            return;
        }
        if (!InventoryHelper.canOperateInventory(client)) {
            return;
        }

        int slot = InventoryHelper.findItemInInventory(client, Items.TOTEM_OF_UNDYING);
        if (client.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING && slot >= 0) {
            InventoryHelper.swapToOffhand(client, slot);
            return;
        }

        if (!Configs.AUTO_TOTEM_SHULKER_RESTOCK.getBooleanValue() || ShulkerRestock.hasPendingAction()) {
            return;
        }
        if (countInventoryTotems(client) >= Configs.AUTO_TOTEM_MIN_TOTEMS.getIntegerValue()) {
            return;
        }
        ShulkerRestock.tryRestockIfModLoaded(client, Items.TOTEM_OF_UNDYING);
    }

    private static int countInventoryTotems(Minecraft client) {
        int count = 0;
        if (client.player == null) {
            return 0;
        }
        if (client.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
            count = client.player.getOffhandItem().getCount();
        }
        for (int slot = 0; slot < 36; slot++) {
            if (client.player.getInventory().getItem(slot).getItem() == Items.TOTEM_OF_UNDYING) {
                count += client.player.getInventory().getItem(slot).getCount();
            }
        }
        return count;
    }
}
