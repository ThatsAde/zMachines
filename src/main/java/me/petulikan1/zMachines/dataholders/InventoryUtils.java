package me.petulikan1.zMachines.dataholders;

import com.nexomc.nexo.api.NexoItems;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.dataholders.impl.CraftingMachine;
import me.petulikan1.zMachines.items.FuelItem;
import me.petulikan1.zMachines.items.RecipeItem;
import me.petulikan1.zMachines.items.SimpleItem;
import me.petulikan1.zMachines.menu.GUI;
import me.petulikan1.zMachines.menu.items.ItemGrid;
import me.petulikan1.zMachines.utils.Pair;
import me.petulikan1.zMachines.utils.Triplet;
import org.bukkit.Tag;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InventoryUtils {

    protected List<Triplet<FuelItem, ItemStack, Integer>> getAllFuelData(Map<Integer, ItemStack> fuelMap, Machine machine) {
        List<Triplet<FuelItem, ItemStack, Integer>> list = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : fuelMap.entrySet()) {
            ItemStack item = entry.getValue();
            FuelItem fi = Loader.getFuelItem(machine, item);
            if (fi == null) {
                continue;
            }
            list.add(new Triplet<>(fi, entry.getValue(), entry.getKey()));
        }
        return list;
    }

    protected HashMap<Integer, ItemStack> getOutputSlots(GUI gui, List<Integer> slots) {
        HashMap<Integer, ItemStack> map = new HashMap<>();
        for (int i : slots) {
            ItemStack item = gui.getInventory().getItem(i);
            map.put(i, item);
        }
        return map;
    }

    protected HashMap<Integer, ItemStack> getItems(GUI gui, List<Integer> slots) {
        HashMap<Integer, ItemStack> map = new HashMap<>();
        for (int i : slots) {
            ItemStack item = gui.getInventory().getItem(i);
            if (item == null)
                continue;
            map.put(i, item);
        }
        return map;
    }

    protected List<Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>>> getAllRecipeData(HashMap<Integer, ItemStack> inputMap, ItemGrid itemGrid, Machine machine) {
        List<Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>>> list = new ArrayList<>();
        return switch (machine.getMachineType()) {
            case RUBBLE_PROCESSOR -> {
                if (inputMap.size() != itemGrid.getInputSlots().size()) yield list;
                for (ItemStack item : inputMap.values()) {
                    if (item == null) yield list;
                    if (!Tag.BASE_STONE_OVERWORLD.isTagged(item.getType())) yield list;
                    if (item.getAmount() < 1) yield list;
                }
                List<RecipeItem> recipes = Loader.recipeItems.get(MachineType.RUBBLE_PROCESSOR);
                if (recipes == null || recipes.isEmpty()) yield list;
                RecipeItem ri = recipes.getFirst();
                list.add(new Triplet<>(ri, ri.getInputMaterials().getFirst(), new LinkedHashMap<>(inputMap)));
                yield list;
            }
            case GROWING_MACHINE -> {
                for (Map.Entry<Integer, ItemStack> entry : inputMap.entrySet()) {
                    ItemStack item = entry.getValue();
                    RecipeItem ri = Loader.getRecipeItem(item, machine);
                    if (ri == null) continue;

                    Map<Integer, ItemStack> slotMap = new LinkedHashMap<>();
                    slotMap.put(entry.getKey(), item);
                    list.add(new Triplet<>(ri, ri.getInputMaterials().getFirst(), slotMap));
                }
                yield list;
            }
            case CRAFTING_MACHINE -> {
                // Recipe-driven and explicit-start. Only the active recipe ticks; no recipe data when idle.
                if (!(machine instanceof CraftingMachine cm)) yield list;
                String activeId = cm.getActiveRecipeId();
                if (activeId == null) yield list;

                HashMap<String, RecipeItem> index = Loader.recipeByIdIndex.get(MachineType.CRAFTING_MACHINE);
                RecipeItem recipe = index == null ? null : index.get(activeId);
                if (recipe == null) yield list;

                if (!recipe.matchesInputs(inputMap)) yield list; // inputs no longer satisfy — machine will idle

                // First inputMaterial drives downstream "inputMaterial" calls in Machine#onTick; the actual
                // consumption logic for the Crafting Machine is in removeInputItems below.
                list.add(new Triplet<>(recipe, recipe.getInputMaterials().getFirst(), new LinkedHashMap<>(inputMap)));
                yield list;
            }
            case null -> throw new IllegalStateException("Invalid machine type");
        };
    }


    protected void handleShiftLeft(GUI gui, InventoryClickEvent e, ItemStack current, List<Integer> list) {
        list.stream()
                .map(slot -> new Pair<>(gui.getItem(slot), slot))
                .filter(pair -> {
                    ItemStack a = pair.getKey();
                    return a != null && a.getType() == current.getType() && a.getAmount() != a.getMaxStackSize() && a.isSimilar(current);
                }).forEach(pair -> {
                    ItemStack item = pair.getKey();
                    int slot = pair.getValue();

                    int max = item.getMaxStackSize();
                    if (item.getAmount() + current.getAmount() >= max) {
                        int toRemove = max - item.getAmount();
                        current.setAmount(current.getAmount() - toRemove);
                        item = item.asQuantity(max);
                    } else {
                        item = item.add(current.getAmount());
                        current.setAmount(0);
                    }

                    e.setCurrentItem(current);
                    gui.getInventory().setItem(slot, item);
                });

        if (current.getAmount() == 0) return;

        for (int i : list) {
            ItemStack itemStack = gui.getInventory().getItem(i);
            if (itemStack == null) {
                e.setCurrentItem(null);
                gui.getInventory().setItem(i, current);
                break;
            }
            if (itemStack.getType() != current.getType()) continue;
            if (itemStack.getAmount() == itemStack.getMaxStackSize()) continue;
            if (!itemStack.isSimilar(current)) continue;

            int max = itemStack.getMaxStackSize();

            if (itemStack.getAmount() + current.getAmount() > max) {
                int toRemove = max - itemStack.getAmount();
                current.setAmount(current.getAmount() - toRemove);
                e.setCurrentItem(current);
                gui.getInventory().setItem(i, itemStack.asQuantity(max));
                continue;
            } else if (itemStack.getAmount() + current.getAmount() == max) {
                current.setAmount(0);
                e.setCurrentItem(current);
                gui.getInventory().setItem(i, itemStack.asQuantity(max));
                continue;
            }
            itemStack = itemStack.add(current.getAmount());
            current.setAmount(0);
            e.setCurrentItem(current);
            gui.getInventory().setItem(i, itemStack);
        }
    }

    protected boolean isEmptyUpdate(GUI gui, ItemGrid itemGrid, Machine machine) {
        List<ItemStack> fuelItems = new ArrayList<>();
        List<ItemStack> inputItems = new ArrayList<>();
        List<ItemStack> outputItems = new ArrayList<>();

        for (int i : itemGrid.getInputSlots()) {
            ItemStack item = gui.getItem(i);
            if (item == null)
                continue;
            inputItems.add(gui.getItem(i));
        }
        for (int i : itemGrid.getFuelSlots()) {
            ItemStack item = gui.getItem(i);
            if (item == null)
                continue;
            fuelItems.add(gui.getItem(i));
        }

        int outputSlots = itemGrid.getOutputSlots().size();
        for (int i : itemGrid.getOutputSlots()) {
            ItemStack item = gui.getItem(i);
            if (item == null)
                continue;
            if (item.getMaxStackSize() == item.getAmount()) {
                outputItems.add(item);
            }
        }
        if(fuelItems.isEmpty()){
            machine.setMachineState(MachineState.WAITING_FUEL);
        }else if(inputItems.isEmpty()){
            machine.setMachineState(MachineState.WAITING_INPUT);
        }else if(outputItems.size()==outputSlots){
            machine.setMachineState(MachineState.FULL);
        }

        return fuelItems.isEmpty() || inputItems.isEmpty() || outputItems.size() == outputSlots;
    }

    protected void addItems(GUI gui, SimpleItem si, int index) {
        ItemStack item = gui.getItem(index);
        if (item == null) {
            item = si.getStackCached().clone();
            gui.getInventory().setItem(index, item);
            return;
        }
        item.setAmount(item.getAmount() + si.getAmount());
        gui.getInventory().setItem(index, item);
    }

    protected void removeInputItems(Machine machine, GUI gui, SimpleItem si, Map<Integer, ItemStack> map) {
        if (machine.getMachineType() == MachineType.RUBBLE_PROCESSOR) {
            // from all 4 slots we take
            for (Map.Entry<Integer, ItemStack> entry : map.entrySet()) {
                ItemStack item = entry.getValue();
                int toSet = Math.max(item.getAmount() - si.getAmount(), 0);
                gui.getInventory().setItem(entry.getKey(), toSet == 0 ? null : item.asQuantity(toSet));
            }
        } else if (machine.getMachineType() == MachineType.CRAFTING_MACHINE) {
            // Each input listed in the recipe must be consumed in full from whichever slot(s) carry it.
            // Walk the recipe's inputs and decrement amounts across the snapshot map (smallest first).
            if (!(machine instanceof CraftingMachine cm)) return;
            String activeId = cm.getActiveRecipeId();
            if (activeId == null) return;
            HashMap<String, RecipeItem> index = Loader.recipeByIdIndex.get(MachineType.CRAFTING_MACHINE);
            RecipeItem recipe = index == null ? null : index.get(activeId);
            if (recipe == null) return;

            for (SimpleItem ingredient : recipe.getInputMaterials()) {
                int remaining = ingredient.getAmount();
                // Iterate slots that carry this ingredient
                for (Map.Entry<Integer, ItemStack> entry : map.entrySet()) {
                    if (remaining <= 0) break;
                    ItemStack stack = gui.getInventory().getItem(entry.getKey());
                    if (stack == null) continue;
                    if (!isSameKind(ingredient, stack)) continue;

                    int take = Math.min(remaining, stack.getAmount());
                    int leftover = stack.getAmount() - take;
                    gui.getInventory().setItem(entry.getKey(), leftover == 0 ? null : stack.asQuantity(leftover));
                    remaining -= take;
                }
            }
        } else {
            for (Map.Entry<Integer, ItemStack> entry : map.entrySet()) {
                if (entry.getValue() == null || gui.getInventory().getItem(entry.getKey()) == null)
                    continue;
                ItemStack item = entry.getValue();
                int index = entry.getKey();
                int toSet = Math.max(item.getAmount() - si.getAmount(), 0);
                gui.getInventory().setItem(index, toSet == 0 ? null : item.asQuantity(toSet));
                break;
            }
        }
    }

    private static boolean isSameKind(SimpleItem ingredient, ItemStack stack) {
        if (ingredient.getNexoId() != null) {
            return ingredient.getNexoId().equals(NexoItems.idFromItem(stack));
        }
        return ingredient.getMaterial() == stack.getType();
    }

    protected void removeFuelItems(GUI gui, SimpleItem si, int index) {
        ItemStack item = gui.getItem(index);
        if (item == null)
            return;
        int toSet = Math.max(item.getAmount() - si.getAmount(), 0);
        gui.getInventory().setItem(index, toSet == 0 ? null : item.asQuantity(toSet));
    }
}

