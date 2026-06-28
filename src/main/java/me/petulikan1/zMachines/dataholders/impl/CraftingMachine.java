package me.petulikan1.zMachines.dataholders.impl;

import lombok.Getter;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.dataholders.LocationInternal;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineState;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.items.RecipeItem;
import me.petulikan1.zMachines.items.SimpleItem;
import me.petulikan1.zMachines.menu.GUI;
import me.petulikan1.zMachines.menu.HolderGUI;
import me.petulikan1.zMachines.menu.ItemGUI;
import me.petulikan1.zMachines.menu.items.ItemGrid;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import me.petulikan1.zMachines.utils.Triplet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recipe-based machine. Unlike {@link RubbleProcessor} and {@link GrowingMachine}, the Crafting
 * Machine does not auto-process when items are placed — the player must explicitly press the
 * confirm (emerald) button to start crafting. This prevents the wrong recipe from running by accident.
 *
 * <p>The GUI has two views:
 * <ul>
 *   <li>List view: paginated browse of all configured recipes; player places items + fuel and presses confirm</li>
 *   <li>Detail view: clicked when the player taps a recipe in the list — shows that recipe's required inputs
 *       and output as a read-only reference, with a paper "back" button to return</li>
 * </ul>
 * Both views render in-place in the same Bukkit inventory (no close/reopen).
 *
 * <p>View state is machine-wide (shared across viewers), matching how the existing GUI framework
 * already shares one {@code Inventory} instance across all viewers.
 */
@Getter
public class CraftingMachine extends Machine {

    public static final int RECIPES_PER_PAGE = 16;

    // View state — machine-wide
    private int currentPage = 0;
    private String detailViewRecipeId = null;
    private final Map<Integer, ItemStack> stashedItems = new HashMap<>();

    // The recipe currently being crafted; null = idle. Transient (not persisted) — server restart resets to idle.
    private String activeRecipeId = null;

    public CraftingMachine(LocationInternal location, int id, UUID owner, int tier) {
        super(location, id, tier, owner, MachineType.CRAFTING_MACHINE, MachineType.CRAFTING_MACHINE.getIdentifier());
    }

    // ---------------------------------------------------------------------
    // Machine template-method overrides
    // ---------------------------------------------------------------------

    @Override
    public boolean acceptsInput(ItemStack item) {
        // Valid input = matches any ingredient of any configured Crafting Machine recipe (amount-agnostic).
        if (item == null) return false;
        for (RecipeItem ri : Loader.recipeItems.getOrDefault(MachineType.CRAFTING_MACHINE, Collections.emptyList())) {
            for (SimpleItem ingredient : ri.getInputMaterials()) {
                if (ingredient.isSameKind(item)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean hopperBusy() {
        // While a viewer is in detail view, input/fuel slots are stashed and the output holds a preview —
        // unsafe for hopper I/O against the live inventory.
        return isInDetailView();
    }

    @Override
    protected boolean consumesInputs() {
        return true;
    }

    @Override
    protected boolean processAllRecipesPerTick() {
        return false;
    }

    @Override
    protected void onRecipeProgress(GUI gui, ItemGrid itemGrid,
                                    Triplet<RecipeItem, SimpleItem, Map<Integer, ItemStack>> recipe,
                                    int progress) {
        // Single progress wool — update slot index 0
        if (!itemGrid.getProgressSlots().isEmpty()) {
            updateProgressItem(gui, itemGrid, 0, progress);
        }
    }

    @Override
    public void tickStart(GUI gui, ItemGrid itemGrid) {
        // Always start in list view, page 0
        currentPage = 0;
        detailViewRecipeId = null;
        stashedItems.clear();
        super.tickStart(gui, itemGrid);
        renderListView(gui, itemGrid);
    }

    @Override
    public void tickStop(GUI gui, ItemGrid itemGrid) {
        // If the player closes the menu while in detail view, the inventory is in a bogus state:
        // input + fuel slots are blanked (real items live in the transient stashedItems map, which
        // does NOT survive instance reload), and the output slot holds the preview. tickStop is
        // about to persist whatever is in those slots — so we MUST repaint the list view first to
        // restore the real items and clear the preview. Otherwise: items lost + preview becomes a
        // free item next open.
        if (isInDetailView()) {
            renderListView(gui, itemGrid);
        }
        super.tickStop(gui, itemGrid);
    }

    @Override
    public void onTick(GUI gui, ItemGrid itemGrid) {
        // Pause tick logic while a viewer is in detail mode (input slots are stashed → tick would misread state)
        if (isInDetailView()) {
            return;
        }
        super.onTick(gui, itemGrid);

        // If we were crafting but the matching recipe can no longer be satisfied, return to idle so the
        // player can press confirm again (e.g. to start a different recipe).
        if (activeRecipeId != null) {
            RecipeItem recipe = lookupRecipe(activeRecipeId);
            if (recipe == null || !recipe.matchesInputs(getInputSnapshot(gui, itemGrid))) {
                activeRecipeId = null;
            }
        }
    }

    // ---------------------------------------------------------------------
    // View management
    // ---------------------------------------------------------------------

    public boolean isInDetailView() {
        return detailViewRecipeId != null;
    }

    public List<RecipeItem> sortedRecipes() {
        List<RecipeItem> list = Loader.recipeItems.getOrDefault(MachineType.CRAFTING_MACHINE, Collections.emptyList());
        return list;
    }

    public int totalPages() {
        int recipes = sortedRecipes().size();
        return Math.max(1, (int) Math.ceil(recipes / (double) RECIPES_PER_PAGE));
    }

    public void renderListView(GUI gui, ItemGrid itemGrid) {
        detailViewRecipeId = null;

        // Clear any output preview that was painted while in detail view. If the slot legitimately
        // held a crafted item, the stash-restore below will put it back; if it was empty, it stays
        // empty. Without this explicit clear, an empty-output → detail → back sequence would leave
        // the recipe's output item visibly stuck in the output slot (and tickStop would persist it).
        // Also remove the blocking ItemGUI that was registered during detail view so the output slot
        // goes back to being a plain (non-interactive-locked) inventory slot.
        for (int slot : itemGrid.getOutputSlots()) {
            gui.getItemGUIs().remove(slot);
            gui.getInventory().setItem(slot, null);
        }

        // Restore stashed items (player's items that were hidden while in detail view)
        for (Map.Entry<Integer, ItemStack> e : stashedItems.entrySet()) {
            gui.getInventory().setItem(e.getKey(), e.getValue());
        }
        stashedItems.clear();

        // Paint recipe icons (the recipe's OUTPUT is what shows in the list)
        List<Integer> recipeSlots = itemGrid.getRecipeListSlots();
        List<RecipeItem> all = sortedRecipes();
        int pageStart = currentPage * RECIPES_PER_PAGE;
        for (int i = 0; i < recipeSlots.size(); i++) {
            int slot = recipeSlots.get(i);
            int recipeIdx = pageStart + i;
            if (recipeIdx < all.size()) {
                RecipeItem ri = all.get(recipeIdx);
                gui.getInventory().setItem(slot, ri.getOutputMaterial().getStackCached().clone());
            } else {
                gui.getInventory().setItem(slot, null);
            }
        }

        // Page navigation visibility — replace with filler when out of range (the click handler also no-ops, so
        // this is purely cosmetic)
        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages() - 1;
        for (int slot : itemGrid.getPageBackSlots()) {
            if (hasPrev) {
                restoreOriginal(gui, slot);
            } else {
                gui.getInventory().setItem(slot, fillerPane());
            }
        }
        for (int slot : itemGrid.getPageForwardSlots()) {
            if (hasNext) {
                restoreOriginal(gui, slot);
            } else {
                gui.getInventory().setItem(slot, fillerPane());
            }
        }
        // Always restore confirm button when in list view
        for (int slot : itemGrid.getConfirmCraftSlots()) {
            restoreOriginal(gui, slot);
        }
    }

    public void renderDetailView(GUI gui, ItemGrid itemGrid, String recipeId) {
        RecipeItem recipe = lookupRecipe(recipeId);
        if (recipe == null) return;

        detailViewRecipeId = recipeId;

        // Stash items from input + fuel + output slots so we can restore them on back-to-list.
        // The output slot in particular may hold a crafted item the player hasn't taken yet — it must
        // survive the preview paint below, otherwise it's silently overwritten.
        stashedItems.clear();
        for (int slot : itemGrid.getInputSlots()) {
            ItemStack s = gui.getInventory().getItem(slot);
            if (s != null) stashedItems.put(slot, s.clone());
            gui.getInventory().setItem(slot, null);
        }
        for (int slot : itemGrid.getFuelSlots()) {
            ItemStack s = gui.getInventory().getItem(slot);
            if (s != null) stashedItems.put(slot, s.clone());
            gui.getInventory().setItem(slot, null);
        }
        for (int slot : itemGrid.getOutputSlots()) {
            ItemStack s = gui.getInventory().getItem(slot);
            if (s != null) stashedItems.put(slot, s.clone());
            // don't null the slot here — the preview paint below will overwrite it
        }

        // Repaint recipe-list area with the recipe's inputs (first N slots)
        List<Integer> recipeSlots = itemGrid.getRecipeListSlots();
        for (int slot : recipeSlots) {
            gui.getInventory().setItem(slot, null);
        }
        List<SimpleItem> inputs = recipe.getInputMaterials();
        for (int i = 0; i < inputs.size() && i < recipeSlots.size(); i++) {
            gui.getInventory().setItem(recipeSlots.get(i), inputs.get(i).getStackCached().clone());
        }

        // Output preview — registered as a blocking ItemGUI so the player cannot pick it up.
        // renderListView removes this ItemGUI when returning to list view.
        for (int slot : itemGrid.getOutputSlots()) {
            final ItemStack preview = recipe.getOutputMaterial().getStackCached().clone();
            gui.setItem(slot, new ItemGUI(preview) {
                @Override
                public boolean onClick(Player p, HolderGUI h, ClickType ct, int s,
                                       InventoryAction a, InventoryClickEvent ev) {
                    // Return false → MenuListener cancels the event → preview is read-only.
                    // (true would ALLOW the click and let the player take the preview item.)
                    return false;
                }
            });
            break; // only first output slot
        }

        // Hide page-forward + confirm; turn first page-back slot into the detail back button
        for (int slot : itemGrid.getPageForwardSlots()) {
            gui.getInventory().setItem(slot, fillerPane());
        }
        for (int slot : itemGrid.getConfirmCraftSlots()) {
            gui.getInventory().setItem(slot, fillerPane());
        }
        if (!itemGrid.getPageBackSlots().isEmpty()) {
            gui.getInventory().setItem(itemGrid.getPageBackSlots().get(0), detailBackItem());
        }
    }

    public void changePage(GUI gui, ItemGrid itemGrid, int delta) {
        if (isInDetailView()) return;
        int newPage = currentPage + delta;
        if (newPage < 0 || newPage >= totalPages()) return;
        currentPage = newPage;
        renderListView(gui, itemGrid);
    }

    /** Returns the recipe at the given absolute slot in the current page, or null. */
    public RecipeItem recipeAtSlot(ItemGrid itemGrid, int slot) {
        int idx = itemGrid.getRecipeListSlots().indexOf(slot);
        if (idx < 0) return null;
        int recipeIdx = currentPage * RECIPES_PER_PAGE + idx;
        List<RecipeItem> all = sortedRecipes();
        if (recipeIdx >= all.size()) return null;
        return all.get(recipeIdx);
    }

    /**
     * Player clicked the emerald confirm button. Look for any recipe (sorted by Order) whose inputs are
     * satisfied by the current slot contents. First match → set as active. No match → INVALID_INPUT.
     */
    public void tryStartCrafting(GUI gui, ItemGrid itemGrid) {
        if (isInDetailView()) return;
        if (activeRecipeId != null) return; // already crafting

        Map<Integer, ItemStack> inputSnapshot = getInputSnapshot(gui, itemGrid);

        for (RecipeItem ri : sortedRecipes()) {
            if (ri.matchesInputs(inputSnapshot)) {
                activeRecipeId = ri.getRecipeId();
                setMachineState(MachineState.RUNNING);
                machineInternalData.setCurrentTick(0);
                return;
            }
        }
        setMachineState(MachineState.INVALID_INPUT);
        updateStatusItem(gui, itemGrid);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Map<Integer, ItemStack> getInputSnapshot(GUI gui, ItemGrid itemGrid) {
        Map<Integer, ItemStack> map = new HashMap<>();
        for (int slot : itemGrid.getInputSlots()) {
            ItemStack s = gui.getInventory().getItem(slot);
            if (s != null) map.put(slot, s);
        }
        return map;
    }

    private RecipeItem lookupRecipe(String recipeId) {
        HashMap<String, RecipeItem> index = Loader.recipeByIdIndex.get(MachineType.CRAFTING_MACHINE);
        if (index == null) return null;
        return index.get(recipeId);
    }

    private void restoreOriginal(GUI gui, int slot) {
        // The original ItemGUI (set up by fillItemsToGUI) holds the configured ItemStack. We re-stamp the
        // inventory with that stack so any filler/replacement is undone.
        if (gui.getItemGUI(slot) != null) {
            gui.getInventory().setItem(slot, gui.getItemGUI(slot).getItem());
        }
    }

    private ItemStack fillerPane() {
        ItemStack s = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = s.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
            s.setItemMeta(meta);
        }
        return s;
    }

    private ItemStack detailBackItem() {
        ItemStack s = new ItemStack(Material.PAPER);
        ItemMeta meta = s.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("← Back to recipes", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to return to the recipe list", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            s.setItemMeta(meta);
        }
        return s;
    }
}
