package me.petulikan1.zMachines.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.menu.items.ItemGrid;
import me.petulikan1.zMachines.messages.MiniImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashMap;

@RequiredArgsConstructor
@Getter
public class MenuConfig {

    private final String key;


    private static final HashMap<String, MenuConfig> cache = new HashMap<>();

    public static MenuConfig init(String key, final int tier) {
        if (cache.containsKey(key + tier))
            return cache.get(key + tier);
        if (!Loader.menuCfg.exists("Menus." + key)) {
            Loader.main.error("Menus." + key + " not found!");
            return null;
        }

        MenuConfig config = new MenuConfig(key);
        config.getItemGrid(tier);
        cache.put(key + tier, config);
        return config;
    }

    public String getMenuPath() {
        return "Menus." + key;
    }

    public Component getTitle(TagResolver... resolvers) {
        return new MiniImpl(Loader.menuCfg, getMenuPath(), "Title", resolvers);
    }

    private InventoryType type;

    public InventoryType getInventoryType() {
        if (type != null) {
            return type;
        }
        String val = Loader.menuCfg.getString(getMenuPath() + ".InventoryType");
        if (val == null)
            return (type = InventoryType.CHEST);
        for (InventoryType t : InventoryType.values()) {
            if (t.name().equalsIgnoreCase(val)) {
                return (type = t);
            }
        }
        Loader.main.error("Invalid InventoryType: " + val + " | Defaulting to Chest...");
        return (type = InventoryType.CHEST);
    }

    private ItemGrid itemGrid;

    public ItemGrid getItemGrid(final int tier) {
        if (itemGrid == null)
            itemGrid = new ItemGrid(Loader.menuCfg, getMenuPath() + ".ItemGrid." + tier, getInventoryType());
        return itemGrid;
    }

    public int getSize() {
        // Chest menus size themselves from the number of configured ItemGrid lines (rows * 9),
        // so adding/removing a line in menus.yml grows/shrinks the menu automatically.
        if (getInventoryType() == InventoryType.CHEST && itemGrid != null) {
            return itemGrid.getRows() * 9;
        }
        return Loader.menuCfg.getInt(getMenuPath() + ".Size");
    }

    private static void clearCache() {
        Loader.main.loG("Reloading MenuConfig cache...");
        cache.clear();
    }

}
