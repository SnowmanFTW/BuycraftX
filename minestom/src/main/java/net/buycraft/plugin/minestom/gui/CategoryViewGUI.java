package net.buycraft.plugin.minestom.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.buycraft.plugin.minestom.tasks.SendCheckoutLink;
import net.buycraft.plugin.minestom.util.GUIUtil;
import net.buycraft.plugin.data.Category;
import net.buycraft.plugin.data.Package;
import net.buycraft.plugin.data.responses.Listing;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;

import java.text.NumberFormat;
import java.util.*;

import static net.buycraft.plugin.minestom.util.GUIUtil.withName;

public class CategoryViewGUI {

    private final BuycraftPlugin plugin;
    private final Map<Integer, List<net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl>> categoryMenus = new HashMap<>();

    public CategoryViewGUI(final BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    private static int calculatePages(Category category) {
        int pagesWithSubcats = (int) Math.ceil(category.getSubcategories().size() / 9D);
        int pagesWithPackages = (int) Math.ceil(category.getPackages().size() / 36D);
        return Math.max(pagesWithSubcats, pagesWithPackages);
    }

    public net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl getFirstPage(Category category) {
        List<net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl> guis = categoryMenus.get(category.getId());
        if (guis == null) return null;
        return Iterables.getFirst(guis, null);
    }

    public void update() {
        if (plugin.getApiClient() == null || plugin.getServerInformation() == null) {
            plugin.getLogger().info("No secret key available (or no server information), so can't update inventories.");
            return;
        }

        Listing listing = plugin.getListingUpdateTask().getListing();
        if (listing == null) {
            plugin.getLogger().warn("No listing found, so can't update inventories.");
            return;
        }

        List<Integer> foundIds = new ArrayList<>();
        for (Category category : listing.getCategories()) {
            foundIds.add(category.getId());
            for (Category category1 : category.getSubcategories()) {
                foundIds.add(category1.getId());
            }
        }

        for (Iterator<Map.Entry<Integer, List<net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl>>> it = categoryMenus.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, List<net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl>> next = it.next();
            if (!foundIds.contains(next.getKey())) {
                for (net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl gui : next.getValue()) {
                    gui.destroy();
                }
                it.remove();
            }
        }

        for (Category category : listing.getCategories()) {
            doUpdate(null, category);
        }
    }

    private void doUpdate(Category parent, Category category) {
        List<net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl> pages = categoryMenus.get(category.getId());
        if (pages == null) {
            pages = new ArrayList<>();
            categoryMenus.put(category.getId(), pages);
            for (int i = 0; i < calculatePages(category); i++) {
                net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl gui = new net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl(parent != null ? parent.getId() : null, i, category);
                gui.registerEvents();
                pages.add(gui);
            }
        } else {
            int allPages = calculatePages(category);
            int toRemove = pages.size() - allPages;
            if (toRemove > 0) {
                List<net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl> prune = pages.subList(pages.size() - toRemove, pages.size());
                for (net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl gui : prune) {
                    gui.destroy();
                }
                prune.clear();
            } else if (toRemove < 0) {
                int toAdd = -toRemove;
                for (int i = 0; i < toAdd; i++) {
                    net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl gui = new net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl(parent != null ? parent.getId() : null, pages.size(), category);
                    gui.registerEvents();
                    pages.add(gui);
                }
            }

            for (int i = 0; i < pages.size(); i++) {
                net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl gui = pages.get(i);
                if (gui.requiresResize(category)) {
                    net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl tmpGui = new net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl(parent != null ? parent.getId() : null, i, category);
                    tmpGui.registerEvents();
                    pages.set(i, tmpGui);

                    GUIUtil.replaceInventory(gui.inventory, tmpGui.inventory);
                } else {
                    gui.update(category);
                }
            }
        }

        for (Category category1 : category.getSubcategories()) {
            doUpdate(category, category1);
        }
    }

    public class GUIImpl {
        private final Inventory inventory;
        private final Integer parentId;
        private final int page;
        private Category category;

        public GUIImpl(Integer parentId, int page, Category category) {
            this.inventory = new Inventory(InventoryType.valueOf("CHEST_" + calculateSize(category, page) / 9 + "_ROW"), GUIUtil.trimName("Tebex: " + category.getName()));
            this.parentId = parentId;
            this.page = page;
            update(category);
        }

        public void registerEvents(){
            onInventoryClick();
        }

        private int calculateSize(Category category, int page) {
            // TODO: Calculate this amount based on no of packages
            int needed = 45; // bottom row
            if (!category.getSubcategories().isEmpty()) {
                int pagesWithSubcats = (int) Math.ceil(category.getSubcategories().size() / 9D);
                if (pagesWithSubcats >= page) {
                    // more pages exist
                    needed += 9;
                }
            }

            // if we show subcategories, we can't show as many pages
            return needed;
        }

        public boolean requiresResize(Category category) {
            return calculateSize(category, page) != inventory.getSize();
        }

        public void destroy() {
            closeAll();
        }

        public void closeAll() {
            for (Player entity : ImmutableList.copyOf(inventory.getViewers())) {
                entity.closeInventory();
            }
        }

        public void open(Player player) {
            player.openInventory(inventory);
        }

        public void update(Category category) {
            this.category = category;
            inventory.clear();

            List<List<Category>> subcatPartition;
            if (!category.getSubcategories().isEmpty()) {
                subcatPartition = Lists.partition(category.getSubcategories(), 45);
                if (subcatPartition.size() - 1 >= page) {
                    List<Category> subcats = subcatPartition.get(page);
                    subcats.sort(Comparator.comparingInt(Category::getOrder));
                    for (int i = 0; i < subcats.size(); i++) {
                        Category subcat = subcats.get(i);

                        ItemStack stack = GUIUtil.createItemFromMaterialString(subcats.get(i).getGuiItem());
                        if (stack == null || stack.isAir()) {
                            stack = ItemStack.of(Material.CHEST);
                        }

                        inventory.setItemStack(i, withName(stack, Component.text(subcat.getName()).color(NamedTextColor.YELLOW)));
                    }
                }
            } else {
                subcatPartition = ImmutableList.of();
            }

            List<List<Package>> packagePartition = Lists.partition(category.getPackages(), 36);
            int base = subcatPartition.isEmpty() ? 0 : 9;

            if (packagePartition.size() - 1 >= page) {
                List<Package> packages = packagePartition.get(page);
                packages.sort(Comparator.comparingInt(Package::getOrder));
                for (int i = 0; i < packages.size(); i++) {
                    Package p = packages.get(i);

                    ItemStack stack =GUIUtil.createItemFromMaterialString(p.getGuiItem());
                    if (stack == null || stack.isAir()) {
                        stack = ItemStack.of(Material.PAPER);
                    }

                    stack = stack.withDisplayName(Component.text(p.getName()).color(NamedTextColor.GREEN));

                    List<Component> lore = new ArrayList<>();
                    // Price
                    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
                    format.setCurrency(Currency.getInstance(plugin.getServerInformation().getAccount().getCurrency().getIso4217()));

                    Component price = Component.text(plugin.getI18n().get("price") + ": ").color(NamedTextColor.GRAY).append(Component.text(format.format(p.getEffectivePrice()))).color(NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD);
                    lore.add(price);

                    if (p.getSale() != null && p.getSale().isActive()) {
                        lore.add(Component.text(plugin.getI18n().get("amount_off", format.format(p.getSale().getDiscount()))).color(NamedTextColor.RED));
                    }

                    stack = stack.withLore(lore);

                    inventory.setItemStack(base + i, stack);
                }
            }

            // Determine if we should draw a previous or next button
            int bottomBase = base + 36;
            if (page > 0) {
                // Definitely draw a previous button
                inventory.setItemStack(bottomBase + 1, withName(ItemStack.of(Material.NETHER_STAR), Component.text(plugin.getI18n().get("previous_page")).color(NamedTextColor.AQUA)));
            }

            if (subcatPartition.size() - 1 > page || packagePartition.size() - 1 > page) {
                // Definitely draw a next button
                inventory.setItemStack(bottomBase + 7, withName(ItemStack.of(Material.NETHER_STAR), Component.text(plugin.getI18n().get("next_page")).color(NamedTextColor.AQUA)));
            }

            // Draw a parent or "view all categories" button
            ItemStack parent = ItemStack.of(Material.WRITABLE_BOOK);
            parent = parent.withDisplayName(Component.text((parentId == null ? plugin.getI18n().get("view_all_categories") : plugin.getI18n().get("back_to_parent"))).color(NamedTextColor.GRAY));
            inventory.setItemStack(bottomBase + 4, parent);
        }

        public void onInventoryClick() {
            MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent.class, event -> {
                Inventory clickedInventory = event.getInventory();
                if(event.getSlot() == -999) return;
                if (clickedInventory != null && Objects.equals(inventory, clickedInventory)) {
                    event.setCancelled(true);

                    final Player player = event.getPlayer();
                    if (category == null) return;
                    ItemStack stack = clickedInventory.getItemStack(event.getSlot());
                    if (stack.isAir()) {
                        return;
                    }

                    TextComponent displayName = (TextComponent) stack.getDisplayName();
                    if (displayName.toString().toLowerCase().contains("yellow")) {
                        // Subcategory was clicked
                        for (final Category category1 : category.getSubcategories()) {
                            if (category1.getName().equals(displayName.content())) {
                                final net.buycraft.plugin.minestom.gui.CategoryViewGUI.GUIImpl gui = getFirstPage(category1);
                                if (gui == null) {
                                    player.sendMessage(Component.text(plugin.getI18n().get("nothing_in_category")).color(NamedTextColor.RED));
                                    return;
                                }
                                gui.open(player);
                                return;
                            }
                        }
                    } else if (displayName.toString().toLowerCase().contains("green")) {
                        // Package was clicked
                        for (Package aPackage : category.getPackages()) {
                            if (aPackage.getName().equals(displayName.content())) {
                                GUIUtil.closeInventoryLater(player);
                                MinecraftServer.getSchedulerManager().scheduleTask(new SendCheckoutLink(plugin, aPackage.getId(), player), TaskSchedule.immediate(), TaskSchedule.stop(), ExecutionType.ASYNC);
                                return;
                                }
                        }
                    } else if (displayName.content().equals(plugin.getI18n().get("previous_page"))) {
                        categoryMenus.get(category.getId()).get(page - 1).open(player);
                    } else if (displayName.content().equals(plugin.getI18n().get("next_page"))) {
                        categoryMenus.get(category.getId()).get(page + 1).open(player);
                    } else if (stack.material() == Material.WRITABLE_BOOK) {
                        if (parentId != null) {
                            categoryMenus.get(parentId).get(0).open(player);
                        } else {
                            plugin.getViewCategoriesGUI().open(player);
                        }
                    }
                } else if (event.getInventory() == inventory) {
                    event.setCancelled(true);
                }
            });
        }
    }
}
