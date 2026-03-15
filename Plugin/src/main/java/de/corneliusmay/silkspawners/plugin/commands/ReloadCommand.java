package de.corneliusmay.silkspawners.plugin.commands;

import de.corneliusmay.silkspawners.plugin.commands.handler.SilkSpawnersCommand;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends SilkSpawnersCommand {

    public ReloadCommand() {
        super("reload", true);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length != 0) {
            return invalidSyntax(sender);
        }

        try {
            if (!plugin.reloadPluginState()) {
                sendMessage(sender, "ERROR");
                return false;
            }

            sendMessage(sender, "SUCCESSFUL");
            return true;
        } catch (Exception ex) {
            plugin.getLog().error("Error while reloading plugin state", ex);
            sendMessage(sender, "ERROR");
            return false;
        }
    }
}

