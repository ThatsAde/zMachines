package me.petulikan1.zMachines.events;

import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.config.MenuConfig;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.menu.items.ItemGrid;
import me.petulikan1.zMachines.menu.items.MenuAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Furnace-style hopper automation for every machine type. zMachines blocks are not vanilla
 * {@link org.bukkit.inventory.InventoryHolder}s, so vanilla hopper transfer never fires on them —
 * this task replicates it manually:
 *
 * <ul>
 *   <li><b>Hopper directly above, facing down</b> → inserts items into the machine, routed by type:
 *       valid fuel → fuel slot, valid recipe input → input slot, anything else is left alone.</li>
 *   <li><b>Hopper directly below</b> → extracts the machine's output slot(s) into the hopper.</li>
 * </ul>
 *
 * Runs on the main thread every 8 ticks (vanilla hopper cadence), moving at most one item per hopper
 * per direction per cycle. Works whether or not a player has the machine GUI open: when open we
 * read/write the live inventory (persisted on close by {@code tickStop}); when closed we mutate the
 * persisted {@code inventoryItems} map and flush via {@link API#updateMachine(Machine)} once per cycle.
 */
public class HopperTransferTask implements Runnable {

    private static final String INPUT = MenuAction.INPUT_SLOT.name();
    private static final String FUEL = MenuAction.FUEL_SLOT.name();
    private static final String OUTPUT = MenuAction.OUTPUT_SLOT.name();

    private static final BlockFace[] SIDES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    @Override
    public void run() {
        if (!Loader.cfg.getBoolean("Hoppers", true)) return;

        Set<Machine> dirtyClosed = new HashSet<>();

        for (Machine m : Loader.machines.values()) {
            try {
                if (m.hopperBusy()) continue;

                Location loc = m.getLocation().toBukkit();
                if (loc == null) continue;
                World w = loc.getWorld();
                if (w == null || !w.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;

                ItemGrid grid = getGrid(m);
                if (grid == null) continue;

                Block base = loc.getBlock();

                // Top hopper (facing down) → insert
                Block above = base.getRelative(BlockFace.UP);
                if (above.getType() == Material.HOPPER && hopperFaces(above, BlockFace.DOWN)) {
                    BlockState st = above.getState();
                    if (st instanceof org.bukkit.block.Hopper hopper) {
                        tryInsert(m, grid, hopper.getInventory(), dirtyClosed);
                    }
                }

                // Side hoppers pointing at the machine → insert
                for (BlockFace face : SIDES) {
                    Block side = base.getRelative(face);
                    if (side.getType() == Material.HOPPER && hopperFaces(side, face.getOppositeFace())) {
                        BlockState st = side.getState();
                        if (st instanceof org.bukkit.block.Hopper hopper) {
                            tryInsert(m, grid, hopper.getInventory(), dirtyClosed);
                        }
                    }
                }

                // Bottom hopper → extract
                Block below = base.getRelative(BlockFace.DOWN);
                if (below.getType() == Material.HOPPER) {
                    BlockState st = below.getState();
                    if (st instanceof org.bukkit.block.Hopper hopper) {
                        tryExtract(m, grid, hopper.getInventory(), dirtyClosed);
                    }
                }
            } catch (Exception e) {
                Loader.main.error("Hopper task error at machine " + m.getId() + ": " + e.getMessage());
            }
        }

        for (Machine m : dirtyClosed) {
            API.runTaskAsync(() -> {
                try {
                    API.updateMachine(m);
                } catch (Exception e) {
                    Loader.main.error("Hopper persist failed for machine " + m.getId() + ": " + e.getMessage());
                }
            });
        }
    }

    // ---------------------------------------------------------------------
    // Transfer logic
    // ---------------------------------------------------------------------

    private void tryInsert(Machine m, ItemGrid grid, Inventory hopperInv, Set<Machine> dirtyClosed) {
        // One fuel + one input per cycle, scanned independently, so fuel is never starved by
        // inputs sharing the same hopper (e.g. the Growing Machine never consumes inputs → seeds
        // would top up the input slots forever and the coal in a later hopper slot never gets a turn).
        insertCategory(m, hopperInv, grid.getFuelSlots(), dirtyClosed, FUEL);
        insertCategory(m, hopperInv, grid.getInputSlots(), dirtyClosed, INPUT);
    }

    private void insertCategory(Machine m, Inventory hopperInv, List<Integer> slots,
                                Set<Machine> dirtyClosed, String type) {
        if (slots.isEmpty()) return;
        for (int i = 0; i < hopperInv.getSize(); i++) {
            ItemStack src = hopperInv.getItem(i);
            if (src == null || src.getType().isAir()) continue;

            boolean matches = type.equals(FUEL)
                    ? Loader.getFuelItem(m, src) != null
                    : (Loader.getFuelItem(m, src) == null && m.acceptsInput(src));
            if (!matches) continue;

            if (placeOne(m, type, slots, src, dirtyClosed)) {
                ItemStack newSrc = src.clone();
                newSrc.setAmount(newSrc.getAmount() - 1);
                hopperInv.setItem(i, newSrc.getAmount() <= 0 ? null : newSrc);
                return; // one of this category per cycle
            }
        }
    }

    private void tryExtract(Machine m, ItemGrid grid, Inventory hopperInv, Set<Machine> dirtyClosed) {
        for (int slot : grid.getOutputSlots()) {
            ItemStack out = readSlot(m, OUTPUT, slot);
            if (out == null || out.getType().isAir()) continue;

            ItemStack one = out.clone();
            one.setAmount(1);
            HashMap<Integer, ItemStack> leftover = hopperInv.addItem(one);
            if (!leftover.isEmpty()) continue; // hopper had no room for this item — try next output slot

            ItemStack newOut = out.clone();
            newOut.setAmount(newOut.getAmount() - 1);
            writeSlot(m, OUTPUT, slot, newOut.getAmount() <= 0 ? null : newOut, dirtyClosed);
            return; // one transfer per cycle
        }
    }

    /** Places one of {@code src} into the first stackable-or-empty target slot. Returns true if placed. */
    private boolean placeOne(Machine m, String type, List<Integer> slots, ItemStack src, Set<Machine> dirtyClosed) {
        // 1) stack onto an existing matching slot with room
        for (int slot : slots) {
            ItemStack cur = readSlot(m, type, slot);
            if (cur == null || cur.getType().isAir()) continue;
            if (cur.isSimilar(src) && cur.getAmount() < cur.getMaxStackSize()) {
                ItemStack copy = cur.clone();
                copy.setAmount(copy.getAmount() + 1);
                writeSlot(m, type, slot, copy, dirtyClosed);
                return true;
            }
        }
        // 2) first empty slot
        for (int slot : slots) {
            ItemStack cur = readSlot(m, type, slot);
            if (cur == null || cur.getType().isAir()) {
                ItemStack one = src.clone();
                one.setAmount(1);
                writeSlot(m, type, slot, one, dirtyClosed);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Open/closed slot abstraction
    // ---------------------------------------------------------------------

    private ItemStack readSlot(Machine m, String type, int slot) {
        if (m.getGui() != null) {
            return m.getGui().getInventory().getItem(slot);
        }
        HashMap<Integer, ItemStack> map = m.getInventoryItems().get(type);
        return map == null ? null : map.get(slot);
    }

    private void writeSlot(Machine m, String type, int slot, ItemStack s, Set<Machine> dirtyClosed) {
        if (m.getGui() != null) {
            // Live inventory is source of truth while open; tickStop persists it on close.
            m.getGui().getInventory().setItem(slot, s);
        } else {
            m.getInventoryItems().computeIfAbsent(type, k -> new HashMap<>()).put(slot, s);
            dirtyClosed.add(m);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ItemGrid getGrid(Machine m) {
        MenuConfig cfg = MenuConfig.init(m.getIdentifier(), m.getTier());
        return cfg == null ? null : cfg.getItemGrid(m.getTier());
    }

    private boolean hopperFaces(Block block, BlockFace face) {
        return block.getBlockData() instanceof org.bukkit.block.data.type.Hopper h
                && h.getFacing() == face;
    }
}
