package net.buycraft.plugin.minestom.command;

import net.buycraft.plugin.bukkit.BuycraftPluginBase;
import net.buycraft.plugin.minestom.BuycraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.CommandSender;

public class InformationSubcommand implements Subcommand{
    private final BuycraftPlugin plugin;

    public InformationSubcommand(final BuycraftPlugin plugin) {
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

        if (plugin.getServerInformation() == null) {
            sender.sendMessage(Component.text(plugin.getI18n().get("information_no_server")).color(TextColor.fromHexString("red")));
            return;
        }

        sender.sendMessage(Component.text(plugin.getI18n().get("information_title")).color(TextColor.fromHexString("gray")));
        sender.sendMessage(Component.text(plugin.getI18n().get("information_server",
                plugin.getServerInformation().getServer().getName(),
                plugin.getServerInformation().getAccount().getName())).color(TextColor.fromHexString("gray")));
        sender.sendMessage(Component.text(plugin.getI18n().get("information_currency",
                plugin.getServerInformation().getAccount().getCurrency().getIso4217())).color(TextColor.fromHexString("gray")));
        sender.sendMessage(Component.text(plugin.getI18n().get("information_domain",
                plugin.getServerInformation().getAccount().getDomain())).color(TextColor.fromHexString("gray")));
    }

    @Override
    public String getDescription() {
        return plugin.getI18n().get("usage_information");
    }
}
