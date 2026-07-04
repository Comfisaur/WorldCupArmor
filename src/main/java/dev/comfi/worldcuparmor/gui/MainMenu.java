package dev.comfi.worldcuparmor.gui;

import dev.comfi.worldcuparmor.ArmorStyle;
import dev.comfi.worldcuparmor.WorldCupArmorPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class MainMenu implements Menu {

    private static final List<EquipmentSlot> PIECE_ORDER =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private final WorldCupArmorPlugin plugin;
    private final Inventory inventory;
    private final Map<Integer, String> teamSlots = new HashMap<>();

    public MainMenu(WorldCupArmorPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("WorldCupArmor Teams", NamedTextColor.DARK_GRAY));
        build();
    }

    private void build() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            names.add(team.getName());
        }
        names.addAll(plugin.colors().configuredTeams());
        if (names.isEmpty()) {
            inventory.setItem(22, GuiItems.item(Material.PAPER,
                    Component.text("No teams found", NamedTextColor.RED),
                    List.of(Component.text("Create one with /team add <name>", NamedTextColor.GRAY))));
        }
        int slot = 0;
        for (String name : names) {
            if (slot >= 45) {
                break;
            }
            Map<EquipmentSlot, ArmorStyle> pieces = plugin.colors().pieces(name);
            ItemStack icon;
            if (pieces.isEmpty()) {
                icon = GuiItems.item(Material.IRON_CHESTPLATE,
                        Component.text(name, NamedTextColor.WHITE),
                        List.of(Component.text("No disguise colors set", NamedTextColor.GRAY),
                                Component.text("Left click to set up pieces", NamedTextColor.YELLOW)));
            } else {
                List<Component> lore = new ArrayList<>();
                for (EquipmentSlot piece : PIECE_ORDER) {
                    ArmorStyle style = pieces.get(piece);
                    if (style == null || style.color() == null) {
                        lore.add(Component.text(PieceMenu.pieceName(piece) + ": real armor", NamedTextColor.DARK_GRAY));
                    } else {
                        String trim = style.hasTrim() ? " (" + PieceMenu.trimName(style) + ")" : "";
                        lore.add(Component.text(PieceMenu.pieceName(piece) + ": #"
                                        + String.format("%06X", style.color().asRGB()) + trim,
                                TextColor.color(style.color().asRGB())));
                    }
                }
                lore.add(Component.text("Left click to edit pieces", NamedTextColor.YELLOW));
                lore.add(Component.text("Right click to remove all colors", NamedTextColor.RED));
                ArmorStyle chest = pieces.get(EquipmentSlot.CHEST);
                ArmorStyle display = chest != null && chest.color() != null ? chest
                        : pieces.values().stream().filter(s -> s.color() != null).findFirst().orElse(null);
                Color displayColor = display == null ? Color.WHITE : display.color();
                icon = GuiItems.styled(Material.LEATHER_CHESTPLATE,
                        display == null ? ArmorStyle.EMPTY.withColor(displayColor) : display,
                        Component.text(name, TextColor.color(displayColor.asRGB())), lore);
            }
            teamSlots.put(slot, name);
            inventory.setItem(slot, icon);
            slot++;
        }
        boolean enabled = plugin.colors().isEnabled();
        inventory.setItem(45, GuiItems.item(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                Component.text(enabled ? "Disguises enabled" : "Disguises disabled", enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
                List.of(Component.text("Click to toggle", NamedTextColor.YELLOW))));
        inventory.setItem(49, GuiItems.item(Material.BOOK,
                Component.text("Reload config", NamedTextColor.AQUA),
                List.of(Component.text("Click to reload from disk", NamedTextColor.YELLOW))));
        inventory.setItem(53, GuiItems.item(Material.BARRIER,
                Component.text("Close", NamedTextColor.RED), List.of()));
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
        String team = teamSlots.get(slot);
        if (team != null) {
            if (event.isRightClick()) {
                plugin.colors().removeTeam(team);
                plugin.disguises().refreshTeam(team);
                new MainMenu(plugin).open(player);
            } else {
                new PieceMenu(plugin, team).open(player);
            }
            return;
        }
        switch (slot) {
            case 45 -> {
                plugin.colors().setEnabled(!plugin.colors().isEnabled());
                plugin.disguises().refreshAll();
                new MainMenu(plugin).open(player);
            }
            case 49 -> {
                plugin.colors().load();
                plugin.disguises().refreshAll();
                new MainMenu(plugin).open(player);
            }
            case 53 -> player.closeInventory();
            default -> {
            }
        }
    }
}
