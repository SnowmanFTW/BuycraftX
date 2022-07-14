package net.buycraft.plugin.minestom.util;

import net.buycraft.plugin.data.responses.Version;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.buycraft.plugin.shared.util.VersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerLoginEvent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static net.buycraft.plugin.shared.util.VersionUtil.isVersionGreater;

public class VersionCheck {
    private final BuycraftPlugin plugin;
    private final String pluginVersion;
    private final String secret;
    private Version lastKnownVersion;
    private boolean upToDate = true;

    public VersionCheck(final BuycraftPlugin plugin, final String pluginVersion, final String secret) {
        this.plugin = plugin;
        this.pluginVersion = pluginVersion;
        this.secret = secret;
        onPostLogin();
    }

    public void verify() throws IOException {
        if (pluginVersion.endsWith("-SNAPSHOT")) {
            return; // SNAPSHOT versions ignore updates
        }

        lastKnownVersion = VersionUtil.getVersion(plugin.getHttpClient(), "bukkit", secret);
        if (lastKnownVersion == null) {
            return;
        }

        // Compare versions
        String latestVersionString = lastKnownVersion.getVersion();
        if (!latestVersionString.equals(pluginVersion)) {
            upToDate = !isVersionGreater(pluginVersion, latestVersionString);
            if (!upToDate) {
                plugin.getLogger().info(plugin.getI18n().get("update_available", lastKnownVersion.getVersion()));
            }
        }
    }

    public void onPostLogin() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> {
            if (event.getPlayer().hasPermission("buycraft.admin") && !upToDate) {
                plugin.getPlatform().executeAsyncLater(() ->
                        event.getPlayer().sendMessage(Component.text(plugin.getI18n().get("update_available", lastKnownVersion.getVersion())).color(TextColor.color(255,255,85))), 3, TimeUnit.SECONDS);
            }
        });
    }

    public Version getLastKnownVersion() {
        return this.lastKnownVersion;
    }


}
