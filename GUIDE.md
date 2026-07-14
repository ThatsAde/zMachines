# zMachines — Setup & Configuration Guide

A guide to installing and configuring zMachines. If you only read one section, read
**[How machines work](#how-machines-work-read-this-first)** — it explains the part that trips people up.

---

## Requirements & install

- **Paper** 1.21.x
- **Nexo** 1.24+ (optional, but required if you want Nexo custom blocks as machines)

Drop `zMachines-1.0.0-dev.jar` into your server's `plugins/` folder and restart. The config files are
generated in `plugins/zMachines/` on first start:

| File | What it controls |
|---|---|
| `config.yml` | Machine blocks, recipes, fuel, hoppers |
| `menus.yml` | The in-GUI slot layout for each machine/tier |
| `display.yml` | Display/formatting strings |
| `translations.yml` | Player-facing messages |

After editing any of these, run `/zmachines reload` — no restart needed.

---

## How machines work (read this first)

A machine is just a **block** with a GUI attached to its location. You tell the plugin *which block* is a
machine in `config.yml`. From then on:

- **Placing** that block (by hand, from a shop, from a crafting recipe, **or via WorldEdit**) turns it into
  a working machine.
- **Right-clicking** it opens the machine GUI.
- **Breaking** it removes the machine and drops its contents.

### The important part: WorldEdit / prebuilt bases

When WorldEdit pastes a schematic (e.g. a prebuilt starter base with a machine block already inside), it
sets the blocks directly — **no place event fires**, so the plugin doesn't know a machine was created yet.

That's fine. The machine **activates automatically the first time a player right-clicks it.** From that
click on, it's a normal machine. You don't need to run any command or place anything by hand.

> ✅ This means you can build a starter base once, put the machine block inside, and every player who
> receives that base will have a working machine after the first click.

---

## Configuring the machine blocks

Each machine type has an `ItemStack` section. Set the block for **each tier** (`Tier1`/`Tier2`/`Tier3`)
to either a vanilla material or a Nexo block ID (`nexo:<id>`).

```yaml
GrowingMachine:
  Name: "<gold>Growing Machine"
  ItemStack:
    Tier1: "nexo:growing_machine_1"
    Tier2: "nexo:growing_machine_2"
    Tier3: "nexo:growing_machine_3"
```

Each tier is one block. If you want the same block for every tier, just put the same value in all three.

> ⚠️ **There is no fallback.** Every tier you want usable must name a block, and the Nexo ID must match
> the **exact** ID from your Nexo config (the part after `nexo:`). If a tier is left blank or the ID is
> misspelled, that block simply **won't become a machine** — it stays a plain block, and `/zmachines give`
> reports an error rather than handing out a default block. After changing IDs, run `/zmachines reload`.

The three machine types and the vanilla blocks they ship with by default (all three tiers):

| Machine | Config key | Default block |
|---|---|---|
| Rubble Processor | `RubbleProcessor` | `FURNACE` |
| Growing Machine | `GrowingMachine` | `COMPOSTER` |
| Crafting Machine | `CraftingMachine` | `MAGENTA_CONCRETE` |

Replace the per-tier `Tier1`/`Tier2`/`Tier3` lines with your Nexo IDs as shown above.

---

## The three machine types

### Rubble Processor
Processes stone-type blocks into an output. All input slots accept any block tagged
`BASE_STONE_OVERWORLD` (cobblestone, granite, diorite, andesite, tuff, deepslate, …). Consumes one block
from each filled input slot per cycle.

### Growing Machine
"Grows" seeds into their harvest. One named recipe per seed type. **Seeds are never consumed** (like
vanilla farming) — the machine keeps producing as long as it has fuel.

### Crafting Machine
Crafts custom items from a list of recipes. Inputs are summed across the input slots, so players can split
the required items across slots in any order. Outputs support both vanilla and Nexo items.

---

## Recipes

All recipes live under the top-level `Recipes:` section, grouped by machine.

### Rubble Processor — one global recipe
```yaml
Recipes:
  RubbleProcessor:
    BaseRate: 4.0      # seconds per cycle at tier 1
    Output: "DIRT:1"   # MATERIAL:amount
```

### Growing Machine — one entry per seed
```yaml
Recipes:
  GrowingMachine:
    pumpkin:
      BaseRate: 30.0
      Input:  "PUMPKIN_SEEDS"
      Output: "PUMPKIN:1"
    melon:
      BaseRate: 35.0
      Input:  "MELON_SEEDS"
      Output: "MELON:1"
```

### Crafting Machine — ordered recipe list
```yaml
Recipes:
  CraftingMachine:
    iron_sword:
      Order: 1                 # position in the recipe list (1 = first)
      BaseRate: 30.0
      Inputs:
      - "IRON_INGOT:2"
      - "STICK:1"
      Output: "IRON_SWORD:1"   # vanilla, or Nexo: "nexo:rifle:1"
```

**Output format** is `MATERIAL:amount` for vanilla or `nexo:<id>:amount` for Nexo items.

---

## Fuel

Every machine consumes fuel while running. Configure the accepted fuels and their burn value:

```yaml
Fuel:
- "COAL:1.0"   # MATERIAL:value
```

---

## Tiers

Tiers 1–3 control speed. Each tier up runs **×1.2 faster**:

| Tier | Speed vs tier 1 | A 30s recipe takes |
|---|---|---|
| 1 | 1.0× | 30.0s |
| 2 | 1.2× | 25.0s |
| 3 | 1.44× | ~20.8s |

The recipe `BaseRate` is the tier-1 time; the plugin scales it down for higher tiers automatically.

---

## Hoppers

Set `Hoppers: true` (top of `config.yml`) to enable hopper automation. When on:

- A hopper **on top** (facing down) → inserts fuel and inputs.
- A hopper **on any side** (facing into the machine) → inserts fuel and inputs.
- A hopper **underneath** → extracts the machine's output.

Toggle it live with `/zmachines reload`.

---

## Admin commands

Permission: `zMachines.admin`.

| Command | Description |
|---|---|
| `/zmachines give <player> <type> <tier> [amount]` | Gives a machine block item. `<type>` = `RUBBLE_PROCESSOR`, `GROWING_MACHINE`, `CRAFTING_MACHINE` (or the config name). `<tier>` = 1–3. |
| `/zmachines reload` | Reloads all config files and re-reads machine block IDs. |

The give command is just a convenience for handing out machines manually — it is **not** required for the
WorldEdit/starter-base flow, which works purely from the block being placed and clicked.

---

## Setting up a WorldEdit starter base

1. In `config.yml`, set the machine block(s) to your Nexo IDs per tier (see
   [Configuring the machine blocks](#configuring-the-machine-blocks)) and run `/zmachines reload`.
2. Build your starter base and place the **Nexo machine block** inside it where you want the machine.
3. Save it as a schematic / set it up in whatever plugin pastes the base.
4. When a player spawns the base, the machine block is pasted with it. The player **right-clicks it once**
   to activate → the GUI opens and the machine is now live.
5. Breaking the block removes the machine and returns the item.

No command, no special tagged item — just the configured Nexo block in the right spot.

---

## Troubleshooting

**Right-clicking the block does nothing / no GUI opens**
- The Nexo ID in `config.yml` doesn't match the actual Nexo block ID. Check spelling (the part after
  `nexo:`), then `/zmachines reload`. There's no fallback, so a mismatched ID means the block is just a
  plain block — exactly this symptom.
- Nexo isn't loaded. Confirm Nexo is installed and enabled; check the console at startup for a zMachines
  error about Nexo support being disabled.

**`/zmachines give` says "No block configured for … tier N"**
- That tier has no block set (or the value is empty/misspelled). Set `TierN` for that machine to a vanilla
  material or `nexo:<id>` in `config.yml`, then `/zmachines reload`. This is by design — there is no
  default-block fallback.

**`/zmachines` command not found**
- The plugin failed to enable. Check the console for a red `[ERROR] zMachines >>` line and send it over.

**Machine block doesn't drop the right item when broken**
- Make sure you're breaking the actual machine block. A block that was never activated (never clicked)
  drops the plain Nexo item, which is expected — re-place and click it to re-activate.

**Changed the config but nothing changed in-game**
- Run `/zmachines reload`. If you changed a block ID, existing already-placed machines keep their old block;
  the new ID applies to newly placed/activated ones.
