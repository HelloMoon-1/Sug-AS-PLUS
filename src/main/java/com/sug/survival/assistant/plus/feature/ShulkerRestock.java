package com.sug.survival.assistant.plus.feature;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import com.sug.survival.assistant.plus.config.Configs;

import java.lang.reflect.Method;

public final class ShulkerRestock {
    private static final int SHULKER_SLOTS = 27;
    private static boolean modDetected;
    private static Item pendingItem;
    private static int pendingContainerSlot = -1;
    private static int pendingSacrificeSlot = -1;
    private static int pendingTargetSlot = -1;
    private static boolean pendingTookTarget;
    private static int pendingTicks;
    private static long lastMissingWarning;
    private static long lastFullInventoryWarning;

    private ShulkerRestock() {}

    public static void init() {
        modDetected = FabricLoader.getInstance().isModLoaded("quickshulker");
        if (!modDetected) {
            Configs.SHULKER_RESTOCK.setBooleanValue(false);
            Configs.AUTO_TOTEM_SHULKER_RESTOCK.setBooleanValue(false);
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("§6[SUG Survival Assistant] §c未检测到 Quick Shulker mod，潜影盒补货功能已禁用"));
                }
            });
        }
    }

    public static void tick(Minecraft client) {
        if (pendingItem == null) return;
        if (client.player == null || client.gameMode == null || --pendingTicks <= 0) {
            resetPending();
            return;
        }

        if (!(client.player.containerMenu instanceof ShulkerBoxMenu)) return;

        int syncId = client.player.containerMenu.containerId;
        if (pendingSacrificeSlot != -1) {
            client.gameMode.handleContainerInput(syncId, toOpenShulkerPlayerSlot(pendingSacrificeSlot), 0, ContainerInput.QUICK_MOVE, client.player);
            pendingSacrificeSlot = -1;
            return;
        }

        if (!pendingTookTarget) {
            client.gameMode.handleContainerInput(syncId, pendingContainerSlot, 0, ContainerInput.PICKUP, client.player);
            pendingTookTarget = true;
            return;
        }

        if (pendingTargetSlot == -1) {
            closeHandledScreen(client);
            resetPending();
            return;
        }
        client.gameMode.handleContainerInput(syncId, toOpenShulkerPlayerSlot(pendingTargetSlot), 0, ContainerInput.PICKUP, client.player);
        closeHandledScreen(client);
        resetPending();
    }

    public static boolean isAvailable() {
        return modDetected && Configs.SHULKER_RESTOCK.getBooleanValue();
    }

    private static boolean isModAvailable() {
        return modDetected;
    }

    public static boolean hasPendingAction() {
        return pendingItem != null;
    }

    public static boolean shouldHideScreen() {
        return pendingItem != null;
    }

    public static int countInShulkers(Minecraft client, Item targetItem) {
        if (client.player == null) return 0;
        int count = 0;
        Inventory inventory = client.player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isShulkerBox(stack)) continue;
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container == null) continue;
            NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SLOTS, ItemStack.EMPTY);
            container.copyInto(items);
            for (ItemStack slotStack : items) {
                if (slotStack.getItem() == targetItem) count += slotStack.getCount();
            }
        }
        return count;
    }

    public static boolean tryRestock(Minecraft client, Item targetItem) {
        if (!isAvailable()) return false;
        return tryRestock(client, targetItem, true, true);
    }

    public static boolean tryRestockIfModLoaded(Minecraft client, Item targetItem) {
        if (!isModAvailable()) return false;
        return tryRestock(client, targetItem, true, false);
    }

    public static boolean tryRestockWithoutSacrifice(Minecraft client, Item targetItem) {
        if (!isModAvailable()) return false;
        return tryRestock(client, targetItem, false, true);
    }

    private static boolean tryRestock(Minecraft client, Item targetItem, boolean allowSacrifice, boolean warnMissing) {
        if (pendingItem != null || client.player == null || client.gameMode == null) return false;

        Inventory inventory = client.player.getInventory();
        boolean foundAnyTarget = false;
        int emptySlot = findEmptyInventorySlot(inventory);
        boolean fullInventory = emptySlot == -1;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isShulkerBox(stack)) continue;

            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container == null) continue;

            NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SLOTS, ItemStack.EMPTY);
            container.copyInto(items);

            for (int i = 0; i < items.size(); i++) {
                ItemStack slotStack = items.get(i);
                if (slotStack.isEmpty() || slotStack.getItem() != targetItem) continue;

                foundAnyTarget = true;
                if (fullInventory && !allowSacrifice) {
                    warnFullInventory(client, targetItem);
                    return false;
                }
                int sacrificeSlot = fullInventory ? findSacrificeSlot(inventory, items, slot, targetItem) : -1;
                if (fullInventory && sacrificeSlot == -1) continue;

                if (!openQuickShulker(slot)) return false;

                pendingItem = targetItem;
                pendingContainerSlot = i;
                pendingSacrificeSlot = sacrificeSlot;
                pendingTargetSlot = fullInventory ? sacrificeSlot : emptySlot;
                pendingTookTarget = false;
                pendingTicks = 20;
                return true;
            }
        }

        if (!foundAnyTarget && warnMissing) warnMissing(client, targetItem);
        return false;
    }

    private static int findEmptyInventorySlot(Inventory inventory) {
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) return slot;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private static int findSacrificeSlot(Inventory inventory, NonNullList<ItemStack> shulkerItems, int shulkerSlot, Item targetItem) {
        int mainFallback = findSacrificeSlotInRange(inventory, shulkerItems, shulkerSlot, targetItem, 9, 36, false);
        if (mainFallback != -1) return mainFallback;
        int hotbarFallback = findSacrificeSlotInRange(inventory, shulkerItems, shulkerSlot, targetItem, 0, 9, false);
        if (hotbarFallback != -1) return hotbarFallback;
        int mainAny = findSacrificeSlotInRange(inventory, shulkerItems, shulkerSlot, targetItem, 9, 36, true);
        if (mainAny != -1) return mainAny;
        return findSacrificeSlotInRange(inventory, shulkerItems, shulkerSlot, targetItem, 0, 9, true);
    }

    private static int findSacrificeSlotInRange(Inventory inventory, NonNullList<ItemStack> shulkerItems, int shulkerSlot, Item targetItem, int start, int end, boolean allowNonBlock) {
        for (int slot = start; slot < end; slot++) {
            if (slot == shulkerSlot) continue;
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.getItem() == targetItem || isShulkerBox(stack)) continue;
            if (!canInsertIntoShulker(shulkerItems, stack)) continue;
            if (allowNonBlock || stack.getItem() instanceof BlockItem) return slot;
        }
        return -1;
    }

    private static boolean canInsertIntoShulker(NonNullList<ItemStack> shulkerItems, ItemStack stack) {
        for (ItemStack shulkerStack : shulkerItems) {
            if (shulkerStack.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(shulkerStack, stack) && shulkerStack.getCount() < shulkerStack.getOrDefault(DataComponents.MAX_STACK_SIZE, 64)) return true;
        }
        return false;
    }

    private static int toOpenShulkerPlayerSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) return inventorySlot + 54;
        return inventorySlot + 18;
    }

    private static boolean openQuickShulker(int shulkerSlot) {
        try {
            Class<?> packetClass = Class.forName("net.kyrptonaught.quickshulker.network.OpenShulkerPacket");
            Method sendOpenPacket = packetClass.getMethod("sendOpenPacket", int.class);
            sendOpenPacket.invoke(null, shulkerSlot);
            return true;
        } catch (ReflectiveOperationException e) {
            Configs.SHULKER_RESTOCK.setBooleanValue(false);
            return false;
        }
    }

    private static void closeHandledScreen(Minecraft client) {
        if (client.player != null) client.player.closeContainer();
    }

    private static void resetPending() {
        pendingItem = null;
        pendingContainerSlot = -1;
        pendingSacrificeSlot = -1;
        pendingTargetSlot = -1;
        pendingTookTarget = false;
        pendingTicks = 0;
    }

    private static void warnMissing(Minecraft client, Item targetItem) {
        if (client.player == null || client.level == null) return;
        long time = client.level.getGameTime();
        if (time - lastMissingWarning < 40) return;
        lastMissingWarning = time;
        client.player.sendSystemMessage(Component.literal("§6[SUG Survival Assistant] §c潜影盒内没有可补充的 " + targetItem.getName(new ItemStack(targetItem)).getString()));
    }

    private static void warnFullInventory(Minecraft client, Item targetItem) {
        if (client.player == null || client.level == null) return;
        long time = client.level.getGameTime();
        if (time - lastFullInventoryWarning < 40) return;
        lastFullInventoryWarning = time;
        client.player.sendSystemMessage(Component.literal("§6[SUG Survival Assistant] §c背包已满，无法从潜影盒补充 " + targetItem.getName(new ItemStack(targetItem)).getString()));
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }
}
