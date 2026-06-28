package me.petulikan1.zMachines.commands;

import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.utils.Validator;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CommandHandler {

    private static final BukkitCommandManager cmdManager = new BukkitCommandManager();

    public static void createAndRegisterCommand(String command, String permission, CommandExecutor mainClass, String... aliases) {
        createAndRegisterCommand(command, permission, mainClass, Arrays.asList(aliases));
    }

    public static void createAndRegisterCommand(String command, String permission, CommandExecutor mainClass, Collection<String> aliases) {
        Validator.validate(command == null, "Command cannot be null");
        Validator.validate(mainClass == null, "CommandExecutor cannot be null");
        PluginCommand cmd = cmdManager.createCommand(command, Loader.main);
        if (permission != null) {
            cmd.setPermission(permission);
        }
        if (aliases != null) {
            cmd.setAliases(new ArrayList<>(aliases));
        }
        cmd.setExecutor(mainClass);
        if (mainClass instanceof TabCompleter completer)
            cmd.setTabCompleter(completer);
        List<PluginCommand> cmds = commands.getOrDefault(Loader.main, new ArrayList<>());
        cmds.add(cmd);
        commands.put(Loader.main, cmds);
        cmdManager.registerCommand(cmd);
    }

    private static final HashMap<Plugin, List<PluginCommand>> commands = new HashMap<>();

    public static void unregisterAll() {
        for (PluginCommand cmd : commands.getOrDefault(Loader.main, new ArrayList<>())) {
            cmdManager.unregister(cmd);
        }
        commands.remove(Loader.main);
    }

}
