package me.petulikan1.zMachines.menu.items;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.config.Config;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineState;
import me.petulikan1.zMachines.dataholders.ProgressLevel;
import me.petulikan1.zMachines.menu.GUI;
import me.petulikan1.zMachines.menu.HolderGUI;
import me.petulikan1.zMachines.menu.ItemGUI;
import me.petulikan1.zMachines.messages.Mini;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public class ItemGrid {
    private final String key;

    // Number of configured rows for this menu/tier. Drives the inventory size (rows * 9).
    private int rows;

    public HashMap<Integer, String> items = new HashMap<>();

    public HashMap<MachineState, MenuItem> statusItems = new HashMap<>();

    public HashMap<ProgressLevel, MenuItem> progressItems = new HashMap<>();

    public final List<Integer> fuelSlots = new ArrayList<>();

    public final List<Integer> inputSlots = new ArrayList<>();

    public final List<Integer> outputSlots = new ArrayList<>();

    public final List<Integer> statusSlots = new ArrayList<>();

    public final List<Integer> progressSlots = new ArrayList<>();

    // Crafting Machine slot types
    public final List<Integer> recipeListSlots = new ArrayList<>();
    public final List<Integer> pageBackSlots = new ArrayList<>();
    public final List<Integer> pageForwardSlots = new ArrayList<>();
    public final List<Integer> confirmCraftSlots = new ArrayList<>();


    public ItemGrid(Config config, String key, InventoryType type) {
        this.key = key;
        int counter = 0;
        int lengthToCheck = type == InventoryType.CHEST ? 9 : type.getDefaultSize();

        List<String> gridRows = config.getStringList(key);
        this.rows = gridRows.size();
        if (type == InventoryType.CHEST && rows > 6) {
            Loader.main.error("Menu " + key + " has " + rows + " lines, but chest menus support at most 6 (54 slots). Extra lines will be ignored.");
            this.rows = 6;
        }

        for (String a : gridRows) {
            String[] split = a.split(",");
            if (split.length != lengthToCheck) {
                Loader.main.error("Invalid item grid! Key: " + key + " | Data: " + a + " | Error: Invalid length - required " + lengthToCheck + ", found: " + split.length);
                counter = counter + lengthToCheck;
                continue;
            }

            for (String b : split) {
                items.put(counter, b);
                counter++;
            }
        }
        for (Map.Entry<Integer, String> e : items.entrySet()) {
            MenuItem mi = Loader.items.get(e.getValue());
            if (mi == null)
                continue;
            if (mi.getAction() == null)
                continue;
            MenuAction menuAction = mi.getAction().getAction();
            if (menuAction == MenuAction.FUEL_SLOT) {
                fuelSlots.add(e.getKey());
            } else if (menuAction == MenuAction.INPUT_SLOT) {
                inputSlots.add(e.getKey());
            } else if (menuAction == MenuAction.OUTPUT_SLOT) {
                outputSlots.add(e.getKey());
            } else if (menuAction == MenuAction.STATUS) {
                statusSlots.add(e.getKey());
            } else if (menuAction == MenuAction.PROGRESS) {
                progressSlots.add(e.getKey());
            } else if (menuAction == MenuAction.RECIPE_LIST) {
                recipeListSlots.add(e.getKey());
            } else if (menuAction == MenuAction.PAGE_BACK) {
                pageBackSlots.add(e.getKey());
            } else if (menuAction == MenuAction.PAGE_FORWARD) {
                pageForwardSlots.add(e.getKey());
            } else if (menuAction == MenuAction.CONFIRM_CRAFT) {
                confirmCraftSlots.add(e.getKey());
            }
        }
        // Keep recipe list slots in inventory-order so paging maps cleanly to slot order.
        java.util.Collections.sort(recipeListSlots);

        for (MachineState mt : MachineState.values()) {
            if (!Loader.displayCfg.exists("Status." + mt.name())) {
                Loader.main.error("Missing MachineStatus key: " + mt.name());
                continue;
            }
            MenuItem mi = new MenuItem(Loader.displayCfg, "Status." + mt.name());
            statusItems.put(mt, mi);
        }

        for (ProgressLevel pl : ProgressLevel.values()) {
            if (!Loader.displayCfg.exists("Progress." + pl.name())) {
                Loader.main.error("Missing Progress key: " + pl.name());
                continue;
            }
            MenuItem progressMenuItem = new MenuItem(Loader.displayCfg, "Progress." + pl.name());
            progressItems.put(pl, progressMenuItem);
        }

    }

    public void prepareItems(Map<MenuItem, List<Integer>> list) {
        for (Map.Entry<Integer, String> e : items.entrySet()) {
            if (!Loader.items.containsKey(e.getValue())) {
                Loader.main.error("Detected missing item " + e.getValue() + " for slot: " + e.getKey() + " | Key: " + getKey());
                continue;
            }
            MenuItem item = Loader.items.get(e.getValue());
            if (item.getAction() == null) {
                Loader.main.error("MenuAction cannot be null - item: " + item.getKey());
                continue;
            }

            if (list.containsKey(item)) {
                list.get(item).add(e.getKey());
            } else {
                List<Integer> l = new ArrayList<>();
                l.add(e.getKey());
                list.put(item, l);
            }
        }
    }

    public void fillItemsToGUI(@NonNull GUI gui, Player player, Machine machine, TagResolver... resolvers) {
        Map<MenuItem, List<Integer>> list = new HashMap<>();

        prepareItems(list);
        TagResolver mainResolver = combine(resolvers, machine.getNameTagResolver());


        for (Map.Entry<MenuItem, List<Integer>> e : list.entrySet()) {
            MenuItem item = e.getKey();
            ItemStack itemStack = item.build(player, mainResolver);


            MenuAction action = item.getAction().getAction();
            if (action == MenuAction.FUEL_SLOT || action == MenuAction.OUTPUT_SLOT || action == MenuAction.INPUT_SLOT) {
                HashMap<Integer, ItemStack> items = machine.getInventoryItems().get(action.name());
                if (items != null && !items.isEmpty()) {
                    boolean update = false;
                    boolean messaged = false;
                    for (Map.Entry<Integer, ItemStack> entry : new HashMap<>(items).entrySet()) {
                        if (e.getValue().contains(entry.getKey())) {
                            gui.getInventory().setItem(entry.getKey(), entry.getValue());
                        } else {
                            update = true;
                            Loader.main.info("Inventory update detected - old machine data? Dropping items on ground!!!");
                            Location machineLocation = machine.getLocation().toBukkit();
                            if (machineLocation != null && entry.getValue() != null) {
                                API.runTaskSync(() -> machineLocation.getWorld().dropItemNaturally(machineLocation, entry.getValue()));
                            }
                            items.remove(entry.getKey());
                            if (!messaged) {
                                messaged = true;
                                Mini.mm(player, "MachineUpdatedItemDrop");
                            }
                        }
                    }
                    if (update) {
                        machine.getInventoryItems().put(action.name(), items);
                        API.runTaskAsync(() -> {
                            try {
                                API.updateMachine(machine);
                            } catch (Exception ee) {
                                ee.printStackTrace();
                                Loader.main.error("Failed to update machine at location: " + machine.getLocation().toBukkit().toString() + " | " + player.getName() + " | " + ee.getMessage());
                                Mini.mm(player, "ErrorUpdating");
                            }
                        });
                    }
                }
                continue;
            }


            ItemGUI itemGUI = new ItemGUI(itemStack) {
                @Override
                public boolean onClick(Player menuPlayer, HolderGUI holderGUI, ClickType clickType, int slot, InventoryAction action, InventoryClickEvent e) {
                    item.getClickSound().play(menuPlayer);
                    return item.getAction().getAction().getHandler().handle(new MenuAction.MenuActionContext(menuPlayer, holderGUI, clickType, slot, action, e, gui, player, this, mainResolver, item, machine));
                }
            };

            gui.setItem(e.getValue(), itemGUI);
        }
    }

    private static TagResolver combine(TagResolver[] original, TagResolver... resolvers) {
        return TagResolver.builder().resolvers(original).resolvers(resolvers).build();
    }

}
