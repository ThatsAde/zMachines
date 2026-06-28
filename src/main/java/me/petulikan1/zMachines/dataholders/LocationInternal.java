package me.petulikan1.zMachines.dataholders;

import lombok.Data;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

@Data
@ToString(exclude = {"location"})
public class LocationInternal {
    private final double x, y, z;
    private final String worldName;
    @Nullable
    private transient WeakReference<Location> location;
    private String nexoID;


    public LocationInternal(Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.worldName = location.getWorld().getName();
        this.location = new WeakReference<>(location);
    }

    public Location toBukkit() {
        if (worldName == null)
            return null;
        // The cache is a WeakReference, so location.get() can return null after GC even though the
        // field itself is non-null. The old code returned that null and never rebuilt — which made
        // HopperTransferTask (the main per-tick caller) skip every machine forever. Rebuild on demand.
        Location cached = (location != null) ? location.get() : null;
        if (cached != null)
            return cached;
        World world = Bukkit.getWorld(worldName);
        if (world == null)
            return null;
        Location loc = new Location(world, x, y, z);
        location = new WeakReference<>(loc);
        return loc;
    }


}
