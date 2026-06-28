package me.petulikan1.zMachines.commands;

import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.utils.Ref;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BukkitCommandManager {

    protected static CommandMap cmdMap;
    protected static Map<String, Command> knownCommands;
    private static final Constructor<?> constructor;

    static {
        cmdMap = (CommandMap) Ref.get(Bukkit.getPluginManager(), "commandMap");
        knownCommands = (Map<String, Command>) Ref.get(cmdMap, "knownCommands");
        constructor = Ref.constructor(PluginCommand.class, String.class, Plugin.class);
    }

    PaperCommandMap paperMap;


    public BukkitCommandManager() {
        if (Ref.serverType() == Ref.ServerType.PAPER && Ref.isNewerThan(20)) {
            paperMap = new PaperCommandMap(Bukkit.getServer(), knownCommands);
            cmdMap = paperMap;
        }
        // If server-type detection failed but the static-block reflection succeeded,
        // cmdMap is already pointing at the real server CommandMap — use it as-is.
        if (cmdMap == null)
            throw new RuntimeException("CommandMap cannot be null but is null!");
        Loader.main.info("CommandManager: " + cmdMap.getClass().getSimpleName());
    }

    public PluginCommand createCommand(String name, Plugin plugin) {
        return (PluginCommand) Ref.newInstance(constructor, name, plugin);
    }

    public void registerCommand(PluginCommand command) {
        String label = command.getName().toLowerCase(Locale.ENGLISH).trim();
        String sd = command.getPlugin().getName().toLowerCase(Locale.ENGLISH).trim();
        command.setLabel(sd + ":" + label);
        command.register(cmdMap);
        if (command.getTabCompleter() == null) {
            if (command.getExecutor() instanceof TabCompleter) {
                command.setTabCompleter((TabCompleter) command.getExecutor());
            } else {
                command.setTabCompleter((arg0, arg1, arg2, arg3) -> null);
            }
        }
        List<String> low = new ArrayList<>();
        for (String s : command.getAliases()) {
            s = s.toLowerCase(Locale.ENGLISH).trim();
            low.add(s);
        }
        command.setAliases(low);
        if (command.getPermission() == null) {
            command.setPermission("");
        }
        if (!low.contains(label)) {
            low.add(label);
        }
        for (String s : low) {
            knownCommands.put(s, command);
        }

        reload();
    }

    private void reload() {
        try {
            Ref.invoke(Bukkit.getServer(), "syncCommands");
        } catch (Exception e) {}
    }

    public void unregister(PluginCommand command) {
        knownCommands.remove(command.getName().toLowerCase(Locale.ENGLISH).trim());
        for (String alias : command.getAliases())
            knownCommands.remove(alias.toLowerCase(Locale.ENGLISH).trim());
        reload();
    }


}
