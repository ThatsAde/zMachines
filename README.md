# zMachines

Machine blocks for Paper servers. Players feed a machine fuel and materials and it produces items on its
own: a Rubble Processor turns stone into resources, a Growing Machine grows crops from seeds, and a
Crafting Machine runs configured recipes. Machines come in three tiers, can be fed by hoppers, and can use
Nexo custom blocks, including blocks pasted in with WorldEdit.

## Credits

The original plugin is by **petulikan1** and **ItsmeFawaz**, developed for
[Abstract-Resources](https://github.com/Abstract-Resources). I (ThatsAde) worked on it
as a contributor. The package name, the overall structure and most of the code are theirs; the section
below lists what I built or changed.

## My contributions

Compared with the original code base (February 2026, about 14,000 lines of Java in 74 files), my work
adds about 2,100 lines and changes about 780 more, across 36 files, plus the documentation.

**Machines**
- **Rubble Processor:** the recipe check required the same material in all four input slots, so the
  machine didn't work for its intended use. It now accepts any block in `BASE_STONE_OVERWORLD`
  (cobblestone, granite, deepslate, tuff…), mixed across slots.
- **Growing Machine:** each seed slot has its own grow cycle and its own progress indicator, the number of
  slots scales with the tier (3, 4, 5), and seeds aren't consumed, like vanilla farming.
- **Crafting Machine** (new, `CraftingMachine`): a paginated list of the configured recipes, a detail view
  for each recipe, and a confirm button so the wrong recipe never runs by accident.
- **Hopper automation** (new, `HopperTransferTask`): machine blocks aren't vanilla inventory holders, so
  hoppers did nothing. They now move items in and out the way they do with a furnace.

**Tiers**
- Rewrote the tier system as tiers 1 to 3. Speed comes from a single `BaseRate` per recipe (×1.0, ×1.2,
  ×1.44) instead of a hardcoded table per tier.
- Old machines in the database (tiers 0 to 2) are migrated on the first start, and machine items made
  before the change are fixed when they're placed, using an `item_version` tag.

**Configuration and menus**
- Split the single `config.yml` into `config.yml`, `menus.yml` and `display.yml`, all reloadable.
- One shared fuel list for every machine instead of one per machine type.
- Menu size follows the configured rows instead of a separate size field, and menu titles can show the
  tier with `<tier>`.
- Menu icons can use a resource-pack model through an optional `ItemModel` key.

**Nexo**
- Per-tier machine blocks are read correctly in every config form. A missing or misspelled block is no
  longer silently replaced by a vanilla furnace or composter.
- Nexo noteblocks are recognised by their mechanic id, so machine blocks pasted with WorldEdit register
  themselves on the first right-click.
- Nexo items work as recipe inputs, not only as outputs.

**Code quality**
- Merged the duplicated tick logic of the machine classes into one template method, removing about 130
  lines.
- Replaced the hand-written pair and triplet classes with Java records.
- Recipe lookup went from a linear scan on every tick to a map built at load time.
- Removed dead code, and fixed two bugs: menu clicks were cancelled when they should have been allowed,
  and the SQLite schema had a typo in a column definition.

**Paper 26.2**
- Ported the plugin to Paper 26.2 and Java 25.

**Docs:** [GUIDE.md](GUIDE.md) (setup, configuration, testing checklist), [CHANGES.md](CHANGES.md) (a
recap of the rework) and [MENU-SIZING.md](MENU-SIZING.md).

## Requirements

- Paper 26.2 or newer, on Java 25
- Nexo 1.24 or newer, optional, needed only for Nexo custom blocks and items

The Paper 26.2 port compiles, but hasn't been tested on a live server yet.

## Building

```bash
./gradlew build
```

The jar ends up in `build/libs/`. See [GUIDE.md](GUIDE.md) for installing and configuring it.

## Commands

| Command | Description |
|---|---|
| `/zmachines give <player> <type> <tier> [amount]` | Gives a machine block item |
| `/zmachines reload` | Reloads every config file |

Both need the `zMachines.admin` permission.
