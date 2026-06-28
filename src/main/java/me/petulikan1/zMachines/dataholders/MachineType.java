package me.petulikan1.zMachines.dataholders;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum MachineType {

    RUBBLE_PROCESSOR("RubbleProcessor", Material.FURNACE),
    GROWING_MACHINE("GrowingMachine", Material.COMPOSTER),
    CRAFTING_MACHINE("CraftingMachine", Material.CRAFTING_TABLE);

    // Config key used in config.yml / menus.yml (e.g. "RubbleProcessor"). Also doubles as the
    // Machine#identifier passed to the Machine constructor.
    private final String identifier;
    // Fallback Bukkit material when the config value can't be resolved (e.g. invalid Nexo id).
    private final Material defaultMaterial;

    MachineType(String identifier, Material defaultMaterial) {
        this.identifier = identifier;
        this.defaultMaterial = defaultMaterial;
    }

    @Nullable
    public static MachineType getMachineType(final String machineType) {
        for (MachineType m : MachineType.values()) {
            // Match either the enum name (GROWING_MACHINE) or the config identifier (GrowingMachine),
            // case-insensitively, so "GrowingMachine", "Growingmachine" and "GROWING_MACHINE" all resolve.
            if (m.name().equalsIgnoreCase(machineType) || m.identifier.equalsIgnoreCase(machineType)) {
                return m;
            }
        }
        return null;
    }


    public static List<Component> getMachineTypeComponents() {
        List<Component> components = new ArrayList<>();
        for (MachineType m : MachineType.values()) {
            components.add(Component.text(m.name()));
        }
        return components;
    }

}
