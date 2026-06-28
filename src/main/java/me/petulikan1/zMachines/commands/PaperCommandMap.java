package me.petulikan1.zMachines.commands;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

public class PaperCommandMap extends SimpleCommandMap {

    public PaperCommandMap(Server server, Map<String, Command> map) {
        super(server, map);
    }

    @Override
    public boolean dispatch(@NotNull CommandSender sender, @NotNull String commandLine) throws CommandException {
        String[] args = commandLine.replace("  ", " ").split(" ");
        if (args.length == 0)
            return false;
        String sentCommandLabel = args[0].toLowerCase();
        Command target = getCommand(sentCommandLabel);
        if (target == null)
            return false;
        try {
            target.execute(sender, sentCommandLabel, Arrays.copyOfRange(args, 1, args.length));
        } catch (CommandException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new CommandException("Unhandled exception executing '" + commandLine + "' in " + target, ex);
        }
        return true;
    }
}
