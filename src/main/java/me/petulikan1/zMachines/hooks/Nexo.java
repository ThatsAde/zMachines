package me.petulikan1.zMachines.hooks;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent;
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import com.nexomc.nexo.items.ItemBuilder;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.utils.drops.Drop;
import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.messages.Mini;
import me.petulikan1.zMachines.utils.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Nexo implements Listener {

    private static boolean NEXO_LOADED = false;

    public static ItemStack getNexoItem(String id) {
        ItemBuilder itemBuilder = NexoItems.itemFromId(id);
        if (itemBuilder == null) {
            Loader.main.error("Invalid Nexo ID: " + id);
            return new ItemStack(Material.BEDROCK);
        }
        return itemBuilder.build();
    }

    private static final Set<String> ids = new LinkedHashSet<>();

    public static void addItem(String id) {
        ids.add(id); // Set.add is a no-op if already present — no duplicates across reloads
        if (NEXO_LOADED) {
            run();
        }
    }

    private static void run() {
        for (String id : ids) {
            ItemStack item = getNexoItem(id);
            Loader.machineMaterials.add(item.getType());
        }
    }

    /**
     * Re-registers all Nexo block materials into {@code Loader.machineMaterials}.
     * Called by {@code API.loadMaterials()} AFTER the {@code machineMaterials.clear()} so
     * the clear doesn't wipe what a same-tick {@code run()} added (reload bug).
     *
     * Also probes the Nexo API when {@code NEXO_LOADED} is still false — this covers the
     * startup-order case where {@code NexoItemsLoadedEvent} fired during Nexo's onEnable,
     * before zMachines registered its listener (softdepend ordering).
     */
    public static void reloadMaterials() {
        if (ids.isEmpty()) return;
        if (!NEXO_LOADED) {
            for (String id : ids) {
                try {
                    if (NexoItems.itemFromId(id) != null) { NEXO_LOADED = true; break; }
                } catch (Exception ignored) { break; }
            }
        }
        if (NEXO_LOADED) run();
    }

    /**
     * Returns true if the block at the given location is a Nexo custom block.
     * Used by {@link me.petulikan1.zMachines.events.MachineListener} to recognise
     * Nexo-material machines regardless of the underlying vanilla block type.
     */
    public static boolean isCustomBlock(org.bukkit.Location location) {
        return NexoBlocks.isCustomBlock(location.getBlock());
    }

    @EventHandler
    public void onLoad(NexoItemsLoadedEvent e) {
        NEXO_LOADED = true;
        if (!ids.isEmpty()) {
            run();
        }
    }

    /**
     * Block-aligned location of a furniture base entity. Furniture interact/break events expose
     * only the base {@link ItemDisplay} (no {@code getBlock()}), so we derive the machine key the
     * same way for place/interact/break — guaranteeing an identical key for the same static entity.
     */
    private static Location furnitureLoc(ItemDisplay base) {
        return base.getLocation().getBlock().getLocation();
    }

    /**
     * Nexo cancels vanilla {@link org.bukkit.event.block.BlockPlaceEvent} when placing custom blocks,
     * so {@code MachineListener.blockPlaceEvent} (ignoreCancelled=true) never fires for Nexo items.
     * This handler catches the Nexo-specific event and registers the machine instead.
     */
    @EventHandler(ignoreCancelled = true)
    public void nexoPlaceBlock(NexoBlockPlaceEvent e) {
        String nexoId = e.getMechanic().getItemID();
        Pair<MachineType, Integer> entry = API.nexoMachineById.get(nexoId);
        if (entry == null) return;

        final Location loc = e.getBlock().getLocation();
        final Player player = e.getPlayer();
        API.runTaskAsync(() -> {
            try {
                API.createMachine(new Pair<>(entry.getKey(), nexoId), player, loc, entry.getValue());
            } catch (Exception ex) {
                Loader.main.error("Failed to create Nexo machine at " + loc + ": " + ex.getMessage());
                Mini.mm(player, "ErrorPlacing");
            }
        });
    }

    /**
     * Furniture is a separate Nexo mechanic with its own event family ({@code events.furniture.*})
     * that does NOT extend the custom_block events — so the block handlers never fire for it.
     * These three handlers add furniture support (place/interact/break), keying the machine by the
     * base entity's block location via {@link #furnitureLoc(ItemDisplay)}.
     */
    @EventHandler(ignoreCancelled = true)
    public void nexoPlaceFurniture(NexoFurniturePlaceEvent e) {
        String nexoId = e.getMechanic().getItemID();
        Pair<MachineType, Integer> entry = API.nexoMachineById.get(nexoId);
        if (entry == null) return;

        final Location loc = furnitureLoc(e.getBaseEntity());
        final Player player = e.getPlayer();
        API.runTaskAsync(() -> {
            try {
                API.createMachine(new Pair<>(entry.getKey(), nexoId), player, loc, entry.getValue());
            } catch (Exception ex) {
                Loader.main.error("Failed to create Nexo furniture machine at " + loc + ": " + ex.getMessage());
                Mini.mm(player, "ErrorPlacing");
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void nexoFurnitureInteract(NexoFurnitureInteractEvent e) {
        final Location loc = furnitureLoc(e.getBaseEntity());
        Machine machine = API.getMachineByLocation(loc);
        if (machine == null) {
            // Not yet registered — may have been placed by WorldEdit or another plugin.
            // Lazy-create the machine on first player interact.
            String nexoId = e.getMechanic().getItemID();
            Pair<MachineType, Integer> entry = API.nexoMachineById.get(nexoId);
            if (entry == null) return;
            e.setCancelled(true);
            final Player player = e.getPlayer();
            if (!player.isConnected() || player.isSneaking()) return;
            API.runTaskAsync(() -> {
                try {
                    Machine created = API.createMachine(new Pair<>(entry.getKey(), nexoId), player, loc, entry.getValue());
                    if (created != null) {
                        API.runTaskSync(() -> {
                            if (player.isConnected() && !player.isSneaking()) created.openGUI(player);
                        });
                    }
                } catch (Exception ex) {
                    Loader.main.error("Failed to lazy-create furniture machine at " + loc + ": " + ex.getMessage());
                }
            });
            return;
        }
        // Machine already registered — suppress furniture behaviour and open GUI.
        e.setCancelled(true);
        final Player player = e.getPlayer();
        if (!player.isConnected() || player.isSneaking()) return;
        // Guard against double-open if the GUI is already showing for this player.
        if (machine.getGui() != null && machine.getGui().getInventory().getViewers().contains(player)) return;
        machine.openGUI(player);
    }

    @EventHandler
    public void nexoFurnitureBreak(NexoFurnitureBreakEvent e) {
        final Location loc = furnitureLoc(e.getBaseEntity());
        Machine machine = API.getMachineByLocation(loc);
        if (machine == null) {
            // Not registered (WorldEdit-placed, never interacted with).
            // Let Nexo drop the furniture item naturally — no machine data to preserve.
            // The item re-registers as a machine when placed and first clicked again.
            return;
        }

        List<ItemStack> items = new ArrayList<>();
        machine.getInventoryItems().values().forEach(a -> a.values().forEach(b -> {
            if (b != null) items.add(b);
        }));
        items.add(machine.getMachineItem());
        // Clear Nexo's natural furniture drop so we don't drop the raw furniture item alongside ours.
        Drop original = e.getDrop();
        original.getLoots().clear();
        original.getExplosionDrops().getLoots().clear();
        e.setDrop(original);

        final Location dropLoc = e.getBaseEntity().getLocation();
        API.runTaskSync(() -> items.forEach(a -> dropLoc.getWorld().dropItemNaturally(dropLoc, a)));
        API.runTaskAsync(() -> {
            try {
                API.deleteMachine(machine);
            } catch (Exception ee) {
                ee.printStackTrace();
                Loader.main.error("Failed to destroy furniture machine at " + dropLoc + ": " + ee.getMessage());
                if (e.getPlayer() != null) Mini.mm(e.getPlayer(), "ErrorBreaking");
            }
        });
    }

    @EventHandler
    public void nexoDestroyBlock(NexoBlockBreakEvent e){
        Machine machine = API.getMachineByLocation(e.getBlock().getLocation());
        if (machine == null) return;

        HashMap<String,HashMap<Integer,ItemStack>> map = machine.getInventoryItems();
        List<ItemStack> items=new ArrayList<>();
        map.values().forEach(a->a.values().forEach(b-> {
            if(b!=null)
                items.add(b);
        }));
        items.add(machine.getMachineItem());
        Drop original = e.getDrop();
        original.getLoots().clear();
        original.getExplosionDrops().getLoots().clear();
        e.setDrop(original);
        API.runTaskSync(()->{
            items.forEach(a->e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0,1,0),a));
        });
        API.runTaskAsync(()->{
            try {
                API.deleteMachine(machine);
            } catch (Exception ee) {
                ee.printStackTrace();
                Loader.main.error("Failed to destroy machine at location: " + e.getBlock().getLocation() + " | " + e.getPlayer().getName() + " | " + ee.getMessage());
                Mini.mm(e.getPlayer(), "ErrorBreaking");
            }
        });

    }



    @EventHandler(ignoreCancelled = true)
    public void handleMachineInteract(NexoBlockInteractEvent e) {
        final Location loc = e.getBlock().getLocation();
        Machine machine = API.getMachineByLocation(loc);
        if (machine == null) {
            // No machine here yet — the block may have been placed by WorldEdit (a schematic paste
            // fires no place event). If its Nexo ID is a configured machine, lazy-create it on the
            // first right-click, then open the GUI. Only right-click triggers this (left-click is
            // the player starting to break the block — leave that alone).
            if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            String nexoId = e.getMechanic().getItemID();
            Pair<MachineType, Integer> entry = API.nexoMachineById.get(nexoId);
            if (entry == null) return;
            e.setCancelled(true);
            final Player player = e.getPlayer();
            if (!player.isConnected() || player.isSneaking()) return;
            API.runTaskAsync(() -> {
                try {
                    Machine created = API.createMachine(new Pair<>(entry.getKey(), nexoId), player, loc, entry.getValue());
                    if (created != null) {
                        API.runTaskSync(() -> {
                            if (player.isConnected() && !player.isSneaking()) created.openGUI(player);
                        });
                    }
                } catch (Exception ex) {
                    Loader.main.error("Failed to lazy-create machine at " + loc + ": " + ex.getMessage());
                }
            });
            return;
        }
        // Registered machine: cancel the Nexo event to suppress default custom-block behaviour
        // (note sounds, etc.) on any interaction.
        e.setCancelled(true);
        // Open the GUI directly here: Nexo may set useInteractedBlock=DENY on the underlying
        // PlayerInteractEvent, which would cause MachineListener.onInteract to exit early.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Player player = e.getPlayer();
            if (player.isConnected() && !player.isSneaking()) {
                machine.openGUI(player);
            }
        }
    }


}
