package me.petulikan1.zMachines.database.adapters;

import com.google.gson.*;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Base64;

public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    private final Base64.Decoder decoder = Base64.getUrlDecoder();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        return ItemStack.deserializeBytes(decoder.decode(jsonElement.getAsString()));
    }

    @Override
    public JsonElement serialize(ItemStack itemStack, Type type, JsonSerializationContext ctx) {
        return new JsonPrimitive(encoder.encodeToString(itemStack.serializeAsBytes()));
    }
}
