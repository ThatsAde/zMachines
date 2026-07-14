package me.petulikan1.zMachines.dataholders;

import lombok.Getter;
import lombok.Setter;
import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.hooks.Nexo;
import me.petulikan1.zMachines.items.BasicItem;
import me.petulikan1.zMachines.items.FuelItem;
import me.petulikan1.zMachines.items.RecipeItem;
import me.petulikan1.zMachines.items.SimpleItem;
import me.petulikan1.zMachines.menu.GUI;
import me.petulikan1.zMachines.menu.ItemGUI;
import me.petulikan1.zMachines.menu.items.ItemGrid;
import me.petulikan1.zMachines.menu.items.MenuAction;
import me.petulikan1.zMachines.messages.Mini;
import me.petulikan1.zMachines.messages.MiniImpl;
import me.petulikan1.zMachines.utils.PDC;
import me.petulikan1.zMachines.utils.Triplet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
public abstract class Machine extends InventoryUtils {

    // Upper bound on the offline catch-up simulation (~14h of ticks). Output/input capacity caps real
    // production far sooner, so this never nerfs legitimate offline gains — it only guards against an
    // unbounded loop when lastViewed is stale/zero (which froze the server).
    private static final long MAX_CATCHUP_TICKS = 1_000_000L;

    public final LocationInternal location;
    private final int tier;
    public final int id;
    public final UUID owner;
    public final MachineType machineType;

    public final String identifier;
    public ItemStack machineItem;

    public HashMap<String, HashMap<Integer, ItemStack>> inventoryItems = new HashMap<>();
    public String nexoItemID;

    public final AtomicReference<Long> startTime;
    public final AtomicReference<Long> lastUpdated;
    public MachineState machineState;
    public MachineInternalData machineInternalData;

    private GUI gui;


    public Machine(LocationInternal location, int id, int tier, UUID owner, MachineType machineType, String identifier) {
        this.location = location;
        this.id = id;
        this.tier = tier;
        this.owner = owner;
        this.machineType = machineType;
        this.identifier = identifier;
        startTime = new AtomicReference<>();
        lastUpdated = new AtomicReference<>();
        lastUpdated.set(0L);
    }

    public void openGUI(Player player) {
        if (gui == null) {
            gui = GUI.create(identifier, player, this);
        }
        if (gui == null)
            return;
        gui.open(player);
    }

    public void tickStart(GUI gui, ItemGrid itemGrid) {
        update(gui, itemGrid);
    }

    protected boolean consumesInputs() { return true; }

    protected boolean processAllRecipesPerTick() { return false; }

    /**
     * True if the given item is a valid recipe input for this machine type (amount-agnostic).
     * Used by the hopper-automation task to route items inserted from a hopper above the machine.
     * Default false; overridden per machine type.
     */
    public boolean acceptsInput(ItemStack item) { return false; }

    /**
     * True when the live GUI is in a transient state that is unsafe for hopper I/O (e.g. the
     * Crafting Machine's detail view, where input/fuel slots are blanked and the output holds a
     * preview). The hopper task skips the machine for that cycle when this returns true.
     */
    public boolean hopperBusy() { return false; }

    protected void onRecipeProgress(GUI gui, ItemGrid itemGrid,
                                    Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>> recipe,
                                    int progress) {}

    public void onTick(GUI gui, ItemGrid itemGrid) {
        if (isEmptyUpdate(gui, itemGrid, this)) {
            updateStatusItem(gui, itemGrid);
            return;
        }

        HashMap<Integer, ItemStack> inputMap = getItems(gui, itemGrid.getInputSlots());
        HashMap<Integer, ItemStack> fuelMap = getItems(gui, itemGrid.getFuelSlots());

        List<Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>>> recipeData = getAllRecipeData(inputMap, itemGrid, this);
        List<Triplet<FuelItem, ItemStack, Integer>> fuelData = getAllFuelData(fuelMap, this);

        if (recipeData.isEmpty() || fuelData.isEmpty()) {
            machineInternalData.setCurrentTick(0);
            updateStatusItem(gui, itemGrid);
            return;
        }

        setMachineState(MachineState.RUNNING);
        updateStatusItem(gui, itemGrid);

        HashSet<Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>>> fullOutputRecipes = new HashSet<>();
        long currentTick = machineInternalData.getCurrentTick();

        for (Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>> recipe : recipeData) {
            long recipeTick = recipe.getA().effectiveItemPerTicks(this.getTier()) / 50;
            if (recipeTick <= 0) continue;

            int progress = (int) Math.ceil(((double) (currentTick % recipeTick) / recipeTick) * 100);
            onRecipeProgress(gui, itemGrid, recipe, progress);

            SimpleItem inputMaterial = recipe.getA().getInputMaterials().getFirst();
            SimpleItem outputMaterial = recipe.getA().getOutputMaterial();

            int slot = findOutputSlot(gui, itemGrid, outputMaterial);
            if (slot == -1) {
                fullOutputRecipes.add(recipe);
                continue;
            }

            if (currentTick % recipeTick == 0 && currentTick != 0) {
                addItems(gui, outputMaterial, slot);
                if (consumesInputs()) {
                    removeInputItems(this, gui, inputMaterial, recipe.getC());
                }
                if (!processAllRecipesPerTick()) break;
            }
        }

        if (fullOutputRecipes.containsAll(recipeData)) {
            setMachineState(MachineState.FULL);
            updateStatusItem(gui, itemGrid);
            return;
        }

        for (Triplet<FuelItem, ItemStack, Integer> fuel : fuelData) {
            long fuelTick = fuel.getA().effectiveItemPerTicks(this.getTier()) / 50;
            if (fuelTick <= 0) continue;
            if (currentTick % fuelTick == 0 && currentTick != 0) {
                removeFuelItems(gui, fuel.getA(), fuel.getC());
                break;
            }
        }

        machineInternalData.incrementCurrentTick();
    }

    private int findOutputSlot(GUI gui, ItemGrid itemGrid, SimpleItem outputMaterial) {
        // Iterate in ascending slot order so output fills the lowest-numbered slot first.
        // The old code went through a HashMap (undefined order), producing non-deterministic fill.
        List<Integer> slots = new ArrayList<>(itemGrid.getOutputSlots());
        Collections.sort(slots);
        for (int slot : slots) {
            ItemStack value = gui.getItem(slot);
            if (value == null) return slot;
            if (value.isSimilar(outputMaterial.getStackCached())
                    && (value.getAmount() + outputMaterial.getAmount()) <= value.getMaxStackSize()) {
                return slot;
            }
        }
        return -1;
    }

    public void tickStop(GUI gui, ItemGrid itemGrid) {
        handleSlotData(MenuAction.FUEL_SLOT, itemGrid.getFuelSlots(), gui);
        handleSlotData(MenuAction.OUTPUT_SLOT, itemGrid.getOutputSlots(), gui);
        handleSlotData(MenuAction.INPUT_SLOT, itemGrid.getInputSlots(), gui);

        machineInternalData.setLastViewed(System.currentTimeMillis());

        this.gui = null;
        Runnable r = () -> {
            try {
                API.updateMachine(this);
            } catch (Exception ee) {
                ee.printStackTrace();
                Loader.main.error("Failed to update machine data at location: " + getLocation().toBukkit().toString() + " | " + ee.getMessage());
            }
        };
        if (!Loader.main.isEnabled()) {
            API.runTaskSync(r);
        } else {
            API.runTaskAsync(r);
        }
    }


    public void onClose(Player player, GUI gui, ItemGrid itemGrid) {
        ItemStack item = player.getOpenInventory().getCursor().clone();
        if (!item.getType().isAir()) {
            player.getOpenInventory().setCursor(null);
            HashMap<Integer, ItemStack> map = player.getInventory().addItem(item);
            if (!map.isEmpty()) {
                API.runTaskSync(() -> {
                    for (Map.Entry<Integer, ItemStack> entry : map.entrySet()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
                    }
                });
                Mini.mm(player, "ItemsDroppedOnGround");
            }
        }
    }

    public void update(GUI gui, ItemGrid itemGrid) {
        if (isEmptyUpdate(gui, itemGrid, this)) {
            return;
        }

        long lastViewed = machineInternalData.getLastViewed();
        // Never opened (lastViewed == 0 from MachineInternalData.defaultData()) → no meaningful elapsed
        // runtime. Without this guard, machineLeftFor ≈ epoch millis → ~35 billion ticks → server freeze.
        if (lastViewed <= 0) {
            return;
        }
        long machineLeftFor = System.currentTimeMillis() - lastViewed;
        long ticksPassed = machineLeftFor / 50;
        if (ticksPassed <= 0) {
            return;
        }
        ticksPassed = Math.min(ticksPassed, MAX_CATCHUP_TICKS); // bound the simulation

        HashMap<Integer, ItemStack> inputMap = getItems(gui, itemGrid.getInputSlots());
        HashMap<Integer, ItemStack> fuelMap = getItems(gui, itemGrid.getFuelSlots());

        List<Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>>> recipeData = getAllRecipeData(inputMap, itemGrid, this);
        List<Triplet<FuelItem, ItemStack, Integer>> fuelData = getAllFuelData(fuelMap, this);

        if (recipeData.isEmpty() || fuelData.isEmpty()) {
            return;
        }

        int maxRunTicks = 0;
        for (int tick = 0; tick <= ticksPassed; tick++) {
            if (recipeData.isEmpty() || fuelData.isEmpty()) break;

            for (Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>> recipe : recipeData) {
                long recipeTick = recipe.getA().effectiveItemPerTicks(this.getTier()) / 50;
                if (recipeTick <= 0) continue;

                // No GUI repaint during headless catch-up — onRecipeProgress → setItemMeta is expensive
                // and pointless here (no viewer sees intermediate progress; live onTick repaints on open).

                SimpleItem inputMaterial = recipe.getA().getInputMaterials().getFirst();
                SimpleItem outputMaterial = recipe.getA().getOutputMaterial();

                int slot = findOutputSlot(gui, itemGrid, outputMaterial);
                if (slot == -1) continue;

                if (tick % recipeTick == 0 && tick != 0) {
                    addItems(gui, outputMaterial, slot);
                    if (consumesInputs()) {
                        removeInputItems(this, gui, inputMaterial, recipe.getC());
                        recipeData = getAllRecipeData(getItems(gui, itemGrid.getInputSlots()), itemGrid, this);
                    }
                    maxRunTicks = tick;
                    if (!processAllRecipesPerTick()) break;
                }
            }
        }

        for (int tick = 0; tick <= maxRunTicks; tick++) {
            for (Triplet<FuelItem, ItemStack, Integer> fuel : fuelData) {
                long fuelTick = fuel.getA().effectiveItemPerTicks(this.getTier()) / 50;
                if (fuelTick <= 0) continue;
                if (tick % fuelTick == 0 && tick != 0) {
                    removeFuelItems(gui, fuel.getA(), fuel.getC());
                    fuelData = getAllFuelData(getItems(gui, itemGrid.getFuelSlots()), this);
                    break;
                }
            }
        }
    }


    private void handleSlotData(MenuAction menuAction, List<Integer> slots, GUI gui) {
        for (int i : slots) {
            inventoryItems.computeIfAbsent(menuAction.name(), k -> new HashMap<>()).put(i, gui.getItem(i));
        }
    }

    public static HashMap<MachineState,ItemStack> itemCache = new HashMap<>();
    protected void updateStatusItem(GUI gui, ItemGrid itemGrid) {
        for (int i : itemGrid.getStatusSlots()) {
            ItemGUI itemGUI = gui.getItemGUI(i);
            if (itemGUI == null) continue;

            ItemStack item = itemCache.get(getMachineState());
            if (item == null) {
                item = itemGrid.getStatusItems().get(this.machineState).build(null, this.getNameTagResolver());
                itemCache.put(this.machineState, item);
            }

            if (item == null) continue;

            gui.setItem(i, itemGUI.setItem(item));
        }
    }

    public static HashMap<ProgressLevel, ItemStack> progressItemCache = new HashMap<>();
    protected void updateProgressItem(GUI gui, ItemGrid itemGrid, int i, int progress) {
        int slot = itemGrid.getProgressSlots().get(i);
        ItemGUI itemGUI = gui.getItemGUI(slot);
        if(itemGUI == null) return;
        ProgressLevel progressLevel = getProgressLevel(progress);
        ItemStack item = progressItemCache.computeIfAbsent(progressLevel, x -> itemGrid.getProgressItems().get(progressLevel).build(null,getNameTagResolver())).clone();
        if(item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            ItemMeta meta = item.getItemMeta();
            // Use the raw configured name from the meta — NOT ItemStack#displayName(), which returns the
            // chat representation wrapped in square brackets ([Progress: 50%]) and was baking those
            // brackets into the wool's name.
            meta.displayName(meta.displayName()
                    .replaceText(x ->
                            x.matchLiteral("%progress%")
                                    .replacement(String.valueOf(progress))));
            item.setItemMeta(meta);
        }
        gui.setItem(slot, itemGUI.setItem(item));
    }

    private static @NotNull ProgressLevel getProgressLevel(double progress) {
        ProgressLevel progressLevel = ProgressLevel.STOPPED;
        if(progress < 25) {
            progressLevel = ProgressLevel.FIRST_QUARTILE;
        } else if(progress < 50) {
            progressLevel = ProgressLevel.SECOND_QUARTILE;
        } else if(progress < 75) {
            progressLevel = ProgressLevel.THIRD_QUARTILE;
        } else if(progress < 100) {
            progressLevel = ProgressLevel.FOURTH_QUARTILE;
        }
        return progressLevel;
    }

    public boolean onInteract(Player player, GUI gui, ClickType type, int slot, boolean upInventory, InventoryClickEvent e, ItemGrid itemGrid) {
        if (slot == -999) return false;
        if (type.isCreativeAction() || type.isKeyboardClick()) return false;

        final ItemStack currentItem = e.getCurrentItem();
        if (upInventory) {
            this.machineInternalData.setCurrentTick(0);

            if (itemGrid.getOutputSlots().contains(e.getSlot())) {
                return currentItem != null && e.getCursor().getType() == Material.AIR;
            }

            return itemGrid.getInputSlots().contains(e.getSlot()) || itemGrid.getFuelSlots().contains(e.getSlot());
        }

        if (currentItem == null) return !e.getCursor().getType().isAir();

        if (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.LEFT) return true;
        if (!e.getClick().isShiftClick()) return false;

        //if (e.getClick() != ClickType.SHIFT_LEFT && e.getClick() != ClickType.SHIFT_RIGHT) return false;

        List<Integer> list = new ArrayList<>(itemGrid.getFuelSlots());
        list.addAll(itemGrid.getInputSlots());
        list.sort(Comparator.comparingInt(a -> a));

        handleShiftLeft(gui, e, currentItem.clone(), list);
        this.machineInternalData.setCurrentTick(0);

        return false;
    }

    public Component getName() {
        return MiniImpl.getTrC(Loader.cfg, identifier, "Name");
    }

    public TagResolver getNameTagResolver() {
        return Placeholder.component("machine", this::getName);
    }

    public ItemStack getMachineItem() {
        if (machineItem == null) {
            BasicItem basicItem = new BasicItem(Loader.cfg, identifier + ".ItemStack");
            basicItem.setNbtConsumer(pdc -> {
                pdc.setBoolean("machine_block", true);
                pdc.setDouble("item_version", basicItem.getItemVersion());
                pdc.setString("machine_id", machineType.name());
                pdc.setInt("machine_tier", this.tier);
            });
            String mat = API.getMaterialRaw(identifier, this.tier);
            boolean isNexoItem = mat.toLowerCase().startsWith("nexo:");
            Material m = Material.getMaterial(mat.toUpperCase());
            if (m == null && isNexoItem && Loader.isNexoSupport) {
                String ID = mat.substring(5);
                this.nexoItemID = ID;
                ItemStack item = Nexo.getNexoItem(ID);
                ItemMeta meta = item.getItemMeta();
                PDC pdc = new PDC(meta);
                pdc.setBoolean("machine_block", true);
                pdc.setDouble("item_version", basicItem.getItemVersion());
                pdc.setString("machine_id", machineType.name());
                pdc.setString("nexoid", ID);
                pdc.setInt("machine_tier", this.tier);
                item.setItemMeta(meta);
                return machineItem = item;
            }
            if (m == null) {
                // No vanilla fallback by design — the configured block for this tier didn't resolve
                // (Nexo support off, or the id/material was changed after this machine was created).
                // Use BEDROCK as a visible sentinel (same as Nexo.getNexoItem's invalid-id marker) so
                // the break drop isn't lost and the misconfig is obvious, rather than NPEing on build.
                Loader.main.error("Could not resolve block material '" + mat + "' for " + identifier
                        + " tier " + this.tier + " — check the Nexo id / material in config.");
                m = Material.BEDROCK;
            }
            machineItem = basicItem.build(m, getNameTagResolver());
        }
        return machineItem;
    }


}
