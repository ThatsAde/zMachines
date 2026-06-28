package me.petulikan1.zMachines.dataholders.impl;

import me.petulikan1.zMachines.dataholders.LocationInternal;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineType;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class RubbleProcessor extends Machine {

    public RubbleProcessor(LocationInternal location, int id, UUID owner, int tier) {
        super(location, id, tier, owner, MachineType.RUBBLE_PROCESSOR, "RubbleProcessor");
    }

    @Override
    public boolean acceptsInput(ItemStack item) {
        // Rubble recipes accept any block tagged BASE_STONE_OVERWORLD (matches InventoryUtils routing).
        return item != null && Tag.BASE_STONE_OVERWORLD.isTagged(item.getType());
    }
}
