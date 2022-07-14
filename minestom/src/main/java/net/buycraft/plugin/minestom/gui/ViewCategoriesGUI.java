package net.buycraft.plugin.minestom.gui;

import com.google.gson.Gson;
import net.buycraft.plugin.minestom.util.GUIUtil;
import net.buycraft.plugin.data.Category;
import net.buycraft.plugin.data.responses.Listing;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Objects;

import static net.buycraft.plugin.minestom.util.GUIUtil.withName;

public class ViewCategoriesGUI {
    private final BuycraftPlugin plugin;
    private Inventory inventory;

    public ViewCategoriesGUI(BuycraftPlugin plugin) {
        this.plugin = plugin;
        inventory = new Inventory(InventoryType.CHEST_1_ROW, GUIUtil.trimName("Tebex: " + plugin.getI18n().get("categories")));
        registerEvents();
    }

    public void registerEvents(){
        onInventoryClick();
    }

    public void open(Player player) {
        if (inventoryNeedsReloading()) {
            MinecraftServer.LOGGER.info("Inventory appears to be empty, trying to read from gui.cache file...");
            try {
                BufferedReader reader = new BufferedReader(new FileReader("extensions/Buycraft/gui.cache"));
                String jsonString = reader.readLine();
                Listing listing = new Gson().fromJson(jsonString, Listing.class);
                if (listing != null)
                    listing.order();

                inventory = new Inventory(InventoryType.CHEST_1_ROW, GUIUtil.trimName("Tebex: " +
                        plugin.getI18n().get("categories")));

                this.createInventoryFromListing(listing);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        player.openInventory(inventory);
    }

    private boolean inventoryNeedsReloading() {
        if (this.inventory == null) {
            return true;
        }
        for (ItemStack is : this.inventory.getItemStacks()) {
            if (!is.isAir()) return false;
        }
        return true;
    }

    private int roundNine(int s) {
        int sz = s - 1;
        return Math.max(9, sz - (sz % 9) + 9);
    }

    public void update() {
        inventory.clear();

        if (plugin.getApiClient() == null || plugin.getServerInformation() == null) {
            MinecraftServer.LOGGER.warn("No secret key available (or no server information), so can't update inventories.");
            return;
        }

        Listing listing = plugin.getListingUpdateTask().getListing();
        if (listing == null) {
            MinecraftServer.LOGGER.warn("No listing found, so can't update inventories.");
            return;
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("extensions/Buycraft/gui.cache"));
            bw.write(new Gson().toJson(listing));
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.createInventoryFromListing(listing);
    }

    private void createInventoryFromListing(Listing listing) {
        if (roundNine(listing.getCategories().size()) != inventory.getSize()) {
            Inventory work = new Inventory(InventoryType.valueOf("CHEST_" + roundNine(listing.getCategories().size()) / 9 + "_ROW"),
                    GUIUtil.trimName("Tebex: " + plugin.getI18n().get("categories")));
            GUIUtil.replaceInventory(inventory, work);
            inventory = work;
        }

        for (Category category : listing.getCategories()) {
            ItemStack stack = GUIUtil.createItemFromMaterialString(category.getGuiItem());
            if (stack == null || stack.isAir()) {
                stack = ItemStack.of(Material.CHEST);
            }

            inventory.addItemStack(withName(stack, Component.text(category.getName()).color(NamedTextColor.YELLOW)));
        }
    }

    public void onInventoryClick() {
        MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent.class, event -> {
            Inventory clickedInventory = event.getInventory();
            if(event.getSlot() == -999) return;

            if (clickedInventory != null && Objects.equals(inventory, clickedInventory)) {
                event.setCancelled(true);

                final Player player = event.getPlayer();

                Listing listing = plugin.getListingUpdateTask().getListing();
                if (listing == null) {
                    return;
                }

                if (event.getSlot() >= listing.getCategories().size()) {
                    return;
                }

                Category category = listing.getCategories().get(event.getSlot());
                if (category == null) {
                    return;
                }

                final CategoryViewGUI.GUIImpl gui = plugin.getCategoryViewGUI().getFirstPage(category);
                if (gui == null) {
                    player.sendMessage(Component.text(plugin.getI18n().get("nothing_in_category")).color(TextColor.fromHexString("red")));
                    return;
                }

                gui.open(player);
            } else if (event.getInventory() == inventory) {
                event.setCancelled(true);
            }
        });
    }
}
