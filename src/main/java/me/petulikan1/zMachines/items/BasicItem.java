package me.petulikan1.zMachines.items;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.config.Config;
import me.petulikan1.zMachines.messages.MiniImpl;
import me.petulikan1.zMachines.utils.PDC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class BasicItem {
    private final String key;
    private final Config config;
    private final int modelData;
    private final boolean hideTooltip;
    private final int amount;
    private final List<ItemFlag> itemFlags;
    private final boolean glow;
    private final double itemVersion;

    @Setter
    @Getter
    private Consumer<PDC> nbtConsumer;

    public BasicItem(Config config, String key) {
        this.key = key;
        this.config = config;
        modelData = config.getInt(key + ".ModelData");
        hideTooltip = config.getBoolean(key + ".HideTooltip");
        int a = config.getInt(key + ".Amount");
        amount = a == 0 ? 1 : a;
        glow = config.getBoolean(key + ".Glow");
        double tempVersion = config.getDouble(key + ".ItemVersion", -1.9);
        if (tempVersion == -1.9) {
            Loader.main.error("Missing ItemVersion key! Item: " + key + ".ItemVersion | Defaulting to 1.0");
            itemVersion = 1.0;
        } else {
            itemVersion = tempVersion;
        }
        itemFlags = retrieveItemFlags();
    }

    private ItemFlag getFlag(String value) {
        for (ItemFlag flag : ItemFlag.values()) {
            if (flag.name().equalsIgnoreCase(value))
                return flag;
        }
        return null;
    }

    public List<ItemFlag> retrieveItemFlags() {
        List<ItemFlag> itemFlags = new ArrayList<>();
        for (String a : config.getStringList(key + ".ItemFlags")) {
            ItemFlag flag = getFlag(a);
            if (flag != null)
                itemFlags.add(flag);
            else
                Loader.main.warn("Detected invalid ItemFlag '" + a + "'");
        }
        return itemFlags;
    }

    private Component fixItalics(Component c) {
        if (c.decorations().containsKey(TextDecoration.ITALIC)) {
            TextDecoration.State state = c.decorations().get(TextDecoration.ITALIC);
            if (state != TextDecoration.State.TRUE)
                return c.decoration(TextDecoration.ITALIC, false);
        }
        return c;
    }

    public Component displayName(TagResolver... resolvers) {
        if (!config.existsKey(key + ".Display")) {
            return Component.empty();
        }
        return fixItalics(MiniImpl.getMm().deserialize(config.getString(key + ".Display"), resolvers));
    }

    public List<Component> getLore(TagResolver... resolvers) {
        List<Component> c = new ArrayList<>();
        for (String a : config.getStringList(key + ".Lore")) {
            c.add(fixItalics(MiniImpl.getMm().deserialize(a, resolvers)));
        }
        return c;
    }

    private final Base64.Decoder DECODER = Base64.getDecoder();
    private final Gson GSON = new Gson();

    public ItemStack build(Material m, TagResolver... resolvers) {
        ItemStack item = new ItemStack(m, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(modelData);
        meta.displayName(displayName(resolvers));
        meta.lore(getLore(resolvers));
        meta.setHideTooltip(hideTooltip);
        meta.addItemFlags(itemFlags.toArray(ItemFlag[]::new));

        if (nbtConsumer != null) {
            nbtConsumer.accept(new PDC(meta));
        }
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

}
