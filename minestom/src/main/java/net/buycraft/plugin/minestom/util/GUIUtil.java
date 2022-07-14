package net.buycraft.plugin.minestom.util;

import com.google.common.collect.ImmutableList;
import net.buycraft.plugin.bukkit.BuycraftPluginBase;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class GUIUtil {
    private static BuycraftPlugin plugin;

    private GUIUtil() {
    }

    public static void setPlugin(BuycraftPlugin plugin) {
        if (net.buycraft.plugin.minestom.util.GUIUtil.plugin != null) {
            throw new IllegalStateException("Plugin already set");
        }
        net.buycraft.plugin.minestom.util.GUIUtil.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public static void closeInventoryLater(final Player player) {
        player.closeInventory();
    }

    public static void replaceInventory(Inventory oldInv, Inventory newInv) {
        // getViewers() is updated as we remove players, so we need to make a copy
        for (Player entity : ImmutableList.copyOf(oldInv.getViewers())) {
            entity.openInventory(newInv);
        }
    }

    public static Inventory getClickedInventory() {
        AtomicReference<Inventory> inv = null;
        MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent.class, event -> {
            if (event.getSlot() < 0) return;

            inv.set(event.getInventory());
        });
        return inv.get();
    }

    public static ItemStack withName(ItemStack stack, Component name) {
        return stack.withDisplayName(name);
    }

    public static String trimName(String name) {
        if (name.length() <= 32) return name;
        return name.substring(0, 29) + "...";
    }

    public static ItemStack createItemFromMaterialString(String materialData) {
        if (materialData == null || materialData.trim().length() <= 0) return null;

        Material material = Material.fromNamespaceId(materialData);

        if (material == null) return null;
        return ItemStack.of(material);
    }
}
