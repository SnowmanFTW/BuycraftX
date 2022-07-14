package net.buycraft.plugin.minestom.tasks;

import net.buycraft.plugin.minestom.BuycraftPlugin;

public class GUIUpdateTask implements Runnable {
    private final BuycraftPlugin plugin;

    public GUIUpdateTask(final BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getViewCategoriesGUI().update();
        plugin.getCategoryViewGUI().update();
    }
}
