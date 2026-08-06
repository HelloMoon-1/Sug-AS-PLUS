package com.sug.survival.assistant.plus.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class InventoryHelper {
    private InventoryHelper() {
    }

    static boolean canOperateInventory(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            return false;
        }
        if (client.gui.screen() == null) {
            return true;
        }
        if (ShulkerRestock.hasPendingAction()) {
            return true;
        }
        return client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen
                || client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
    }

    static int findItemInInventory(Minecraft client, Item item) {
        if (client.player == null) {
            return -1;
        }
        Inventory inventory = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    static int toScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return inventorySlot + 36;
        }
        return inventorySlot;
    }

    static void swapToOffhand(Minecraft client, int inventorySlot) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        client.gameMode.handleContainerInput(
                client.player.containerMenu.containerId,
                toScreenSlot(inventorySlot),
                40,
                ContainerInput.SWAP,
                client.player
        );
    }

    static void swapWithHotbar(Minecraft client, int inventorySlot, int hotbarSlot) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        client.gameMode.handleContainerInput(
                client.player.containerMenu.containerId,
                toScreenSlot(inventorySlot),
                hotbarSlot,
                ContainerInput.SWAP,
                client.player
        );
    }

    static int getSelectedHotbarSlot(Minecraft client) {
        if (client.player == null) {
            return 0;
        }
        return client.player.getInventory().getSelectedSlot();
    }

    static void selectHotbarSlot(Minecraft client, int slot) {
        if (client.player == null || slot < 0 || slot > 8) {
            return;
        }
        client.player.getInventory().setSelectedSlot(slot);
    }
}
