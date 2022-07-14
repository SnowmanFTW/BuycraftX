package net.buycraft.plugin.minestom.command;

import net.buycraft.plugin.bukkit.BuycraftPluginBase;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;

public class ForceCheckSubcommand implements Subcommand {
    private final BuycraftPlugin plugin;

    public ForceCheckSubcommand(final BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage(Component.text(plugin.getI18n().get("no_params")).color(TextColor.fromHexString("red")));
            return;
        }

        if (plugin.getApiClient() == null) {
            sender.sendMessage(Component.text(plugin.getI18n().get("need_secret_key")).color(TextColor.fromHexString("red")));
            return;
        }

        if (plugin.getDuePlayerFetcher().inProgress()) {
            sender.sendMessage(Component.text(plugin.getI18n().get("already_checking_for_purchases")).color(TextColor.fromHexString("red")));
            return;
        }

        MinecraftServer.getSchedulerManager().submitTask(() -> {
            plugin.getDuePlayerFetcher().run(false);
            return TaskSchedule.stop();
        }, ExecutionType.ASYNC);
        sender.sendMessage(Component.text(plugin.getI18n().get("forcecheck_queued")).color(TextColor.fromHexString("green")));
    }

    @Override
    public String getDescription() {
        return plugin.getI18n().get("usage_forcecheck");
    }
}
