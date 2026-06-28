package me.petulikan1.zMachines.dataholders.impl;

import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.dataholders.LocationInternal;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.items.RecipeItem;
import me.petulikan1.zMachines.items.SimpleItem;
import me.petulikan1.zMachines.menu.GUI;
import me.petulikan1.zMachines.menu.items.ItemGrid;
import me.petulikan1.zMachines.utils.Triplet;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class GrowingMachine extends Machine {

    public GrowingMachine(LocationInternal location, int id, UUID owner, int tier) {
        super(location, id, tier, owner, MachineType.GROWING_MACHINE, "GrowingMachine");
    }

    @Override
    public boolean acceptsInput(ItemStack item) {
        // Growing recipes are indexed by input material (seeds); a non-null lookup means it's valid.
        return item != null && Loader.getRecipeItem(item, this) != null;
    }

    @Override
    protected boolean consumesInputs() {
        return false;
    }

    @Override
    protected boolean processAllRecipesPerTick() {
        return true;
    }

    @Override
    protected void onRecipeProgress(GUI gui, ItemGrid itemGrid,
                                    Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>> recipe,
                                    int progress) {
        recipe.getC().keySet().stream().findFirst().ifPresent(slot ->
                updateProgressItem(gui, itemGrid, itemGrid.getInputSlots().indexOf(slot), progress));
    }
}
