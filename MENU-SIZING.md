# Menu sizing is now automatic

Machine menus used to have a fixed size that was separate from the actual slot
layout, so adding or removing a row in `menus.yml` didn't resize the menu —
you'd get a menu with dead empty rows, or slots that got cut off.

That's fixed. The menu size is now calculated from the number of rows you
define in each machine's `ItemGrid`. No more manual size field to keep in
sync.

## How to use it

Open `menus.yml` and find the machine/tier you want to change, e.g.:

```yaml
GrowingMachine:
  ItemGrid:
    '1':
      - "x,x,x,x,x,x,s,x,x"
      - "f,x,i,i,i,x,o,o,x"
      - "x,x,p,p,p,x,o,o,x"
      - "c,x,x,x,x,x,o,o,x"
```

- **Add a row** → the menu gets one row taller (9 more slots).
- **Remove a row** → the menu gets one row shorter.
- Each row must have exactly **9 entries** separated by commas.
- Sizing is **per tier**, so tier 1, 2, and 3 can have different heights if you want.
- Maximum is **6 rows** (54 slots — Minecraft's chest limit). If you configure more, the extra rows are ignored and a warning is printed in the console.

After editing, just run:

```
/zmachines reload
```

No restart needed.
