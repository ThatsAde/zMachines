package me.petulikan1.zMachines.messages;

import lombok.Getter;
import lombok.NonNull;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.config.Config;
import me.petulikan1.zMachines.utils.StringUtils;
import me.petulikan1.zMachines.utils.Validator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MiniImpl implements Mini {

    private final TextComponent tc;

    public MiniImpl(@NonNull Config config, @NonNull String rootPath, @NonNull String path) {
        tc = config.get(rootPath + "." + path) instanceof List<?> ? (TextComponent) getTrLC(config, rootPath, path) : (TextComponent) getTrC(config, rootPath, path);
    }

    public MiniImpl(@NonNull Config config, @NonNull String rootPath, @NonNull String path, TagResolver... tagResolvers) {
        tc = config.get(rootPath + "." + path) instanceof List<?> ? (TextComponent) getTrLC(config, rootPath, path, tagResolvers) : (TextComponent) getTrC(config, rootPath, path, tagResolvers);
    }


    @Override
    public @NotNull String content() {
        return tc.content();
    }

    @Override
    public @NotNull TextComponent content(@NotNull String content) {
        return tc.content(content);
    }

    @Override
    public @NotNull Builder toBuilder() {
        return tc.toBuilder();
    }

    @Override
    public @Unmodifiable @NotNull List<Component> children() {
        return tc.children();
    }

    @Override
    public @NotNull TextComponent children(@NotNull List<? extends ComponentLike> children) {
        return tc.children(children);
    }

    @Override
    public @NotNull Style style() {
        return tc.style();
    }

    @Override
    public @NotNull TextComponent style(@NotNull Style style) {
        return tc.style(style);
    }

    @Getter
    private final static MiniMessage mm = MiniMessage.miniMessage();
    private final static LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection().toBuilder().useUnusualXRepeatedCharacterHexFormat().hexColors().build();

    public static Component getTrLC(@NonNull Config config, @NonNull String rootPath, @NonNull String path, @NonNull TagResolver... tagResolvers) {
        List<String> list = getTrL2(config, rootPath, path);
        String text = String.join("\n", list);
        if (text.contains("&")) {
            Loader.main.error("Path: " + path + " contains unsupported character - '&'");
            return Component.text("Path: " + path + " contains unsupported character - '&'", NamedTextColor.RED);
        }
        return mm.deserialize(text, convert(tagResolvers, getPrefixPlaceholder()));
    }

    public Component getTrC(@NonNull Config config, @NonNull String rootPath, @NonNull String path, @NonNull TagResolver... tagResolvers) {
        String text = getTr2(config, rootPath, path);
        if (text == null) {
            return Component.text("No value found for path: Translations." + path, NamedTextColor.RED);
        }
        if (text.contains("&")) {
            Loader.main.error("Path: " + path + " contains unsupported character - '&'");
            return Component.text("Path: " + path + " contains unsupported character - '&'", NamedTextColor.RED);
        }
        return mm.deserialize(text, convert(tagResolvers, getPrefixPlaceholder()));
    }

    public static TagResolver convert(TagResolver[] resolver, TagResolver... resolvers) {
        return TagResolver.builder().resolvers(resolvers).resolvers(resolver).build();
    }

    public static Component getTrC(@NonNull Config config, @NonNull String rootPath, @NonNull String path) {
        String text = getTr2(config, rootPath, path);
        if (text == null) {
            return Component.text("No value found for path: Translations." + path, NamedTextColor.RED);
        }
        if (text.contains("&")) {
            Loader.main.error("Path: " + path + " contains unsupported character - '&'");
            return Component.text("Path: " + path + " contains unsupported character - '&'", NamedTextColor.RED);
        }
        return mm.deserialize(text, getPrefixPlaceholder());
    }

    public static String getPrefix() {
        return Loader.getTranslations().getString("Prefix", "<gradient:#ae78ff:#ff5964>zMachines</gradient> <gray>»");
    }

    public static Component parse(String a) {
        return mm.deserialize(a);
    }

    public static TagResolver getPrefixPlaceholder() {
        return TagResolver.builder().resolver(Placeholder.parsed("prefix", getPrefix())).build();
    }

    public static String getTr2(@NonNull Config config, @NonNull String rootPath, @NonNull String path) {
        Validator.validate2(config.existsKey(rootPath + "." + path), "Path: " + rootPath + "." + path + " doesn't exists!");
        return config.getString(rootPath + "." + path);
    }

    public static List<String> getTrL2(@NonNull Config config, @NonNull String rootPath, @NonNull String path) {
        Validator.validate2(config.existsKey(rootPath + "." + path), "Path: " + rootPath + "." + path + " doesn't exists!");
        return config.getStringList(rootPath + "." + path);
    }

    public static void msgConsole(Object message) {
        if (message instanceof Map) {
            for (Map.Entry<?, ?> loop : ((Map<?, ?>) message).entrySet()) {
                msg(loop.getKey() + " " + loop.getValue(), Loader.getConsole());
            }
        } else if (message instanceof Collection) {
            for (Object loop : (Collection<?>) message) {
                msg(loop + "", Loader.getConsole());
            }
        } else msg(message + "", Loader.getConsole());
    }


    public static void msg(String message, CommandSender sender) {
        Validator.validate(sender == null, "CommandSender is null");
        Validator.validate(message == null, "Message is null");
        String[] split = StringUtils.colorize(message.replace("\\n", "\n")).split("\n");
        String old = "";
        for (String s : split) {
            String newMessage = old + s;
            sender.sendMessage(legacy.deserialize(newMessage));
            if (StringUtils.color != null)
                old = StringUtils.colorize("&" + StringUtils.color.getLastColors(s)[0]);
            else
                old = s;
        }
    }
}
