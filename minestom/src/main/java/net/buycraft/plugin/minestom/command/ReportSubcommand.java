package net.buycraft.plugin.minestom.command;

import net.buycraft.plugin.bukkit.BuycraftPluginBase;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.buycraft.plugin.shared.util.ReportBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportSubcommand implements Subcommand {
    private final BuycraftPlugin plugin;

    public ReportSubcommand(BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final CommandSender sender, String[] args) {
        sender.sendMessage(Component.text(plugin.getI18n().get("report_wait")).color(TextColor.fromHexString("yellow")));

        MinecraftServer.getSchedulerManager().submitTask(() -> {
            ReportBuilder builder = ReportBuilder.builder()
                    .client(plugin.getHttpClient())
                    .configuration(plugin.getConfiguration())
                    .platform(plugin.getPlatform())
                    .duePlayerFetcher(plugin.getDuePlayerFetcher())
                    .ip(MinecraftServer.getServer().getAddress())
                    .port(MinecraftServer.getServer().getPort())
                    .serverOnlineMode(MojangAuth.isEnabled())
                    .build();

            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
            String filename = "report-" + f.format(new Date()) + ".txt";
            Path p = plugin.getFolder().toPath().resolve(filename);
            String generated = builder.generate();

            try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                w.write(generated);
                sender.sendMessage(Component.text(plugin.getI18n().get("report_saved", p.toAbsolutePath().toString())).color(TextColor.fromHexString("yellow")));
            } catch (IOException e) {
                sender.sendMessage(Component.text(plugin.getI18n().get("report_cant_save")).color(TextColor.fromHexString("red")));
                plugin.getLogger().info(generated);
            }
            return TaskSchedule.stop();
        }, ExecutionType.ASYNC);
    }

    @Override
    public String getDescription() {
        return plugin.getI18n().get("usage_report");
    }
}
