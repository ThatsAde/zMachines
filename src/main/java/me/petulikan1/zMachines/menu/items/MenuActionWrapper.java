package me.petulikan1.zMachines.menu.items;

import lombok.Getter;
import me.petulikan1.zMachines.Loader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
public class MenuActionWrapper {
    private final @NotNull MenuAction action;
    private final @Nullable String data;

    public MenuActionWrapper(String action) {
        String[] split = action.split(":");
        String val;
        String addData = null;
        if (split.length == 0) {
            val = action;
        } else if (split.length == 1) {
            val = split[0];
        } else if (split.length == 2) {
            val = split[0];
            addData = split[1];
        } else {
            val = action;
        }
        for (MenuAction menuAction : MenuAction.values()) {
            if (menuAction.name().equalsIgnoreCase(val)) {
                if (menuAction == MenuAction.OPEN_MENU && addData == null) {
                    Loader.main.error("OPEN_MENU action requires an argument - `OPEN_MENU:menu_name`");
                }
                this.action = menuAction;
                this.data = addData;
                return;
            }
        }
        Loader.main.error("Failed to retrieve menu action - invalid name: " + action);
        Loader.main.error("Valid actions are: " + Arrays.toString(MenuAction.values()));
        this.action = MenuAction.NONE;
        this.data = null;
    }

}
