package me.petulikan1.zMachines.menu;

import lombok.Getter;
import lombok.Setter;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.config.MenuConfig;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.messages.Formatter;
import me.petulikan1.zMachines.messages.Mini;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GUI implements HolderGUI {

    private Component title;
    private final Inventory inv;
    private final Map<Integer, ItemGUI> items = new ConcurrentHashMap<>();
    @Setter
    @Getter
    private boolean insertable = false;


    public GUI(Component original, int originalSize, InventoryType type, Player... p) {
        title = original;
        int size;
        switch (originalSize) {
            case 17:
            case 18:
            case 19:
                size = 18;
                break;
            case 26:
            case 27:
            case 28:
                size = 27;
                break;
            case 35:
            case 36:
            case 37:
                size = 36;
                break;
            case 44:
            case 45:
            case 46:
                size = 45;
                break;
            default:
                if (originalSize > 46)
                    size = 54;
                else
                    size = 9;
                break;
        }
        if (type == InventoryType.CHEST) {
            inv = Bukkit.createInventory(null, size, title);
        } else {
            inv = Bukkit.createInventory(null, type, title);
        }
        open(p);
    }

    @Override
    public boolean onInteract(Player player, HolderGUI gui, ClickType type, int slot, boolean upInventory, InventoryClickEvent e) {
        return false;
    }

    @Override
    public void onClose(Player player) {

    }

    @Override
    public void onOpen(Player player) {

    }


    @Override
    public boolean isTopInventory(Inventory inventory) {
        return inv.equals(inventory);
    }

    @Override
    public final void setTitle(Component title) {
        this.title = title;
    }

    @Override
    public final void setItem(int slot, ItemGUI item) {
        items.put(slot, item);
        inv.setItem(slot, item.getItem());
    }

    @Override
    public final void setItem(Collection<Integer> slots, ItemGUI gui) {
        for (int i : slots) {
            if (i >= inv.getSize())
                continue;
            setItem(i, gui);
        }
    }

    public final void removeItem(int slot) {
        items.remove(slot);
        inv.setItem(slot, null);
    }

    @Override
    public void remove(int slot) {
        removeItem(slot);
    }

    public final void addItem(ItemGUI item) {
        if (getFirstEmpty() != -1)
            setItem(getFirstEmpty(), item);
    }

    public final void add(ItemGUI item) {
        addItem(item);
    }

    @Override
    public ItemStack getItem(int slot) {
        try {
            return inv.getItem(slot);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ItemGUI getItemGUI(int slot) {
        return getItemGUIs().get(slot);
    }

    @Override
    public Map<Integer, ItemGUI> getItemGUIs() {
        return items;
    }

    public final int getFirstEmpty() {
        return inv.firstEmpty();
    }

    public final void open(Player... players) {
        for (Player player : players) {
            if (Loader.gui.containsKey(player.getUniqueId())) {
                HolderGUI a = Loader.gui.get(player.getUniqueId());
                Loader.gui.remove(player.getUniqueId());
                a.onClose(player);
            }
            Loader.gui.put(player.getUniqueId(), this);
            player.openInventory(inv);
            onOpen(player);
        }
    }


    @Override
    public Inventory getInventory() {
        return inv;
    }

    @Override
    public void close(Player... p) {
        if (p == null)
            return;
        for (Player player : p) {
            if (player == null)
                continue;

            Loader.gui.remove(player.getUniqueId());
            if (inv.getViewers().contains(player))
                inv.getViewers().get(inv.getViewers().indexOf(player)).closeInventory();
            onClose(player);
        }
    }

    @Override
    public void closeAll() {
        for (HumanEntity p : new ArrayList<>(inv.getViewers())) {
            if (p == null)
                continue;
            Loader.gui.remove(p.getUniqueId());
            p.closeInventory();
            onClose((Player) p);
        }
    }

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public static GUI create(String key, Player player, Machine machine, TagResolver... resolvers) {
        lock.readLock().lock();
        MenuConfig config = MenuConfig.init(key, machine.getTier());
        if (config == null) {
            Loader.main.error("Menu with key &e" + key + "&c doesn't exists");
            Mini.mm(player, "Menu.MissingMenu", Formatter.ps("key", key));
            lock.readLock().unlock();
            return null;
        }
        try {

            GUI g = new GUI(config.getTitle(Placeholder.component("tier", Component.text(machine.getTier()))), config.getSize(), config.getInventoryType()) {
                @Override
                public void onClose(Player player) {
                    machine.onClose(player, this, config.getItemGrid());
                }

                @Override
                public void onOpen(Player player) {
                    Bukkit.getScheduler().runTask(Loader.main, () -> {
                        machine.tickStart(this, config.getItemGrid());
                        Bukkit.getScheduler().runTaskTimer(Loader.main, (task) -> {
                            if (this.getInventory().getViewers().isEmpty()) {
                                machine.tickStop(this, config.getItemGrid());
                                task.cancel();
                                return;
                            }
                            try {
                                machine.onTick(this, config.getItemGrid());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, 1, 1);
                    });
                }

                @Override
                public boolean onInteract(Player player, HolderGUI gui, ClickType type, int slot, boolean upInventory, InventoryClickEvent e) {
                    return machine.onInteract(player, this, type, slot, upInventory, e, config.getItemGrid());
                }
            };
            config.getItemGrid().fillItemsToGUI(g, player, machine, resolvers);
            return g;
        } catch (Exception e) {
            Loader.main.error("Error while opening GUI with key " + key + " for player: " + player.getName());
            e.printStackTrace();
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
}
