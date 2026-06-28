package me.petulikan1.zMachines.items;

import com.nexomc.nexo.api.NexoItems;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Getter
public class SimpleItem {

    private final Material material;
    private final int amount;
    private final String nexoId; // nullable; when set, this is a Nexo item

    private ItemStack stack;

    // Vanilla-only constructor — keeps backward compatibility with all existing call sites.
    public SimpleItem(Material material, int amount) {
        this(material, amount, null);
    }

    // Nexo-aware constructor — material may be a fallback (typically BEDROCK) when nexoId is set.
    public SimpleItem(Material material, int amount, String nexoId) {
        this.material = material;
        this.amount = amount;
        this.nexoId = nexoId;
    }

    public SimpleItem setAmount(int amount) {
        return new SimpleItem(this.material, amount, this.nexoId);
    }

    /**
     * Returns true if the given stack matches this ingredient (correct type AND has at least {@code amount}).
     * Nexo items are matched by their Nexo ID; vanilla items by Material.
     */
    public boolean matches(ItemStack stack) {
        if (stack == null) return false;
        if (nexoId != null) {
            return nexoId.equals(NexoItems.idFromItem(stack)) && stack.getAmount() >= amount;
        }
        return stack.getType() == material && stack.getAmount() >= amount;
    }

    /**
     * Returns true if the given stack is the same KIND of item as this ingredient (ignores amount).
     */
    public boolean isSameKind(ItemStack stack) {
        if (stack == null) return false;
        if (nexoId != null) {
            return nexoId.equals(NexoItems.idFromItem(stack));
        }
        return stack.getType() == material;
    }

    public ItemStack getStackCached() {
        if (stack == null) {
            if (nexoId != null) {
                ItemStack base = me.petulikan1.zMachines.hooks.Nexo.getNexoItem(nexoId);
                base.setAmount(amount);
                stack = base;
            } else {
                stack = ItemStack.of(material, amount);
            }
        }
        return stack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleItem other)) return false;
        return amount == other.amount
                && material == other.material
                && Objects.equals(nexoId, other.nexoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, amount, nexoId);
    }
}
