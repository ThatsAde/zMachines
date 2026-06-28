package me.petulikan1.zMachines.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class Formatter {
    public static @NotNull TagResolver number(@TagPattern final @NotNull String key, final @NotNull Number number) {
        return TagResolver.resolver(key, (argumentQueue, context) -> {
            Object decimalFormat;
            if (argumentQueue.hasNext()) {
                String locale = argumentQueue.pop().value();
                if (argumentQueue.hasNext()) {
                    String format = argumentQueue.pop().value();
                    decimalFormat = new DecimalFormat(format, new DecimalFormatSymbols(Locale.forLanguageTag(locale)));
                } else if (locale.contains(".")) {
                    decimalFormat = new DecimalFormat(locale, DecimalFormatSymbols.getInstance());
                } else {
                    decimalFormat = DecimalFormat.getInstance(Locale.forLanguageTag(locale));
                }
            } else {
                decimalFormat = DecimalFormat.getInstance();
            }

            return Tag.inserting(context.deserialize(((NumberFormat) decimalFormat).format(number)));
        });
    }

    public static @NotNull TagResolver date(@TagPattern final @NotNull String key, final @NotNull TemporalAccessor time) {
        return TagResolver.resolver(key, (argumentQueue, context) -> {
            String format = argumentQueue.popOr("Format expected.").value();
            return Tag.inserting(context.deserialize(DateTimeFormatter.ofPattern(format).format(time)));
        });
    }

    public static @NotNull TagResolver choice(@TagPattern final @NotNull String key, final Number number) {
        return TagResolver.resolver(key, (argumentQueue, context) -> {
            String format = argumentQueue.popOr("Format expected.").value();
            ChoiceFormat choiceFormat = new ChoiceFormat(format);
            return Tag.inserting(context.deserialize(choiceFormat.format(number)));
        });
    }

    public static TagResolver booleanChoice(@TagPattern final @NotNull String key, final boolean value) {
        return TagResolver.resolver(key, (argumentQueue, context) -> {
            String trueCase = argumentQueue.popOr("True format expected.").value();
            String falseCase = argumentQueue.popOr("False format expected.").value();
            return Tag.inserting(context.deserialize(value ? trueCase : falseCase));
        });
    }

    enum PageType {
        PREV,
        NEXT;

        @Nullable
        public static PageType get(String type) {
            for (PageType p : values()) {
                if (p.name().equalsIgnoreCase(type))
                    return p;
            }
            return null;
        }
    }

    public static TagResolver ps(@TagPattern String key, String text) {
        return Placeholder.component(key, Component.text(text));
    }
}
