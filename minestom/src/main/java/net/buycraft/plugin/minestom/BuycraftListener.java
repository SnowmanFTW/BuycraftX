package net.buycraft.plugin.minestom;

import net.buycraft.plugin.data.QueuedPlayer;
import net.buycraft.plugin.data.ServerEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerCommandEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;

import java.util.Date;

public class BuycraftListener {
    private final BuycraftPlugin plugin;

    public BuycraftListener(final BuycraftPlugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }

    public void registerEvents(){
        onPlayerCommandPreprocess();
        onPlayerJoin();
        onPlayerQuit();
    }

    private void onPlayerJoin(){
        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> {
            if (plugin.getApiClient() == null) {
                return;
            }

            plugin.getServerEventSenderTask().queueEvent(new ServerEvent(
                    event.getPlayer().getUuid().toString().replace("-", ""),
                    event.getPlayer().getUsername(),
                    event.getPlayer().getPlayerConnection().getRemoteAddress().toString(),
                    ServerEvent.JOIN_EVENT,
                    new Date()
            ));

            QueuedPlayer qp = plugin.getDuePlayerFetcher().fetchAndRemoveDuePlayer(event.getPlayer().getUsername());
            if (qp != null) {
                plugin.getPlayerJoinCheckTask().queue(qp);
            }
        });
    }

    public void onPlayerCommandPreprocess() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerCommandEvent.class, event -> {
            if (!plugin.getConfiguration().isDisableBuyCommand()) {
                for (String s : plugin.getConfiguration().getBuyCommandName()) {
                    if (event.getCommand().equalsIgnoreCase(s) ||
                            event.getCommand().regionMatches(true, 0, s + " ", 0, s.length() + 1)) {
                        event.setCancelled(true);
                        plugin.getViewCategoriesGUI().open(event.getPlayer());
                    }
                }
            }
        });
    }

    private void onPlayerQuit(){
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            if (plugin.getApiClient() == null) {
                return;
            }

            plugin.getServerEventSenderTask().queueEvent(new ServerEvent(
                    event.getPlayer().getUuid().toString().replace("-", ""),
                    event.getPlayer().getUsername(),
                    event.getPlayer().getPlayerConnection().getRemoteAddress().toString(),
                    ServerEvent.LEAVE_EVENT,
                    new Date()
            ));
        });
    }
}
