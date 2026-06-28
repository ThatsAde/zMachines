package me.petulikan1.zMachines.menu.items;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.config.Config;
import me.petulikan1.zMachines.messages.MiniImpl;
import me.petulikan1.zMachines.utils.PDC;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
public class MenuItem {
    private final String key;
    private final Config config;
    private final Material material;
    private final int modelData;
    private final boolean hideTooltip;
    private final int amount;
    private final List<ItemFlag> itemFlags;
    private URL skullURL;
    private final boolean glow;
    private final ClickSound clickSound;

    @Setter
    private Consumer<PDC> nbtConsumer;

    private boolean playerSkullURL;

    private final MenuActionWrapper action;

    public MenuItem(Config config, String key) {
        this(config, key, null);
    }

    public MenuItem(Config config, String key, @Nullable MenuActionWrapper action) {
        this.key = key;
        this.config = config;
        Material m = Material.getMaterial(config.getString(key + ".Material", "BEDROCK"));
        if (m == null)
            m = Material.BEDROCK;
        this.material = m;
        modelData = config.getInt(key + ".ModelData");
        hideTooltip = config.getBoolean(key + ".HideTooltip");
        int a = config.getInt(key + ".Amount");
        amount = a == 0 ? 1 : a;
        glow = config.getBoolean(key + ".Glow");
        clickSound = new ClickSound(key + ".Sound", config);
        if (m == Material.PLAYER_HEAD) {
            if (!config.exists(key + ".SkullValue")) {
                Loader.main.error("Detected PLAYER_HEAD but 'SkullValue' was not found!");
            }
            String skullValue;
            skullValue = config.getString(key + ".SkullValue");
            if (skullValue.isEmpty()) {
                Loader.main.error("Detected PLAYER_HEAD - 'SkullValue' was found but is empty!");
            } else {
                if (!skullValue.equalsIgnoreCase("<player>")) {
                    try {
                        skullURL = URI.create(skullValue).toURL();
                    } catch (Exception e) {
                        // not an URL, probably base64
                        String decoded = new String(DECODER.decode(skullValue));
                        try {
                            JsonObject texturesObject = GSON.fromJson(decoded, JsonObject.class);
                            if (texturesObject != null && texturesObject.has("textures")) {
                                JsonObject skinObject = texturesObject.get("textures").getAsJsonObject();
                                if (skinObject != null && skinObject.has("SKIN")) {
                                    JsonObject urlObject = skinObject.get("SKIN").getAsJsonObject();
                                    if (urlObject != null && urlObject.has("url")) {
                                        skullURL = URI.create(urlObject.get("url").getAsString()).toURL();
                                    }
                                }
                            }
                        } catch (Exception ee) {

                            Loader.main.error("Failed to deserialize skull value: " + skullValue + " | Using default author's");
                            try {
                                skullURL = URI.create("http://textures.minecraft.net/texture/da7a9adc25db0728fdc444ba28de58ca61cdd4217ea123f12671d3037468bcf3").toURL();
                            } catch (Exception ignored) {}
                        }
                    }
                } else {
                    playerSkullURL = true;
                }
            }
        }
        itemFlags = retrieveItemFlags();
        this.action = action;
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

    private void applySkullMeta(Player player, ItemMeta meta) {
        if (meta instanceof SkullMeta skullMeta) {
            if (skullURL != null) {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(skullURL);
                profile.setTextures(textures);
                skullMeta.setPlayerProfile(profile);
            } else if (playerSkullURL && player != null) {
                skullMeta.setPlayerProfile(player.getPlayerProfile());
            }
        }
    }

    public ItemStack build(@Nullable Player player, TagResolver... resolvers) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(modelData);
            meta.displayName(displayName(resolvers));
            meta.lore(getLore(resolvers));
            meta.setHideTooltip(hideTooltip);
            meta.addItemFlags(itemFlags.toArray(ItemFlag[]::new));
            applySkullMeta(player, meta);

            if (nbtConsumer != null) {
                nbtConsumer.accept(new PDC(meta));
            }
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    public static class ClickSound {
        Sound sound;

        @SuppressWarnings("PatternValidation")
        public ClickSound(String key, Config config) {
            if (!config.exists(key))
                return;
            String soundID = config.getString(key + ".SoundID", null);
            if (soundID == null) {
                Loader.main.error("Sound id for key: '" + key + ".SoundID' is null!");
                return;
            }
            float pitch = config.getFloat(key + ".Pitch", -10);
            if (pitch == -10) {
                Loader.main.error("Missing pitch key for sound: '" + key + ".Pitch'");
                return;
            }
            Key k;
            try {
                k = Key.key(soundID);
            } catch (Exception e) {
                Loader.main.error("Error while parsing SoundID for sound key: '" + key + ".SoundID' | Error: " + e.getMessage());
                return;
            }
            sound = Sound.sound(k, Sound.Source.NEUTRAL, 1, pitch);
        }


        public void play(Player player) {
            if (sound == null)
                return;
            if (cooldown.contains(player.getUniqueId()))
                return;
            cooldown.add(player.getUniqueId());
            API.runTaskAsync(() -> cooldown.remove(player.getUniqueId()), 50, TimeUnit.MILLISECONDS);
            player.playSound(sound, Sound.Emitter.self());
        }

        private final Set<UUID> cooldown = new HashSet<>();

    }
}
