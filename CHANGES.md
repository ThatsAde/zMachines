# zMachines — Development Recap

## Overview

Full implementation of the two machine types (Rubble Processor and Growing Machine), a complete tier system rewrite, and a medium-scope codebase cleanup. All changes are backward-compatible with existing placed machines via an automatic data migration.

---

## 1. Rubble Processor

**Problem:** The recipe matching logic required all 4 input slots to contain the *same* material. The machine was therefore non-functional for its intended use case.

**Fix:** Rewrote the recipe matching to accept any block from Bukkit's `Tag.BASE_STONE_OVERWORLD` tag (cobblestone, granite, diorite, andesite, tuff, deepslate, etc.). Slots may hold different materials simultaneously. One item is consumed from each of the 4 input slots per cycle.

---

## 2. Growing Machine

**Per-slot progress:** Each seed slot now tracks its own independent grow cycle. The wool indicator directly below a seed slot reflects only that slot's progress — gray when empty, cycling red → orange → yellow → lime while growing, and resetting to red after each harvest. Slots left empty stay gray.

**Tier-scaled slot count:** The number of seed (and matching wool) slots scales with tier:
- Tier 1 → 3 slots
- Tier 2 → 4 slots
- Tier 3 → 5 slots

**Seeds are never consumed**, matching vanilla farming behavior.

---

## 3. Tier System Rewrite

Tiers are now numbered **1, 2, 3** (previously 0, 1, 2). Speed scales automatically from a single `BaseRate` value in the config:

| Tier | Speed multiplier | Example (BaseRate = 4.0 s) |
|------|-----------------|---------------------------|
| 1    | 1.0×            | 4.00 s per cycle           |
| 2    | 1.2×            | 3.33 s per cycle           |
| 3    | 1.44×           | 2.78 s per cycle           |

No more hardcoded per-tier rate tables. Each recipe only defines one `BaseRate`; tier 2 and 3 timings are derived automatically.

**Backward compatibility:**
- Existing machines stored in the database (old tier 0/1/2) are automatically migrated to 1/2/3 on first plugin startup — no manual action needed.
- Machine items in player inventories from versions before 2.0 are detected via an `item_version` NBT tag and their tier is adjusted on placement.

---

## 4. Codebase Cleanup

### Unified tick logic
`Machine` and `GrowingMachine` previously had ~70% duplicated tick logic with the only real differences being whether to consume inputs and whether to update progress wool. This was consolidated into a template method pattern:

- `consumesInputs()` — returns `true` by default; `GrowingMachine` overrides to `false`
- `onRecipeProgress(...)` — no-op by default; `GrowingMachine` overrides to update its wool slots

`GrowingMachine.onTick` and `GrowingMachine.update` (~130 lines of duplicated code) were deleted entirely.

### Replaced tuple boilerplate with Java records
`PairInternal`, `TripletInternal`, and `QuadrupletInternal` (pre-Java 16 boilerplate classes) were replaced with Java records `Pair<A,B>` and `Triplet<A,B,C>`. `QuadrupletInternal` was removed entirely as it had no usages.

### O(1) recipe lookup
`Loader.getRecipeItem()` previously iterated all recipes linearly on every tick (called every 50 ms per slot). A `HashMap<Material, RecipeItem>` index is now populated at config load time, making the lookup O(1).

### Removed dead code
- `EmptyItemGUI.java` — class never instantiated
- Dead `public static GUI gui` fields in `RubbleProcessor` and `GrowingMachine`
- Leftover commented-out blocks in `InventoryUtils`

### Fixed inverted click-cancel logic
`MenuListener` was calling `e.setCancelled(allow)` instead of `e.setCancelled(!allow)`, meaning every handled click was being cancelled rather than allowed. Standard Bukkit convention restored.

### Fixed SQLite schema typo
The database schema contained `owner´text` (backtick instead of space) which could cause column definition issues on certain SQLite versions. Corrected to `owner text`.

---

## 5. Configuration Split

The single `config.yml` was split into three focused files:

| File | Contents |
|---|---|
| `config.yml` | Machine block materials, recipes, fuel — the settings you change for gameplay |
| `menus.yml` | GUI item definitions (`Items`) and inventory layouts per tier (`Menus`) |
| `display.yml` | Status indicator items (`Status`) and progress wool items (`Progress`) |

All three files support live `/zmachines reload`.

---

## 6. Universal Fuel

Fuel items were previously defined separately per machine type. Since all machines use the same fuel, this was simplified to a single shared list:

```yaml
Fuel:
  - "COAL:1.0"
  - "CHARCOAL:0.8"
```

Any fuel item listed here works in every machine. The per-machine fuel sections were removed.

---

## 7. Documentation

- **`GUIDE.md`** — configuration reference and step-by-step testing checklist covering both machine types, all three tiers, break/drop behavior, and server-restart persistence.
