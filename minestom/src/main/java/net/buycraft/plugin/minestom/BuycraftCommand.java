package net.buycraft.plugin.minestom;

import net.buycraft.plugin.minestom.command.Subcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class BuycraftCommand extends Command {
    private final Map<String, Subcommand> subcommandMap = new LinkedHashMap<>();
    private final BuycraftPlugin plugin;

    public BuycraftCommand(BuycraftPlugin plugin) {
        super("tebex", "buycraft");
        ArgumentString sub = ArgumentType.String("sub");
        ArgumentStringArray rest = ArgumentType.StringArray("rest");
        this.plugin = plugin;
        sub.setSuggestionCallback((sender, context, suggestion) -> {
           for(String subcomm: subcommandMap.keySet()){
               suggestion.addEntry(new SuggestionEntry(subcomm));
           }
        });
        setDefaultExecutor((sender, context) -> {
            if (!sender.hasPermission("buycraft.admin") && !(sender instanceof ConsoleSender)) {
                sender.sendMessage(Component.text(plugin.getI18n().get("no_permission")).color(TextColor.color(255,0,0)));
                return;
            }
            showHelp(sender);
        });
        addSyntax((sender, context) -> {
            String arg = context.get(sub);
            for (Map.Entry<String, net.buycraft.plugin.minestom.command.Subcommand> entry : subcommandMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(arg)) {
                    entry.getValue().execute(sender, new String[]{});
                }
            }
        }, sub);
        addSyntax((sender, context) -> {
            String arg = context.get(sub);
            String[] argRest = context.get(rest);
            for (Map.Entry<String, net.buycraft.plugin.minestom.command.Subcommand> entry : subcommandMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(arg)) {
                    entry.getValue().execute(sender, argRest);
                }
            }
        }, sub, rest);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text(plugin.getI18n().get("usage")).color(TextColor.fromHexString("DarkBlue")).decorate(TextDecoration.BOLD));
        for (Map.Entry<String, Subcommand> entry : subcommandMap.entrySet()) {
            sender.sendMessage(Component.text("/tebex " + entry.getKey()).color(TextColor.fromHexString("Green")).append(Component.text(": " + entry.getValue().getDescription()).color(TextColor.fromHexString("Gray"))));
        }
    }

    public Map<String, Subcommand> getSubcommandMap() {
        return this.subcommandMap;
    }
}
