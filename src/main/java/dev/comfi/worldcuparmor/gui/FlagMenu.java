package dev.comfi.worldcuparmor.gui;

import dev.comfi.worldcuparmor.FlagService;
import dev.comfi.worldcuparmor.WorldCupArmorPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FlagMenu implements Menu {

    private static final int PER_PAGE = 45;

    private final WorldCupArmorPlugin plugin;
    private final String team;
    private final int page;
    private final int pages;
    private final Inventory inventory;
    private final Map<Integer, String> flagSlots = new HashMap<>();

    public FlagMenu(WorldCupArmorPlugin plugin, String team, int page) {
        this.plugin = plugin;
        this.team = team;
        List<String> flags = plugin.flags().available();
        this.pages = Math.max(1, (flags.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.min(Math.max(page, 0), pages - 1);
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text("Flag: " + team + " (" + (this.page + 1) + "/" + pages + ")", NamedTextColor.DARK_GRAY));
        build(flags);
    }

    private void build(List<String> flags) {
        String current = plugin.colors().flag(team);
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < flags.size(); i++) {
            String flag = flags.get(start + i);
            boolean selected = flag.equals(current);
            Component name = plugin.flags().sprite(flag)
                    .append(Component.text(" " + FlagService.pretty(flag),
                            selected ? NamedTextColor.GREEN : NamedTextColor.WHITE));
            inventory.setItem(i, GuiItems.selected(GuiItems.item(Material.WHITE_BANNER, name,
                    List.of(Component.text(selected ? "Current flag of " + team : "Click to set as the flag of " + team,
                            selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW))), selected));
            flagSlots.put(i, flag);
        }
        inventory.setItem(45, GuiItems.item(Material.ARROW,
                Component.text("Back", NamedTextColor.WHITE), List.of()));
        if (page > 0) {
            inventory.setItem(48, GuiItems.item(Material.PAPER,
                    Component.text("Previous page", NamedTextColor.YELLOW), List.of()));
        }
        if (page < pages - 1) {
            inventory.setItem(50, GuiItems.item(Material.PAPER,
                    Component.text("Next page", NamedTextColor.YELLOW), List.of()));
        }
        inventory.setItem(53, GuiItems.item(Material.BARRIER,
                Component.text("Remove flag", NamedTextColor.RED),
                List.of(Component.text("Clears the team prefix", NamedTextColor.GRAY))));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        String flag = flagSlots.get(slot);
        if (flag != null) {
            plugin.colors().setFlag(team, flag);
            plugin.flags().apply(team);
            player.sendMessage(Component.text("Set the flag of " + team + " to ", NamedTextColor.GREEN)
                    .append(plugin.flags().sprite(flag))
                    .append(Component.text(" " + FlagService.pretty(flag) + ".", NamedTextColor.GREEN)));
            new FlagMenu(plugin, team, page).open(player);
            return;
        }
        switch (slot) {
            case 45 -> new PieceMenu(plugin, team).open(player);
            case 48 -> new FlagMenu(plugin, team, page - 1).open(player);
            case 50 -> new FlagMenu(plugin, team, page + 1).open(player);
            case 53 -> {
                plugin.colors().removeFlag(team);
                plugin.flags().apply(team);
                player.sendMessage(Component.text("Removed the flag of " + team + ".", NamedTextColor.RED));
                new FlagMenu(plugin, team, page).open(player);
            }
            default -> {
            }
        }
    }
}
