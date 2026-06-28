package me.petulikan1.zMachines.events;
import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.dataholders.Tier;
import me.petulikan1.zMachines.hooks.Nexo;
import me.petulikan1.zMachines.messages.Mini;
import me.petulikan1.zMachines.utils.PDC;
import me.petulikan1.zMachines.utils.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MachineListener implements Listener {


    @EventHandler(ignoreCancelled = true)
    public void blockPlaceEvent(BlockPlaceEvent e) {
        if (!e.canBuild()) return;

        Pair<MachineType, String> mt = getMachineType(e.getItemInHand());
        if (mt == null) return;

        int storedTier = getMachineTier(e.getItemInHand());
        if (storedTier == -1) return;
        // Tier migration: pre-2.0 items used 0-indexed tiers (0/1/2). Bump +1 then clamp.
        if (isLegacyItem(e.getItemInHand())) storedTier += 1;
        final int tier = Tier.clamp(storedTier);

        API.runTaskAsync(() -> {
            try {
                API.createMachine(mt, e.getPlayer(), e.getBlock().getLocation(), tier);
            } catch (Exception ee) {
                ee.printStackTrace();
                Loader.main.error("Failed to create a new machine at location: " + e.getBlock().getLocation() + " | " + e.getPlayer().getName() + " | " + ee.getMessage());
                Mini.mm(e.getPlayer(), "ErrorPlacing");
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        if (!Loader.machineMaterials.contains(e.getBlock().getType())) return;

        Machine machine = API.getMachineByLocation(e.getBlock().getLocation());
        if (machine == null) return;

        HashMap<String, HashMap<Integer, ItemStack>> map = machine.getInventoryItems();
        List<ItemStack> items = new ArrayList<>();
        map.values().forEach(a -> a.values().forEach(b -> {
            if (b != null)
                items.add(b);
        }));
        if (machine.getNexoItemID() == null) {
            // Block.getDrops().clear() is a no-op (getDrops() returns a fresh collection each call),
            // which let the vanilla block drop alongside the manually-dropped machine item → duplication.
            // setDropItems(false) is the real API that suppresses the vanilla drop.
            e.setDropItems(false);
            items.add(machine.getMachineItem());
        }
        Location loc = e.getBlock().getLocation().add(0, 1, 0);
        if (loc.getBlock().getType() != Material.AIR) {
            loc = loc.subtract(0, 1, 0);
        }
        Location finalLoc = loc;
        items.forEach(a -> e.getBlock().getWorld().dropItemNaturally(finalLoc, a));

        API.runTaskAsync(() -> {
            try {
                API.deleteMachine(machine);
            } catch (Exception ee) {
                ee.printStackTrace();
                Loader.main.error("Failed to destroy machine at location: " + e.getBlock().getLocation() + " | " + e.getPlayer().getName() + " | " + ee.getMessage());
                Mini.mm(e.getPlayer(), "ErrorBreaking");
            }
        });
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.useInteractedBlock() == Event.Result.DENY) return;
        if (e.useItemInHand() == Event.Result.DENY) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        if (e.getClickedBlock() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Primary check: vanilla material registered in machineMaterials.
        // Fallback: Nexo custom blocks — their placed-block type may differ from the item type
        // added to machineMaterials, so we detect them via the NexoBlocks API instead.
        if (!Loader.machineMaterials.contains(e.getClickedBlock().getType())) {
            if (!Loader.isNexoSupport || !Nexo.isCustomBlock(e.getClickedBlock().getLocation())) return;
        }

        final Player player = e.getPlayer();
        if (!player.isConnected() || player.isSneaking()) return;

        Machine machine = API.getMachineByLocation(e.getClickedBlock().getLocation());
        if (machine == null) return;

        e.setCancelled(true);

        // Guard against double-open: for Nexo machines, NexoBlockInteractEvent may have already
        // opened the GUI on the same tick. Reopening would trigger tickStop → tickStart again.
        if (machine.getGui() != null && machine.getGui().getInventory().getViewers().contains(player)) return;

        machine.openGUI(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent e) {
        // Machine blocks like FURNACE/COMPOSTER are real vanilla containers; stop vanilla hoppers from
        // moving items in/out of them so our HopperTransferTask is the sole mover of machine I/O.
        if (isMachineBlockInventory(e.getDestination()) || isMachineBlockInventory(e.getSource())) {
            e.setCancelled(true);
        }
    }

    private boolean isMachineBlockInventory(Inventory inv) {
        Location loc = inv.getLocation();
        return loc != null && API.getMachineByLocation(loc) != null;
    }

    private Pair<MachineType, String> getMachineType(ItemStack item) {
        if (!item.hasItemMeta())
            return null;
        PDC pdc = new PDC(item.getItemMeta());
        if (!pdc.hasBoolean("machine_block"))
            return null;
        String machineID = pdc.getString("machine_id");
        if (machineID == null)
            return null;
        String nexoID = pdc.getString("nexoid");
        return new Pair<>(MachineType.getMachineType(machineID), nexoID);
    }

    private int getMachineTier(ItemStack item) {
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return -1;

        return Optional.ofNullable(itemMeta.getPersistentDataContainer().get(new NamespacedKey(Loader.main, "machine_tier"), PersistentDataType.INTEGER))
                .orElse(-1);
    }

    private boolean isLegacyItem(ItemStack item) {
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return false;
        Double version = itemMeta.getPersistentDataContainer().get(new NamespacedKey(Loader.main, "item_version"), PersistentDataType.DOUBLE);
        return version != null && version < 2.0;
    }
}
