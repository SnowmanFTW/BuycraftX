package net.buycraft.plugin.minestom.command;

import net.minestom.server.command.CommandSender;

public interface Subcommand {
    void execute(CommandSender sender, String[] args);

    String getDescription();
}
