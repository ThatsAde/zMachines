package me.petulikan1.zMachines.items;

import lombok.Getter;
import me.petulikan1.zMachines.dataholders.Tier;
import org.bukkit.Material;

@Getter
public class FuelItem extends SimpleItem {

    private final double baseRate;

    public FuelItem(Material material, double baseRate) {
        super(material, 1);
        this.baseRate = baseRate;
    }

    public long effectiveItemPerTicks(int tier) {
        return (long) (baseRate * 1000 * Tier.timeMultiplier(tier));
    }
}
