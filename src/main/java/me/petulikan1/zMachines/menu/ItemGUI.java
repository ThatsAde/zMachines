package me.petulikan1.zMachines.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class ItemGUI {

    private ItemStack s;

    public ItemGUI(ItemStack s) {
        this.s = s;
    }

    public abstract boolean onClick(Player player, HolderGUI gui, ClickType clickType, int slot, InventoryAction action, InventoryClickEvent e);

    public final ItemStack getItem() {
        return this.s;
    }

    public final ItemGUI setItem(ItemStack stack) {
        if (stack != null)
            this.s = stack;
        return this;
    }

}
