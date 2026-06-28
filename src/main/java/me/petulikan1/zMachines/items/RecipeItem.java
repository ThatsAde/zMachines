package me.petulikan1.zMachines.items;

import com.nexomc.nexo.api.NexoItems;
import lombok.Getter;
import me.petulikan1.zMachines.dataholders.Tier;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class RecipeItem {

    private final double baseRate;
    private final SimpleItem outputMaterial;
    private final List<SimpleItem> inputMaterials;

    // Used by the Crafting Machine — order in the recipe list and lookup key for the detail view.
    private final int order;
    private final String recipeId;

    public RecipeItem(List<SimpleItem> inputMaterials, SimpleItem outputMaterial, double baseRate) {
        this(inputMaterials, outputMaterial, baseRate, 0, "");
    }

    public RecipeItem(List<SimpleItem> inputMaterials, SimpleItem outputMaterial, double baseRate, int order, String recipeId) {
        this.inputMaterials = inputMaterials;
        this.outputMaterial = outputMaterial;
        this.baseRate = baseRate;
        this.order = order;
        this.recipeId = recipeId;
    }

    public long effectiveItemPerTicks(int tier) {
        return (long) (baseRate * 1000 * Tier.timeMultiplier(tier));
    }

    /**
     * Returns true if the sum of items across the given slot map is sufficient to fulfill every input of this recipe.
     * Used by the Crafting Machine: the player may place ingredients across any of the 6 input slots in any order.
     *
     * <p>Matches both vanilla materials (by Material) and Nexo items (by Nexo ID).
     */
    public boolean matchesInputs(Map<Integer, ItemStack> inputMap) {
        // Aggregate available amounts by key (Nexo ID takes precedence; otherwise Material.name()).
        Map<String, Integer> available = new HashMap<>();
        for (ItemStack stack : inputMap.values()) {
            if (stack == null) continue;
            String key = keyFor(stack);
            available.merge(key, stack.getAmount(), Integer::sum);
        }

        for (SimpleItem ingredient : inputMaterials) {
            String key = keyFor(ingredient);
            int have = available.getOrDefault(key, 0);
            if (have < ingredient.getAmount()) return false;
        }
        return true;
    }

    private static String keyFor(ItemStack stack) {
        String nexoId = NexoItems.idFromItem(stack);
        if (nexoId != null) return "nexo:" + nexoId;
        return "vanilla:" + stack.getType().name();
    }

    private static String keyFor(SimpleItem item) {
        if (item.getNexoId() != null) return "nexo:" + item.getNexoId();
        return "vanilla:" + item.getMaterial().name();
    }
}
