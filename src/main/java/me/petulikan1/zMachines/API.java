package me.petulikan1.zMachines;

import me.petulikan1.zMachines.commands.CommandHandler;
import me.petulikan1.zMachines.commands.impl.zMachinesCommand;
import me.petulikan1.zMachines.config.MenuConfig;
import me.petulikan1.zMachines.database.SQLiteStorageHandler;
import me.petulikan1.zMachines.dataholders.LocationInternal;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.dataholders.Tier;
import me.petulikan1.zMachines.events.HopperTransferTask;
import me.petulikan1.zMachines.events.MachineListener;
import me.petulikan1.zMachines.hooks.Nexo;
import me.petulikan1.zMachines.items.FuelItem;
import me.petulikan1.zMachines.items.RecipeItem;
import me.petulikan1.zMachines.items.SimpleItem;
import me.petulikan1.zMachines.menu.HolderGUI;
import me.petulikan1.zMachines.menu.event.MenuListener;
import me.petulikan1.zMachines.menu.items.MenuActionWrapper;
import me.petulikan1.zMachines.menu.items.MenuItem;
import me.petulikan1.zMachines.utils.Pair;
import me.petulikan1.zMachines.utils.Ref;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class API {

    // Nexo mechanic ID (e.g. "growing_machine_1_north") → machine type + tier.
    // Built from config on every load/reload; used by Nexo.java for ID-based placement detection.
    public static final Map<String, Pair<MachineType, Integer>> nexoMachineById = new ConcurrentHashMap<>();

    protected static void init() {
        Loader.main.info("Loading zMachines...");
        Loader.storageHandler = new SQLiteStorageHandler();
        try {
            Loader.storageHandler.initialize();
        } catch (Exception e) {
            Loader.main.error("An error occurred while trying to initialize SQLite database!");
            namedError(e);
            Bukkit.getPluginManager().disablePlugin(Loader.main);
            return;
        }
        Loader.main.info("Successfully initialized SQLite database! Loading data...");

        try {
            Loader.storageHandler.loadData();
        } catch (Exception e) {
            Loader.main.error("There was an error while trying to load data from database!");
            namedError(e);
            Bukkit.getPluginManager().disablePlugin(Loader.main);
            return;
        }
        Loader.main.info("Successfully loaded data!");


        reload(true);

        // Register the command FIRST so /zmachines is always available, even if an optional
        // integration (Nexo) fails to register below. Previously this ran last, so a Nexo API
        // mismatch left the whole command unregistered.
        CommandHandler.createAndRegisterCommand("zmachines", "zmachines.admin", new zMachinesCommand());

        if (Loader.isNexoSupport) {
            try {
                Bukkit.getPluginManager().registerEvents(new Nexo(), Loader.main);
            } catch (Throwable t) {
                // A Nexo version mismatch surfaces here as NoClassDefFoundError: Bukkit reflects the
                // listener's @EventHandler methods and tries to resolve their event parameter types
                // (e.g. the furniture/custom-block events). If those classes are absent in the
                // installed Nexo, that throws. Catch it (Throwable, since it's a LinkageError, not an
                // Exception) so the rest of the plugin still loads — and log the real cause.
                Loader.isNexoSupport = false;
                Loader.main.error("Failed to register Nexo integration — Nexo support disabled for this session. " +
                        "Make sure the installed Nexo version matches the plugin's API (needs furniture + custom-block events). Cause: " + t);
                t.printStackTrace();
            }
        }

        Bukkit.getPluginManager().registerEvents(new MenuListener(), Loader.main);
        Bukkit.getPluginManager().registerEvents(new MachineListener(), Loader.main);

        // Hopper automation: insert (top) / extract (bottom) for every machine. The task itself
        // reads the "Hoppers" config toggle each cycle, so /zmachines reload enables/disables it live.
        Bukkit.getScheduler().runTaskTimer(Loader.main, new HopperTransferTask(), 8L, 8L);
    }


    public static void reload(boolean start) {
        loadMaterials();
        loadItems();
        loadFuels();
        loadRecipeItems();
        if (!start) {
            Ref.invokeStatic(MenuConfig.class, "clearCache");
            for (HolderGUI gui : new ArrayList<>(Loader.gui.values())) {
                gui.closeAll();
            }
            Machine.itemCache.clear();
        }
    }

    private static void loadItems() {
        Loader.items.clear();
        for (String key : Loader.menuCfg.getKeys("Items")) {
            if (!Loader.menuCfg.existsKey("Items." + key + ".MenuAction") || Loader.menuCfg.getString("Items." + key + ".MenuAction").isEmpty()) {
                Loader.main.error("MenuAction for item: " + key + " cannot be null or empty!");
                continue;
            }
            MenuActionWrapper menuAction = new MenuActionWrapper(Loader.menuCfg.getString("Items." + key + ".MenuAction"));
            MenuItem menuItem = new MenuItem(Loader.menuCfg, "Items." + key + ".Settings", menuAction);
            Loader.items.put(key, menuItem);
        }
    }

    public static void loadFuels() {
        Loader.fuelItems.clear();
        for (String a : Loader.cfg.getStringList("Fuel")) {
            String[] b = a.split(":");
            if (b.length != 2) {
                Loader.main.error("Invalid fuel item format - " + a + " | correct format: <item:rate>");
                continue;
            }
            Material m = Material.getMaterial(b[0]);
            if (m == null) {
                Loader.main.error("Invalid material provided as fuel - " + b[0]);
                continue;
            }
            float rate;
            try {
                rate = Float.parseFloat(b[1]);
            } catch (Exception e) {
                Loader.main.error("Error! " + b[1] + " is not a float!");
                continue;
            }
            Loader.fuelItems.add(new FuelItem(m, rate));
        }
    }

    private static SimpleItem parseItem(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        // Nexo format: "nexo:<id>[:<amount>]" — the id may itself contain colons in theory; we treat the LAST
        // colon-segment as the amount only if it's purely numeric, otherwise the id includes it.
        if (raw.toLowerCase().startsWith("nexo:")) {
            String rest = raw.substring("nexo:".length());
            int amount = 1;
            int lastColon = rest.lastIndexOf(':');
            String id = rest;
            if (lastColon != -1) {
                String tail = rest.substring(lastColon + 1);
                try {
                    amount = Integer.parseInt(tail);
                    id = rest.substring(0, lastColon);
                } catch (NumberFormatException ignored) {
                    // id contains the colon, no explicit amount
                }
            }
            if (id.isEmpty()) {
                Loader.main.error("Empty Nexo id in '" + raw + "'");
                return null;
            }
            // Material is just a fallback for any vanilla-path code that still reaches .getMaterial();
            // matches()/getStackCached() use the Nexo id when present.
            return new SimpleItem(Material.BEDROCK, amount, id);
        }
        String[] split = raw.split(":");
        Material m = Material.getMaterial(split[0].toUpperCase());
        if (m == null) {
            Loader.main.error("Invalid material: '" + split[0] + "'");
            return null;
        }
        int amount = 1;
        if (split.length == 2) {
            try {
                amount = Integer.parseInt(split[1]);
            } catch (NumberFormatException ex) {
                Loader.main.error("Invalid amount in '" + raw + "': " + split[1]);
                return null;
            }
        }
        return new SimpleItem(m, amount);
    }


    public static void loadRecipeItems() {
        Loader.recipeItems.clear();
        Loader.recipeIndex.clear();
        Loader.recipeByIdIndex.clear();
        loadRubbleRecipe();
        loadGrowingRecipes();
        loadCraftingRecipes();
    }

    private static void loadRubbleRecipe() {
        List<RecipeItem> list = new ArrayList<>();
        HashMap<Material, RecipeItem> index = new HashMap<>();
        String path = "Recipes.RubbleProcessor";
        if (Loader.cfg.existsKey(path + ".BaseRate") && Loader.cfg.existsKey(path + ".Output")) {
            double baseRate = Loader.cfg.getDouble(path + ".BaseRate", 4.0);
            SimpleItem output = parseItem(Loader.cfg.getString(path + ".Output", ""));
            if (output == null) {
                Loader.main.error("Invalid Output for RubbleProcessor recipe at '" + path + ".Output'");
            } else {
                // placeholder: rubble matching is tag-based (see InventoryUtils.getAllRecipeData);
                // this SimpleItem's amount drives the per-slot consumption (1 per cycle)
                List<SimpleItem> placeholder = new ArrayList<>();
                placeholder.add(new SimpleItem(Material.STONE, 1));
                list.add(new RecipeItem(placeholder, output, baseRate));
            }
        } else {
            Loader.main.error("Missing RubbleProcessor recipe at '" + path + "' — need BaseRate and Output.");
        }
        Loader.recipeItems.put(MachineType.RUBBLE_PROCESSOR, list);
        Loader.recipeIndex.put(MachineType.RUBBLE_PROCESSOR, index);
    }

    private static void loadCraftingRecipes() {
        List<RecipeItem> list = new ArrayList<>();
        HashMap<String, RecipeItem> idIndex = new HashMap<>();
        String basePath = "Recipes.CraftingMachine";
        // No existsKey() check here — the DataLoader stores only leaf keys (e.g. "...iron_sword.Order"),
        // so existsKey() on a section path returns false even when the section has children.
        // getKeys() does the correct prefix scan; if the section is missing, it returns an empty set
        // and the loop is just skipped. Matches the pattern used by loadGrowingRecipes.
        for (String key : Loader.cfg.getKeys(basePath)) {
            String path = basePath + "." + key;
            if (!Loader.cfg.existsKey(path + ".BaseRate")) {
                Loader.main.error("Missing BaseRate at '" + path + "'");
                continue;
            }
            if (!Loader.cfg.existsKey(path + ".Inputs")) {
                Loader.main.error("Missing Inputs at '" + path + "'");
                continue;
            }
            if (!Loader.cfg.existsKey(path + ".Output")) {
                Loader.main.error("Missing Output at '" + path + "'");
                continue;
            }
            double baseRate = Loader.cfg.getDouble(path + ".BaseRate", -1.0);
            if (baseRate <= 0) {
                Loader.main.error("Invalid BaseRate at '" + path + ".BaseRate'");
                continue;
            }
            int order = Loader.cfg.getInt(path + ".Order", Integer.MAX_VALUE);
            SimpleItem output = parseItem(Loader.cfg.getString(path + ".Output", ""));
            if (output == null) continue;

            List<SimpleItem> inputs = new ArrayList<>();
            for (String raw : Loader.cfg.getStringList(path + ".Inputs")) {
                SimpleItem si = parseItem(raw);
                if (si != null) inputs.add(si);
            }
            if (inputs.isEmpty()) {
                Loader.main.error("No valid inputs at '" + path + ".Inputs'");
                continue;
            }

            RecipeItem recipe = new RecipeItem(inputs, output, baseRate, order, key);
            list.add(recipe);
            idIndex.put(key, recipe);
        }
        // Sort by Order ascending so the recipe list paginates in user-controlled order
        list.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

        Loader.recipeItems.put(MachineType.CRAFTING_MACHINE, list);
        Loader.recipeByIdIndex.put(MachineType.CRAFTING_MACHINE, idIndex);
    }

    private static void loadGrowingRecipes() {
        List<RecipeItem> list = new ArrayList<>();
        HashMap<Material, RecipeItem> index = new HashMap<>();
        String basePath = "Recipes.GrowingMachine";
        for (String key : Loader.cfg.getKeys(basePath)) {
            String path = basePath + "." + key;
            if (!Loader.cfg.existsKey(path + ".BaseRate")) {
                Loader.main.error("Missing BaseRate at '" + path + "'");
                continue;
            }
            if (!Loader.cfg.existsKey(path + ".Input")) {
                Loader.main.error("Missing Input at '" + path + "'");
                continue;
            }
            if (!Loader.cfg.existsKey(path + ".Output")) {
                Loader.main.error("Missing Output at '" + path + "'");
                continue;
            }
            double baseRate = Loader.cfg.getDouble(path + ".BaseRate", -1.0);
            if (baseRate <= 0) {
                Loader.main.error("Invalid BaseRate at '" + path + ".BaseRate'");
                continue;
            }
            SimpleItem input = parseItem(Loader.cfg.getString(path + ".Input", ""));
            SimpleItem output = parseItem(Loader.cfg.getString(path + ".Output", ""));
            if (input == null || output == null) continue;
            List<SimpleItem> inputs = new ArrayList<>();
            inputs.add(input);
            RecipeItem recipe = new RecipeItem(inputs, output, baseRate);
            list.add(recipe);
            index.put(input.getMaterial(), recipe);
        }
        Loader.recipeItems.put(MachineType.GROWING_MACHINE, list);
        Loader.recipeIndex.put(MachineType.GROWING_MACHINE, index);
    }


    public static void namedError(Exception e) {
        Loader.main.error("--------------------------------");
        e.printStackTrace();
        Loader.main.error("--------------------------------");
    }


    /**
     * Returns the raw material string for the given machine type + tier.
     * Checks {@code <id>.ItemStack.TierN.Material} first; falls back to the flat
     * {@code <id>.ItemStack.Material} key so existing configs continue to work.
     * This is the "give default" — typically the North / primary variant.
     */
    public static String getMaterialRaw(String identifier, int tier) {
        String raw = Loader.cfg.getString(identifier + ".ItemStack.Tier" + tier + ".Material", "");
        if (!raw.isEmpty()) return raw;
        return Loader.cfg.getString(identifier + ".ItemStack.Material", "");
    }

    /**
     * Returns the raw material string for the given machine type + tier + cardinal facing.
     * Checks {@code <id>.ItemStack.TierN.<Facing>} first (e.g. {@code Tier1.South}), then
     * falls back to {@link #getMaterialRaw(String, int)} (the tier default / North variant).
     *
     * @param facing capitalised direction: {@code "North"}, {@code "South"}, {@code "East"}, {@code "West"}
     */
    public static String getMaterialRaw(String identifier, int tier, String facing) {
        String raw = Loader.cfg.getString(identifier + ".ItemStack.Tier" + tier + "." + facing, "");
        if (!raw.isEmpty()) return raw;
        return getMaterialRaw(identifier, tier);
    }

    // Cardinal facings iterated when registering materials — covers per-facing Nexo furniture variants.
    private static final List<String> FACINGS = List.of("North", "South", "East", "West");

    private static void loadMaterials() {
        // Register the block material for every machine type + every tier + every facing so
        // block-click detection covers all per-tier, per-facing Nexo furniture variants.
        // Duplicate raw strings (facing not configured → resolves to same ID as default) are skipped.
        List<Material> materials = new ArrayList<>();
        for (MachineType mt : MachineType.values()) {
            String key = mt.getIdentifier();
            Set<String> seen = new HashSet<>();
            for (int tier = Tier.MIN; tier <= Tier.MAX; tier++) {
                for (String facing : FACINGS) {
                    String raw = getMaterialRaw(key, tier, facing);
                    if (seen.add(raw)) {
                        registerMaterial(key + " tier " + tier + " " + facing, raw, materials);
                    }
                }
            }
        }
        Loader.machineMaterials.clear();
        Loader.machineMaterials.addAll(materials);
        // Re-add Nexo block materials AFTER the clear. Handles two cases:
        // 1. Reload: run() was called inside addItem() before the clear above — re-add what was wiped.
        // 2. Startup order: NexoItemsLoadedEvent fired before our listener registered — probe the API now.
        if (Loader.isNexoSupport) Nexo.reloadMaterials();
        buildNexoIdMap();
    }

    private static void buildNexoIdMap() {
        Map<String, Pair<MachineType, Integer>> map = new HashMap<>();
        for (MachineType mt : MachineType.values()) {
            String key = mt.getIdentifier();
            for (int tier = Tier.MIN; tier <= Tier.MAX; tier++) {
                for (String facing : FACINGS) {
                    String raw = getMaterialRaw(key, tier, facing);
                    if (raw.toLowerCase().startsWith("nexo:")) {
                        map.putIfAbsent(raw.substring(5), new Pair<>(mt, tier));
                    }
                }
            }
        }
        nexoMachineById.clear();
        nexoMachineById.putAll(map);
    }

    private static void registerMaterial(String label, String raw, List<Material> materials) {
        if (raw.isEmpty()) {
            Loader.main.error("No material configured for " + label);
            return;
        }
        Material m = Material.getMaterial(raw.toUpperCase());
        if (m != null) {
            if (!materials.contains(m)) materials.add(m);
        } else if (raw.toLowerCase().startsWith("nexo:")) {
            Nexo.addItem(raw.substring("nexo:".length()));
        } else {
            Loader.main.error("Invalid material for " + label + " - " + raw);
        }
    }


    public static void runTaskAsync(Runnable task) {
        runTaskAsync(task, 0, TimeUnit.SECONDS);
    }

    public static void runTaskAsync(Runnable task, long delay, TimeUnit unit) {
        if (Bukkit.getServer().isPrimaryThread()) {
            CompletableFuture.runAsync(task, CompletableFuture.delayedExecutor(delay, unit));
        } else {
            task.run();
        }
    }


    public static void runTaskSync(Runnable task) {
        if (Bukkit.getServer().isPrimaryThread()) {
            task.run();
        } else {
            CompletableFuture.runAsync(task);
        }
    }


    public static Machine createMachine(Pair<MachineType, String> mt, Player player, Location location, final int tier) throws Exception {
        return Loader.storageHandler.createMachine(location, player.getUniqueId(), mt, tier);
    }

    public static boolean deleteMachine(Machine machine) throws Exception {
        return Loader.storageHandler.deleteMachine(machine);
    }

    public static void updateMachine(Machine machine) throws Exception {
        Loader.storageHandler.updateMachine(machine);
    }

    public static String getLocationKeyed(LocationInternal locInt){
        return locInt.getX() + ";" + locInt.getY() + ";" + locInt.getZ() + ";" + locInt.getWorldName();
    }
    public static String getLocationKeyed(Location loc){
        return loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getWorld().getName();
    }

    public static Machine getMachineByLocation(Location locInt) {
        return Loader.machines.get(getLocationKeyed(locInt));
    }


    protected static void shutdown() {
        for (HolderGUI gui : new ArrayList<>(Loader.gui.values())) {
            gui.closeAll();
        }

        HandlerList.unregisterAll(Loader.main);
        CommandHandler.unregisterAll();
        if (Loader.storageHandler != null)
            Loader.storageHandler.close();
    }

}
