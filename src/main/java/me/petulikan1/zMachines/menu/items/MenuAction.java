package me.petulikan1.zMachines.menu.items;

import lombok.Getter;
import me.petulikan1.zMachines.config.MenuConfig;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.impl.CraftingMachine;
import me.petulikan1.zMachines.items.RecipeItem;
import me.petulikan1.zMachines.menu.GUI;
import me.petulikan1.zMachines.menu.HolderGUI;
import me.petulikan1.zMachines.menu.ItemGUI;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

@Getter
public enum MenuAction {
    CLOSE_MENU(ctx -> {
        ctx.holderGUI.close(ctx.player);
        return false;
    }),
    OPEN_MENU(ctx -> {
        ctx.gui.close(ctx.player);
        String menuName = ctx.menuItem.getAction().getData();
        if (menuName != null) {
            GUI newGUI = GUI.create(menuName, ctx.player, ctx.machine, ctx.tagResolver);
            if (newGUI != null) {
                newGUI.open(ctx.player);
            }
        }
        return false;
    }),
    NONE((ctx) -> false),
    FUEL_SLOT(ctx -> false),
    INPUT_SLOT(ctx -> false),
    OUTPUT_SLOT(ctx -> false),
    STATUS(ctx -> false),
    PROGRESS(ctx -> false),

    // ---------------------------------------------------------------------
    // Crafting Machine
    // ---------------------------------------------------------------------

    /** A slot in the paginated recipe list. Click opens the detail view for the recipe at that slot. */
    RECIPE_LIST(ctx -> {
        if (!(ctx.machine instanceof CraftingMachine cm)) return false;
        var grid = resolveItemGrid(ctx.machine);
        if (grid == null) return false;
        if (cm.isInDetailView()) return false;
        RecipeItem recipe = cm.recipeAtSlot(grid, ctx.slot);
        if (recipe == null) return false;
        cm.renderDetailView(ctx.gui, grid, recipe.getRecipeId());
        return false;
    }),

    /**
     * Previous-page button (paper). In list view it pages back; in detail view it acts as the "back to list"
     * button. Dispatched on view state so we don't need to swap the slot's ItemGUI when toggling modes.
     */
    PAGE_BACK(ctx -> {
        if (!(ctx.machine instanceof CraftingMachine cm)) return false;
        var grid = resolveItemGrid(ctx.machine);
        if (grid == null) return false;
        if (cm.isInDetailView()) {
            cm.renderListView(ctx.gui, grid);
        } else {
            cm.changePage(ctx.gui, grid, -1);
        }
        return false;
    }),

    /** Next-page button (paper). No-op when on the last page or in detail view. */
    PAGE_FORWARD(ctx -> {
        if (!(ctx.machine instanceof CraftingMachine cm)) return false;
        var grid = resolveItemGrid(ctx.machine);
        if (grid == null) return false;
        if (cm.isInDetailView()) return false;
        cm.changePage(ctx.gui, grid, +1);
        return false;
    }),

    /** Confirm-craft button (emerald). Scans recipes for first match and starts crafting if valid. */
    CONFIRM_CRAFT(ctx -> {
        if (!(ctx.machine instanceof CraftingMachine cm)) return false;
        var grid = resolveItemGrid(ctx.machine);
        if (grid == null) return false;
        cm.tryStartCrafting(ctx.gui, grid);
        return false;
    });

    private final MenuActionHandler handler;

    MenuAction(MenuActionHandler handler) {
        this.handler = handler;
    }

    private static me.petulikan1.zMachines.menu.items.ItemGrid resolveItemGrid(Machine machine) {
        MenuConfig cfg = MenuConfig.init(machine.getIdentifier(), machine.getTier());
        return cfg == null ? null : cfg.getItemGrid(machine.getTier());
    }


    @FunctionalInterface
    public interface MenuActionHandler {
        boolean handle(MenuActionContext ctx);
    }

    public record MenuActionContext(
            Player player,
            HolderGUI holderGUI,
            ClickType clickType,
            Integer slot,
            InventoryAction inventoryAction,
            InventoryClickEvent e,
            GUI gui,
            Player player2,
            ItemGUI itemGUI,
            TagResolver tagResolver,
            MenuItem menuItem,
            Machine machine
    ) {
    }


}
