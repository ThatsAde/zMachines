package me.petulikan1.zMachines.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

public interface HolderGUI {

    void onClose(Player player);


    void onOpen(Player player);

    void setTitle(Component c);

    void setItem(int slot, ItemGUI item);

    void setItem(Collection<Integer> slots, ItemGUI gui);

    void remove(int slot);

    ItemStack getItem(int slot);

    ItemGUI getItemGUI(int slot);

    Map<Integer, ItemGUI> getItemGUIs();

    Inventory getInventory();

    void close(Player... p);

    void closeAll();

    boolean onInteract(Player player, HolderGUI gui, ClickType type, int slot, boolean upInventory, InventoryClickEvent e);

    boolean isTopInventory(Inventory inventory);

}
