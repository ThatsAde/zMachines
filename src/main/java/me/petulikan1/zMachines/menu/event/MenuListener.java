package me.petulikan1.zMachines.menu.event;

import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.menu.HolderGUI;
import me.petulikan1.zMachines.menu.ItemGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.UUID;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (Loader.gui.containsKey(uuid)) {
            HolderGUI gui = Loader.gui.get(uuid);
            gui.close((Player) e.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(InventoryClickEvent e) {
        UUID uuid = e.getWhoClicked().getUniqueId();
        if (Loader.gui.containsKey(uuid)) {
            HolderGUI gui = Loader.gui.get(uuid);
            ItemGUI itemGUI = gui.getItemGUI(e.getRawSlot());
            boolean allow;
            if (itemGUI != null) {
                allow = itemGUI.onClick(((Player) e.getWhoClicked()), gui, e.getClick(), e.getSlot(), e.getAction(), e);
            } else {
                allow = gui.onInteract(((Player) e.getWhoClicked()), gui, e.getClick(), e.getSlot(), gui.getInventory().equals(e.getClickedInventory()), e);
            }
            e.setCancelled(!allow);
        }
    }

    @EventHandler
    public void onItemDrag(InventoryDragEvent e) {
        UUID uuid = e.getWhoClicked().getUniqueId();
        if (Loader.gui.containsKey(uuid)) {
            e.setCancelled(true);
        }
    }

}
