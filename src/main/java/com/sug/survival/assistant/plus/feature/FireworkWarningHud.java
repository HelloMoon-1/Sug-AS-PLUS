package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FireworkWarningHud {
    private static int fireworkCount;
    private static int inventoryTotemCount;
    private static int shulkerTotemCount;
    private static int updateTicks;
    private static boolean cacheValid;

    private FireworkWarningHud() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath("sug_survival_assistant_plus", "firework_warning"),
                (context, tickCounter) -> render(context)
        );
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            cacheValid = false;
            updateTicks = 0;
            return;
        }

        if (cacheValid && ++updateTicks < 5) {
            return;
        }
        updateTicks = 0;
        fireworkCount = Configs.FIREWORK_WARNING.getBooleanValue()
                ? countInventoryItems(client, Items.FIREWORK_ROCKET)
                : 0;
        if (Configs.TOTEM_WARNING.getBooleanValue()) {
            inventoryTotemCount = countInventoryItems(client, Items.TOTEM_OF_UNDYING);
            shulkerTotemCount = ShulkerRestock.countInShulkers(client, Items.TOTEM_OF_UNDYING);
        } else {
            inventoryTotemCount = 0;
            shulkerTotemCount = 0;
        }
        cacheValid = true;
    }

    private static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !cacheValid) {
            return;
        }

        int y = context.guiHeight() - 68;
        if (Configs.FIREWORK_WARNING.getBooleanValue()) {
            int threshold = Configs.FIREWORK_WARNING_THRESHOLD.getIntegerValue();
            if (fireworkCount <= threshold) {
                drawCentered(context, client, Component.literal("烟花: " + fireworkCount + " / " + threshold), y, -43691);
                y -= 10;
            }
        }

        if (Configs.TOTEM_WARNING.getBooleanValue()) {
            int total = inventoryTotemCount + shulkerTotemCount;
            int threshold = Configs.TOTEM_WARNING_THRESHOLD.getIntegerValue();
            if (total <= threshold) {
                drawCentered(
                        context,
                        client,
                        Component.literal(
                                "请及时补充图腾: " + total + " / " + threshold
                                        + "（背包 " + inventoryTotemCount + "，盒子 " + shulkerTotemCount + "）"
                        ),
                        y,
                        -43691
                );
            }
        }
    }

    private static int countInventoryItems(Minecraft client, Item item) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        ItemStack offHandStack = client.player.getOffhandItem();
        if (offHandStack.getItem() == item) {
            count += offHandStack.getCount();
        }
        return count;
    }

    private static void drawCentered(GuiGraphicsExtractor context, Minecraft client, Component text, int y, int color) {
        int x = (context.guiWidth() - client.font.width(text)) / 2;
        context.text(client.font, text, x, y, color, true);
    }
}
