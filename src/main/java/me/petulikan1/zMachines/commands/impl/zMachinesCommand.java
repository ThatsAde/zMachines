package me.petulikan1.zMachines.commands.impl;

import lombok.NonNull;
import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.dataholders.Tier;
import me.petulikan1.zMachines.hooks.Nexo;
import me.petulikan1.zMachines.items.BasicItem;
import me.petulikan1.zMachines.messages.Mini;
import me.petulikan1.zMachines.messages.MiniImpl;
import me.petulikan1.zMachines.utils.PDC;
import me.petulikan1.zMachines.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class zMachinesCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command command, @NotNull String ds, @NotNull String[] args) {
        if (!s.hasPermission("zMachines.admin"))
            return false;
        if (args.length == 0) {
            Mini.mm(s, "Help");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            /*            Loader.translations.reload();*/
            Loader.cfg.reload();
            Loader.menuCfg.reload();
            Loader.displayCfg.reload();
            API.reload(false);

            Mini.mm(s, "Reloaded");
            return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            // /zmachines give <player> <type> <tier> [amount] [facing]
            // facing = North|South|East|West (default: North / TierN.Material)
            if (args.length < 3) {
                Mini.mm(s, "Give.Help");
                return true;
            }
            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                Mini.mm(s, "Commons.PlayerOffline");
                return true;
            }
            MachineType mt = MachineType.getMachineType(args[2]);
            if (mt == null) {
                Mini.mm(s, "Give.InvalidMachineType",
                        comp("machines", Component.join(JoinConfiguration.commas(true), MachineType.getMachineTypeComponents()))
                );
                return true;
            }
            int tier;
            try {
                if (args.length < 4)
                    tier = Tier.MIN;
                else
                    tier = Integer.parseInt(args[3]);
            } catch (Exception e) {
                Mini.mm(s, "Give.InvalidNumber",
                        comp("number", Component.text(args[3]))
                );
                return true;
            }
            if (!Tier.isValid(tier)) {
                Mini.mm(s, "Give.InvalidNumber",
                        comp("number", Component.text(tier))
                );
                return true;
            }

            int amount;
            try {
                if (args.length < 5)
                    amount = 1;
                else
                    amount = Integer.parseInt(args[4]);
            } catch (Exception e) {
                Mini.mm(s, "Give.InvalidNumber",
                        comp("number", Component.text(args[4]))
                );
                return true;
            }

            // Optional facing argument: North|South|East|West (case-insensitive, default = "North")
            String facing = "North";
            if (args.length >= 6) {
                String facingArg = args[5].substring(0, 1).toUpperCase() + args[5].substring(1).toLowerCase();
                if (!GIVE_FACINGS.contains(facingArg)) {
                    s.sendMessage("Invalid facing. Use: " + String.join(", ", GIVE_FACINGS));
                    return true;
                }
                facing = facingArg;
            }

            String key = mt.getIdentifier();
            BasicItem basicItem = new BasicItem(Loader.cfg, key + ".ItemStack");
            basicItem.setNbtConsumer(pdc -> {
                pdc.setBoolean("machine_block", true);
                pdc.setDouble("item_version", basicItem.getItemVersion());
                pdc.setString("machine_id", mt.name());
                pdc.setInt("machine_tier", tier);
            });

            String mat = API.getMaterialRaw(key, tier, facing);
            boolean isNexoItem = mat.toLowerCase().startsWith("nexo:");
            Material m = Material.getMaterial(mat.toUpperCase());
            Component machineName = MiniImpl.getTrC(Loader.cfg, key, "Name");
            ItemStack item = null;
            if (m == null && isNexoItem && Loader.isNexoSupport) {
                String ID = mat.substring(5);
                item = Nexo.getNexoItem(ID);
                ItemMeta meta = item.getItemMeta();
                PDC pdc = new PDC(meta);
                pdc.setBoolean("machine_block", true);
                pdc.setDouble("item_version", basicItem.getItemVersion());
                pdc.setString("machine_id", mt.name());
                pdc.setString("nexoid", ID);
                pdc.setInt("machine_tier", tier);
                item.setItemMeta(meta);
                item.setAmount(amount);
            }
            if (item == null) {
                // Only fall back to the enum-default material if the configured one didn't resolve.
                // Previously this branch always overwrote `m`, which clobbered valid vanilla materials
                // (e.g. MAGENTA_CONCRETE) and forced every give to use the default block.
                if (m == null) m = mt.getDefaultMaterial();
                item = basicItem.build(m, comp("machine", machineName));
                item.setAmount(amount);
            }

            HashMap<Integer, ItemStack> residual = player.getInventory().addItem(item);
            if (!residual.isEmpty()) {
                for (ItemStack i : residual.values()) {
                    player.getLocation().getWorld().dropItemNaturally(player.getLocation(), i);
                }
            }
            Mini.mm(s, "Give.Success", comp("machine", machineName), comp("amount", amount), comp("player", player.getName()));
            return true;
        }
        Mini.mm(s, "Help");
        return true;
    }

    private TagResolver comp(@TagPattern String key, Component component) {
        return Placeholder.component(key, component);
    }

    private TagResolver comp(@TagPattern String key, String val) {
        return Placeholder.component(key, Component.text(val));
    }

    private TagResolver comp(@TagPattern String key, int val) {
        return Placeholder.component(key, Component.text(val));
    }


    private Player getPlayer(@NonNull String player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(player))
                return p;
        }
        return null;
    }

    private List<String> getPlayers() {
        List<String> a = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            a.add(p.getName());
        }
        return a;
    }

    private final List<String> machineTypes = Arrays.stream(MachineType.values()).map(Enum::name).toList();
    private static final List<String> GIVE_FACINGS = List.of("North", "South", "East", "West");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command command, @NotNull String ds, @NotNull String[] args) {
        if (!s.hasPermission("zMachines.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return StringUtils.copyPartialMatches(args[0], List.of("reload", "give"));
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return StringUtils.copyPartialMatches(args[1], getPlayers());
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return StringUtils.copyPartialMatches(args[2], machineTypes);
            }
        }
        if (args.length == 6) {
            if (args[0].equalsIgnoreCase("give")) {
                return StringUtils.copyPartialMatches(args[5], GIVE_FACINGS);
            }
        }
        return List.of();
    }
}
