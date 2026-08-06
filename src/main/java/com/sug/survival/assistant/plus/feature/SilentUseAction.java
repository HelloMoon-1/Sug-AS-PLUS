package com.sug.survival.assistant.plus.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class SilentUseAction {
    private static Item pendingItem;
    private static int pendingTicks;

    private SilentUseAction() {
    }

    public static void tick() {
        if (pendingItem == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        if (InventoryHelper.findItemInInventory(client, pendingItem) != -1) {
            Item item = pendingItem;
            pendingItem = null;
            useItem(item);
            return;
        }

        if (--pendingTicks <= 0) {
            pendingItem = null;
        }
    }

    public static void usePearl() {
        useItem(Items.ENDER_PEARL);
    }

    public static void useFirework() {
        useItem(Items.FIREWORK_ROCKET);
    }

    private static void useItem(Item item) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        int itemSlot = InventoryHelper.findItemInInventory(client, item);
        if (itemSlot == -1) {
            if (pendingItem != null || ShulkerRestock.hasPendingAction()) {
                return;
            }
            if (!ShulkerRestock.tryRestock(client, item)) {
                return;
            }
            pendingItem = item;
            pendingTicks = 20;
            return;
        }

        int selectedSlot = InventoryHelper.getSelectedHotbarSlot(client);
        boolean alreadySelected = itemSlot == selectedSlot;
        if (!alreadySelected) {
            InventoryHelper.swapWithHotbar(client, itemSlot, selectedSlot);
        }
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        if (!alreadySelected) {
            InventoryHelper.swapWithHotbar(client, itemSlot, selectedSlot);
        }
    }
}
