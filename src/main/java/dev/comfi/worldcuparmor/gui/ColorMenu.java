package dev.comfi.worldcuparmor.gui;

import dev.comfi.worldcuparmor.WorldCupArmorPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ColorMenu implements Menu {

    private static final Map<DyeColor, Color> VIBRANT = new EnumMap<>(DyeColor.class);

    static {
        VIBRANT.put(DyeColor.WHITE, Color.fromRGB(0xFFFFFF));
        VIBRANT.put(DyeColor.ORANGE, Color.fromRGB(0xFF8000));
        VIBRANT.put(DyeColor.MAGENTA, Color.fromRGB(0xFF00FF));
        VIBRANT.put(DyeColor.LIGHT_BLUE, Color.fromRGB(0x33CCFF));
        VIBRANT.put(DyeColor.YELLOW, Color.fromRGB(0xFFFF00));
        VIBRANT.put(DyeColor.LIME, Color.fromRGB(0x00FF00));
        VIBRANT.put(DyeColor.PINK, Color.fromRGB(0xFF69B4));
        VIBRANT.put(DyeColor.GRAY, Color.fromRGB(0x7F7F7F));
        VIBRANT.put(DyeColor.LIGHT_GRAY, Color.fromRGB(0xC8C8C8));
        VIBRANT.put(DyeColor.CYAN, Color.fromRGB(0x00FFFF));
        VIBRANT.put(DyeColor.PURPLE, Color.fromRGB(0x9900FF));
        VIBRANT.put(DyeColor.BLUE, Color.fromRGB(0x0000FF));
        VIBRANT.put(DyeColor.BROWN, Color.fromRGB(0x8B4513));
        VIBRANT.put(DyeColor.GREEN, Color.fromRGB(0x00A800));
        VIBRANT.put(DyeColor.RED, Color.fromRGB(0xFF0000));
        VIBRANT.put(DyeColor.BLACK, Color.fromRGB(0x000000));
    }

    private final WorldCupArmorPlugin plugin;
    private final String team;
    private final EquipmentSlot target;
    private final Inventory inventory;
    private final Map<Integer, DyeColor> dyeSlots = new HashMap<>();

    public ColorMenu(WorldCupArmorPlugin plugin, String team, EquipmentSlot target) {
        this.plugin = plugin;
        this.team = team;
        this.target = target;
        String piece = target == null ? "all pieces" : PieceMenu.pieceName(target).toLowerCase(Locale.ROOT);
        this.inventory = Bukkit.createInventory(this, 36,
                Component.text("Dye " + piece + ": " + team, NamedTextColor.DARK_GRAY));
        build();
    }

    private void build() {
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            DyeColor dye = dyes[i];
            int slot = i < 8 ? i : i + 1;
            Color color = VIBRANT.get(dye);
            inventory.setItem(slot, GuiItems.item(Material.valueOf(dye.name() + "_DYE"),
                    Component.text(prettify(dye.name()), TextColor.color(color.asRGB())),
                    List.of(Component.text("Click to apply this color", NamedTextColor.YELLOW),
                            Component.text("#" + String.format("%06X", color.asRGB()), NamedTextColor.GRAY))));
            dyeSlots.put(slot, dye);
        }
        inventory.setItem(27, GuiItems.item(Material.ARROW,
                Component.text("Back", NamedTextColor.WHITE), List.of()));
        inventory.setItem(31, GuiItems.item(Material.NAME_TAG,
                Component.text("Enter exact hex code", NamedTextColor.AQUA),
                List.of(Component.text("Click, then type the code in chat", NamedTextColor.YELLOW))));
    }

    private String prettify(String name) {
        String lower = name.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
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
        DyeColor dye = dyeSlots.get(slot);
        if (dye != null) {
            apply(player, VIBRANT.get(dye), prettify(dye.name()));
            return;
        }
        switch (slot) {
            case 27 -> new PieceMenu(plugin, team).open(player);
            case 31 -> {
                player.closeInventory();
                plugin.hexInput().await(player, team, target);
            }
            default -> {
            }
        }
    }

    private void apply(Player player, Color color, String label) {
        if (target == null) {
            plugin.colors().setAll(team, color);
            player.sendMessage(Component.text("Dyed all pieces of " + team + " " + label + ".", NamedTextColor.GREEN));
        } else {
            plugin.colors().setColor(team, target, color);
            player.sendMessage(Component.text("Dyed the " + PieceMenu.pieceName(target).toLowerCase(Locale.ROOT)
                    + " of " + team + " " + label + ".", NamedTextColor.GREEN));
        }
        plugin.disguises().refreshTeam(team);
        new PieceMenu(plugin, team).open(player);
    }
}
