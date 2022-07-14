package net.buycraft.plugin.minestom.tasks;

import net.buycraft.plugin.bukkit.BuycraftPluginBase;
import net.buycraft.plugin.data.responses.CheckoutUrlResponse;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

import java.io.IOException;

public class SendCheckoutLink implements Runnable {
    private BuycraftPlugin plugin;
    private int id;
    private Player player;
    private Boolean isCategory;
    private CommandSender sender;

    public SendCheckoutLink(BuycraftPlugin plugin, int id, Player p) {
        this.plugin = plugin;
        this.id = id;
        this.player = p;
        this.isCategory = false;
        this.sender = null;
    }

    public SendCheckoutLink(BuycraftPlugin plugin, int id, Player p, boolean isCategory, CommandSender sender) {
        this.plugin = plugin;
        this.id = id;
        this.player = p;
        this.isCategory = isCategory;
        this.sender = sender;
    }

    @Override
    public void run() {
        CheckoutUrlResponse response;
        try {
            if (!isCategory) {
                response = plugin.getApiClient().getCheckoutUri(player.getUsername(), id).execute().body();
            } else {
                response = plugin.getApiClient().getCategoryUri(player.getUsername(), id).execute().body();
            }
        } catch (IOException e) {
            if (sender == null)
                player.sendMessage(Component.text(plugin.getI18n().get("cant_check_out") + " " + e.getMessage()).color(TextColor.fromHexString("red")));
            else
                sender.sendMessage(Component.text(plugin.getI18n().get("cant_check_out") + " " + e.getMessage()).color(TextColor.fromHexString("red")));
            return;
        }
        if (!isCategory) {
            player.sendMessage(Component.text("                                            ").decorate(TextDecoration.STRIKETHROUGH));
            player.sendMessage(Component.text(plugin.getI18n().get("to_buy_this_package")).color(NamedTextColor.GREEN));
            player.sendMessage(Component.text(response.getUrl()).color(NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED));
            player.sendMessage(Component.text("                                            ").decorate(TextDecoration.STRIKETHROUGH));
        } else {
            player.sendMessage(Component.text("                                            ").decorate(TextDecoration.STRIKETHROUGH));
            player.sendMessage(Component.text(plugin.getI18n().get("to_view_this_category")).color(NamedTextColor.GREEN));
            player.sendMessage(Component.text(response.getUrl()).color(NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED));
            player.sendMessage(Component.text("                                            ").decorate(TextDecoration.STRIKETHROUGH));
        }
    }
}
