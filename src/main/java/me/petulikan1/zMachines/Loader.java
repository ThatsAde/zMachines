package me.petulikan1.zMachines;

import lombok.Getter;
import me.petulikan1.zMachines.config.Config;
import me.petulikan1.zMachines.database.SQLiteStorageHandler;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.items.FuelItem;
import me.petulikan1.zMachines.items.RecipeItem;
import me.petulikan1.zMachines.menu.HolderGUI;
import me.petulikan1.zMachines.menu.items.MenuItem;
import me.petulikan1.zMachines.messages.MiniImpl;
import me.petulikan1.zMachines.utils.Ref;
import me.petulikan1.zMachines.utils.VersionUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Loader extends JavaPlugin {

    @Getter
    public static Loader main;

    @Getter
    public static Config cfg, menuCfg, displayCfg, translations;


    public static SQLiteStorageHandler storageHandler;
    public static ConcurrentHashMap<UUID, HolderGUI> gui = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Machine> machines = new ConcurrentHashMap<>();

    public static HashMap<String, MenuItem> items = new HashMap<>();

    public static Set<Material> machineMaterials = new HashSet<>();

    public static List<FuelItem> fuelItems = new ArrayList<>();
    public static HashMap<MachineType, List<RecipeItem>> recipeItems = new HashMap<>();
    public static HashMap<MachineType, HashMap<Material, RecipeItem>> recipeIndex = new HashMap<>();
    // String-keyed recipe lookup. Used by the Crafting Machine where recipes are identified by config key
    // rather than by input material.
    public static HashMap<MachineType, HashMap<String, RecipeItem>> recipeByIdIndex = new HashMap<>();

    public static boolean isNexoSupport;

    public static List<Material> getMachineMaterials(MachineType type) {
        List<Material> list = new ArrayList<>();
        fuelItems.forEach(b -> list.add(b.getMaterial()));
        return list;
    }

    public static FuelItem getFuelItem(Machine machine, ItemStack item) {
        for (FuelItem fi : fuelItems) {
            if (fi.getMaterial().equals(item.getType())) {
                return fi;
            }
        }
        return null;
    }


    public static boolean isRecipeMaterial(ItemStack item, Machine machine) {
        return getRecipeItem(item, machine) != null;
    }

    public static RecipeItem getRecipeItem(ItemStack item, Machine machine) {
        HashMap<Material, RecipeItem> index = recipeIndex.get(machine.getMachineType());
        if (index == null) return null;
        return index.get(item.getType());
    }


    @Override
    public void onEnable() {
        Arrays.asList(
                Component.text("                                                 "),
                MiniImpl.parse("<gradient:#ae78ff:#ff5964>      __  __            _     _                  </gradient>"),
                MiniImpl.parse("<gradient:#ae78ff:#ff5964>     |  \\/  |          | |   (_)                 </gradient>"),
                MiniImpl.parse("<gradient:#ae78ff:#ff5964>  ___| \\  / | __ _  ___| |__  _ _ __   ___  ___  </gradient>"),
                MiniImpl.parse("<gradient:#ae78ff:#ff5964> |_  / |\\/| |/ _` |/ __| '_ \\| | '_ \\ / _ \\/ __| </gradient>"),
                MiniImpl.parse("<gradient:#ae78ff:#ff5964>  / /| |  | | (_| | (__| | | | | | | |  __/\\__ \\ </gradient>"),
                MiniImpl.parse("<gradient:#ae78ff:#ff5964> /___|_|  |_|\\__,_|\\___|_| |_|_|_| |_|\\___||___/ </gradient>"),
                Component.text("                                                 "),
                Component.text("                                                 ")
        ).forEach(
                a -> Loader.getConsole().sendMessage(a)
        );
        main = this;
        cfg = Config.createConfig("config.yml", "config.yml", this);
        menuCfg = Config.createConfig("menus.yml", "menus.yml", this);
        displayCfg = Config.createConfig("display.yml", "display.yml", this);
        translations = Config.createConfig("translations.yml", "translations.yml", this);
        isNexoSupport = this.getServer().getPluginManager().isPluginEnabled("Nexo");

        try {
            Ref.init(Ref.getClass("net.md_5.bungee.api.ChatColor") != null ? Ref.getClass("net.kyori.adventure.Adventure") != null ? Ref.ServerType.PAPER : Ref.ServerType.SPIGOT : Ref.ServerType.BUKKIT,
                    Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);
        } catch (Exception e) {
            String version = (String) Ref.invoke(Bukkit.getServer(), "getMinecraftVersion");
            VersionUtils.Version ver = VersionUtils.getVersion(version, "1.20.5");
            if (ver == VersionUtils.Version.SAME_VERSION || ver == VersionUtils.Version.NEWER_VERSION)
                Ref.init(Ref.getClass("net.md_5.bungee.api.ChatColor") != null ? Ref.getClass("net.kyori.adventure.Adventure") != null ? Ref.ServerType.PAPER : Ref.ServerType.SPIGOT : Ref.ServerType.BUKKIT,
                        version);
        }
        API.runTaskAsync(() -> {
            try {
                API.init();
            } catch (Throwable t) {
                // init() runs async via CompletableFuture, which silently swallows thrown exceptions.
                // Log loudly so startup failures (e.g. an incompatible optional dependency) are visible
                // instead of leaving the plugin half-initialised (no command, no listeners).
                Loader.main.error("zMachines failed to initialise: " + t);
                t.printStackTrace();
            }
        });

    }

    @Override
    public void onDisable() {
        API.shutdown();
    }

    public static CommandSender getConsole() {
        return Bukkit.getConsoleSender();
    }

    public void loG(Object... a) {
        for (Object b : a) {
            MiniImpl.msgConsole("&9[LOG] zMachines >> " + b);
        }
    }

    public void warn(Object... a) {
        for (Object b : a) {
            MiniImpl.msgConsole("&6[WARN] zMachines >> " + b);
        }
    }

    public void error(Object... a) {
        for (Object b : a) {
            MiniImpl.msgConsole("&c[ERROR] zMachines >> " + b);
        }
    }

    public void info(Object... a) {
        for (Object b : a) {
            MiniImpl.msgConsole("&a[INFO] zMachines >> " + b);
        }
    }

}

