package me.petulikan1.zMachines.messages;

import me.petulikan1.zMachines.Loader;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface Mini extends TextComponent {

    static Mini mm(@NonNull String text, TagResolver... tagResolvers) {
        return new MiniImpl(Loader.getTranslations(), "Translations", text, tagResolvers);
    }

    static void mm(@NonNull CommandSender s, @NonNull String text, @NonNull TagResolver... resolvers) {
        s.sendMessage(mm(text, resolvers));
    }


}
