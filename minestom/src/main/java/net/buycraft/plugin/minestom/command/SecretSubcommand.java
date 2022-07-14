package net.buycraft.plugin.minestom.command;

import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.data.responses.ServerInformation;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.timer.TaskSchedule;

import java.io.IOException;
import java.util.logging.Level;

public class SecretSubcommand implements Subcommand{

    private final BuycraftPlugin plugin;

    public SecretSubcommand(final BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        if (!(sender instanceof ConsoleSender)) {
            sender.sendMessage(Component.text(plugin.getI18n().get("secret_console_only")).color(TextColor.fromHexString("red")));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text(plugin.getI18n().get("secret_need_key")).color(TextColor.fromHexString("red")));
            return;
        }

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            String currentKey = plugin.getConfiguration().getServerKey();
            BuyCraftAPI client = BuyCraftAPI.create(args[0], plugin.getHttpClient());
            try {
                plugin.updateInformation(client);
            } catch (Exception e) {
                plugin.getLogger().error("Unable to verify secret", e);
                sender.sendMessage(Component.text(plugin.getI18n().get("secret_does_not_work")).color(TextColor.fromHexString("red")));
                return;
            }

            ServerInformation information = plugin.getServerInformation();
            plugin.setApiClient(client);
            plugin.getConfiguration().setServerKey(args[0]);
            try {
                plugin.saveConfiguration();
            } catch (IOException e) {
                sender.sendMessage(Component.text(plugin.getI18n().get("secret_cant_be_saved")).color(TextColor.fromHexString("red")));
            }

            sender.sendMessage(Component.text(plugin.getI18n().get("secret_success",
                    information.getServer().getName(), information.getAccount().getName())).color(TextColor.fromHexString("green")));

            boolean repeatChecks = false;
            if (currentKey.equals("INVALID")) {
                repeatChecks = true;
            }

            plugin.getDuePlayerFetcher().run(repeatChecks);
        }, TaskSchedule.immediate(), TaskSchedule.stop());
    }

    @Override
    public String getDescription() {
        return plugin.getI18n().get("usage_secret");
    }
}
