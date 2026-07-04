package dev.comfi.worldcuparmor.gui;

import dev.comfi.worldcuparmor.ArmorStyle;
import dev.comfi.worldcuparmor.FlagService;
import dev.comfi.worldcuparmor.WorldCupArmorPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PieceMenu implements Menu {

    private static final Map<Integer, EquipmentSlot> PIECE_SLOTS = Map.of(
            10, EquipmentSlot.HEAD,
            12, EquipmentSlot.CHEST,
            14, EquipmentSlot.LEGS,
            16, EquipmentSlot.FEET);

    private final WorldCupArmorPlugin plugin;
    private final String team;
    private final Inventory inventory;

    public PieceMenu(WorldCupArmorPlugin plugin, String team) {
        this.plugin = plugin;
        this.team = team;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Pieces: " + team, NamedTextColor.DARK_GRAY));
        build();
    }

    static String pieceName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "Helmet";
            case CHEST -> "Chestplate";
            case LEGS -> "Leggings";
            case FEET -> "Boots";
            default -> slot.name();
        };
    }

    static String trimName(ArmorStyle style) {
        String pattern = style.pattern().getKey().getKey();
        String material = style.material().getKey().getKey();
        return Character.toUpperCase(pattern.charAt(0)) + pattern.substring(1) + " / "
                + Character.toUpperCase(material.charAt(0)) + material.substring(1);
    }

    private static Material leatherFor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Material.LEATHER_HELMET;
            case CHEST -> Material.LEATHER_CHESTPLATE;
            case LEGS -> Material.LEATHER_LEGGINGS;
            case FEET -> Material.LEATHER_BOOTS;
            default -> Material.LEATHER_CHESTPLATE;
        };
    }

    private static Material netheriteFor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Material.NETHERITE_HELMET;
            case CHEST -> Material.NETHERITE_CHESTPLATE;
            case LEGS -> Material.NETHERITE_LEGGINGS;
            case FEET -> Material.NETHERITE_BOOTS;
            default -> Material.NETHERITE_CHESTPLATE;
        };
    }

    private void build() {
        for (Map.Entry<Integer, EquipmentSlot> entry : PIECE_SLOTS.entrySet()) {
            EquipmentSlot slot = entry.getValue();
            ArmorStyle style = plugin.colors().style(team, slot);
            ItemStack icon;
            List<Component> lore = new ArrayList<>();
            if (style == null || style.color() == null) {
                lore.add(Component.text("No color set", NamedTextColor.GRAY));
                if (style != null && style.hasTrim()) {
                    lore.add(Component.text("Trim: " + trimName(style), NamedTextColor.GRAY));
                }
                lore.add(Component.text("Left click to pick a color", NamedTextColor.YELLOW));
                lore.add(Component.text("Shift left click to pick a trim", NamedTextColor.AQUA));
                icon = GuiItems.styled(netheriteFor(slot), style == null ? ArmorStyle.EMPTY : style,
                        Component.text(pieceName(slot), NamedTextColor.WHITE), lore);
            } else {
                lore.add(Component.text("Color #" + String.format("%06X", style.color().asRGB()), NamedTextColor.GRAY));
                lore.add(Component.text(style.hasTrim() ? "Trim: " + trimName(style) : "No trim set", NamedTextColor.GRAY));
                lore.add(Component.text("Left click to change the color", NamedTextColor.YELLOW));
                lore.add(Component.text("Shift left click to pick a trim", NamedTextColor.AQUA));
                lore.add(Component.text("Right click to clear this piece", NamedTextColor.RED));
                icon = GuiItems.styled(leatherFor(slot), style,
                        Component.text(pieceName(slot), NamedTextColor.WHITE), lore);
            }
            inventory.setItem(entry.getKey(), icon);
        }
        inventory.setItem(22, GuiItems.item(Material.ARMOR_STAND,
                Component.text("All pieces", NamedTextColor.GOLD),
                List.of(Component.text("Click to dye all four pieces at once", NamedTextColor.YELLOW),
                        Component.text("Shift click to trim all four pieces at once", NamedTextColor.AQUA))));
        String flag = plugin.colors().flag(team);
        List<Component> flagLore = new ArrayList<>();
        if (flag == null) {
            flagLore.add(Component.text("No flag set", NamedTextColor.GRAY));
            flagLore.add(Component.text("Left click to pick a country", NamedTextColor.YELLOW));
        } else {
            flagLore.add(Component.text("Flag: ", NamedTextColor.GRAY)
                    .append(plugin.flags().sprite(flag))
                    .append(Component.text(" " + FlagService.pretty(flag), NamedTextColor.WHITE)));
            flagLore.add(Component.text("Left click to change the country", NamedTextColor.YELLOW));
            flagLore.add(Component.text("Right click to remove the flag", NamedTextColor.RED));
        }
        inventory.setItem(24, GuiItems.item(Material.WHITE_BANNER,
                Component.text("Country flag", NamedTextColor.GOLD), flagLore));
        inventory.setItem(18, GuiItems.item(Material.ARROW,
                Component.text("Back", NamedTextColor.WHITE), List.of()));
        inventory.setItem(26, GuiItems.item(Material.BARRIER,
                Component.text("Remove all colors", NamedTextColor.RED),
                List.of(Component.text("This team will show real armor", NamedTextColor.GRAY))));
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
        EquipmentSlot piece = PIECE_SLOTS.get(slot);
        if (piece != null) {
            if (event.isRightClick()) {
                plugin.colors().removePiece(team, piece);
                plugin.disguises().refreshTeam(team);
                new PieceMenu(plugin, team).open(player);
            } else if (event.isShiftClick()) {
                new TrimMenu(plugin, team, piece).open(player);
            } else {
                new ColorMenu(plugin, team, piece).open(player);
            }
            return;
        }
        switch (slot) {
            case 22 -> {
                if (event.isShiftClick()) {
                    new TrimMenu(plugin, team, null).open(player);
                } else {
                    new ColorMenu(plugin, team, null).open(player);
                }
            }
            case 24 -> {
                if (event.isRightClick()) {
                    plugin.colors().removeFlag(team);
                    plugin.flags().apply(team);
                    new PieceMenu(plugin, team).open(player);
                } else {
                    new FlagMenu(plugin, team, 0).open(player);
                }
            }
            case 18 -> new MainMenu(plugin).open(player);
            case 26 -> {
                plugin.colors().removeTeam(team);
                plugin.disguises().refreshTeam(team);
                player.sendMessage(Component.text("Removed all colors for " + team + ".", NamedTextColor.RED));
                new MainMenu(plugin).open(player);
            }
            default -> {
            }
        }
    }
}
