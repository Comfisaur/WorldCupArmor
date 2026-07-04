package dev.comfi.worldcuparmor.gui;

import dev.comfi.worldcuparmor.ArmorStyle;
import dev.comfi.worldcuparmor.WorldCupArmorPlugin;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TrimMenu implements Menu {

    private static final TrimMaterial DEFAULT_MATERIAL = TrimMaterial.QUARTZ;
    private static final TrimPattern DEFAULT_PATTERN = TrimPattern.SENTRY;

    private static final Map<String, Material> MATERIAL_ICONS = Map.ofEntries(
            Map.entry("quartz", Material.QUARTZ),
            Map.entry("iron", Material.IRON_INGOT),
            Map.entry("netherite", Material.NETHERITE_INGOT),
            Map.entry("redstone", Material.REDSTONE),
            Map.entry("copper", Material.COPPER_INGOT),
            Map.entry("gold", Material.GOLD_INGOT),
            Map.entry("emerald", Material.EMERALD),
            Map.entry("diamond", Material.DIAMOND),
            Map.entry("lapis", Material.LAPIS_LAZULI),
            Map.entry("amethyst", Material.AMETHYST_SHARD),
            Map.entry("resin", Material.RESIN_BRICK));

    private final WorldCupArmorPlugin plugin;
    private final String team;
    private final EquipmentSlot target;
    private final Inventory inventory;
    private final Map<Integer, TrimPattern> patternSlots = new HashMap<>();
    private final Map<Integer, TrimMaterial> materialSlots = new HashMap<>();

    public TrimMenu(WorldCupArmorPlugin plugin, String team, EquipmentSlot target) {
        this.plugin = plugin;
        this.team = team;
        this.target = target;
        String piece = target == null ? "all pieces" : PieceMenu.pieceName(target).toLowerCase(Locale.ROOT);
        this.inventory = Bukkit.createInventory(this, 54,
                Component.text("Trim " + piece + ": " + team, NamedTextColor.DARK_GRAY));
        build();
    }

    private ArmorStyle currentStyle() {
        ArmorStyle style = plugin.colors().style(team, target == null ? EquipmentSlot.CHEST : target);
        return style == null ? ArmorStyle.EMPTY : style;
    }

    private static String prettify(String key) {
        String lower = key.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void build() {
        ArmorStyle current = currentStyle();

        List<TrimPattern> patterns = new ArrayList<>();
        RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).forEach(patterns::add);
        patterns.sort(Comparator.comparing(pattern -> pattern.getKey().getKey()));
        int slot = 0;
        for (TrimPattern pattern : patterns) {
            if (slot >= 27) {
                break;
            }
            String key = pattern.getKey().getKey();
            Material icon = Material.matchMaterial(key + "_armor_trim_smithing_template");
            boolean selected = current.pattern() != null && current.pattern().getKey().equals(pattern.getKey());
            inventory.setItem(slot, GuiItems.selected(GuiItems.item(icon == null ? Material.PAPER : icon,
                    Component.text(prettify(key), selected ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    List.of(Component.text(selected ? "Currently selected pattern" : "Click to use this pattern",
                            selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW))), selected));
            patternSlots.put(slot, pattern);
            slot++;
        }

        List<TrimMaterial> materials = new ArrayList<>();
        RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).forEach(materials::add);
        materials.sort(Comparator.comparing(material -> material.getKey().getKey()));
        slot = 27;
        for (TrimMaterial material : materials) {
            if (slot >= 45) {
                break;
            }
            String key = material.getKey().getKey();
            Material icon = MATERIAL_ICONS.getOrDefault(key, Material.PAPER);
            boolean selected = current.material() != null && current.material().getKey().equals(material.getKey());
            inventory.setItem(slot, GuiItems.selected(GuiItems.item(icon,
                    Component.text(prettify(key), selected ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    List.of(Component.text(selected ? "Currently selected material" : "Click to use this material",
                            selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW))), selected));
            materialSlots.put(slot, material);
            slot++;
        }

        inventory.setItem(45, GuiItems.item(Material.ARROW,
                Component.text("Back", NamedTextColor.WHITE), List.of()));
        if (current.hasTrim()) {
            inventory.setItem(49, GuiItems.styled(Material.LEATHER_CHESTPLATE, current,
                    Component.text("Current trim: " + prettify(current.pattern().getKey().getKey())
                            + " / " + prettify(current.material().getKey().getKey()), NamedTextColor.AQUA),
                    List.of(Component.text("Pick a pattern and a material above", NamedTextColor.GRAY))));
        } else {
            inventory.setItem(49, GuiItems.item(Material.GRAY_DYE,
                    Component.text("No trim set", NamedTextColor.GRAY),
                    List.of(Component.text("Pick a pattern or a material to start", NamedTextColor.YELLOW))));
        }
        inventory.setItem(53, GuiItems.item(Material.BARRIER,
                Component.text("Remove trim", NamedTextColor.RED),
                List.of(Component.text("The armor will show without a trim", NamedTextColor.GRAY))));
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
        TrimPattern pattern = patternSlots.get(slot);
        if (pattern != null) {
            ArmorStyle current = currentStyle();
            apply(player, pattern, current.material() == null ? DEFAULT_MATERIAL : current.material());
            return;
        }
        TrimMaterial material = materialSlots.get(slot);
        if (material != null) {
            ArmorStyle current = currentStyle();
            apply(player, current.pattern() == null ? DEFAULT_PATTERN : current.pattern(), material);
            return;
        }
        switch (slot) {
            case 45 -> new PieceMenu(plugin, team).open(player);
            case 53 -> {
                if (target == null) {
                    plugin.colors().removeTrimAll(team);
                } else {
                    plugin.colors().removeTrim(team, target);
                }
                plugin.disguises().refreshTeam(team);
                player.sendMessage(Component.text("Removed the trim.", NamedTextColor.RED));
                new TrimMenu(plugin, team, target).open(player);
            }
            default -> {
            }
        }
    }

    private void apply(Player player, TrimPattern pattern, TrimMaterial material) {
        if (target == null) {
            plugin.colors().setTrimAll(team, pattern, material);
        } else {
            plugin.colors().setTrim(team, target, pattern, material);
        }
        plugin.disguises().refreshTeam(team);
        player.sendMessage(Component.text("Set the trim to " + prettify(pattern.getKey().getKey())
                + " with " + prettify(material.getKey().getKey()) + ".", NamedTextColor.GREEN));
        new TrimMenu(plugin, team, target).open(player);
    }
}
