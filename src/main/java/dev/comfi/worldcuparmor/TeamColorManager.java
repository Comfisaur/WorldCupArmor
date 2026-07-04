package dev.comfi.worldcuparmor;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamColorManager {

    public static final Set<EquipmentSlot> PIECES =
            Set.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private final WorldCupArmorPlugin plugin;
    private final Map<String, Map<EquipmentSlot, ArmorStyle>> teamStyles = new ConcurrentHashMap<>();
    private final Map<String, String> teamFlags = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;

    public TeamColorManager(WorldCupArmorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String key(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> throw new IllegalArgumentException(slot.name());
        };
    }

    private static EquipmentSlot slotFromKey(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "helmet" -> EquipmentSlot.HEAD;
            case "chestplate" -> EquipmentSlot.CHEST;
            case "leggings" -> EquipmentSlot.LEGS;
            case "boots" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static Color parse(String hex) {
        if (hex == null) {
            return null;
        }
        try {
            return Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static TrimPattern patternByName(String name) {
        if (name == null) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        return key == null ? null
                : RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).get(key);
    }

    public static TrimMaterial materialByName(String name) {
        if (name == null) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        return key == null ? null
                : RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).get(key);
    }

    private ArmorStyle parseStyle(ConfigurationSection pieces, String key, String team) {
        if (pieces.isString(key)) {
            Color color = parse(pieces.getString(key));
            return color == null ? ArmorStyle.EMPTY : new ArmorStyle(color, null, null);
        }
        ConfigurationSection section = pieces.getConfigurationSection(key);
        if (section == null) {
            return ArmorStyle.EMPTY;
        }
        Color color = parse(section.getString("color"));
        TrimPattern pattern = patternByName(section.getString("trim-pattern"));
        TrimMaterial material = materialByName(section.getString("trim-material"));
        if (section.contains("trim-pattern") && (pattern == null || material == null)) {
            plugin.getLogger().warning("Invalid trim for team " + team + ", piece " + key);
            pattern = null;
            material = null;
        }
        return new ArmorStyle(color, pattern, material);
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        config.options().pathSeparator('\u0001');
        enabled = config.getBoolean("enabled", true);
        teamStyles.clear();
        teamFlags.clear();
        ConfigurationSection teams = config.getConfigurationSection("teams");
        if (teams == null) {
            return;
        }
        for (String team : teams.getKeys(false)) {
            Map<EquipmentSlot, ArmorStyle> pieces = new EnumMap<>(EquipmentSlot.class);
            if (teams.isString(team)) {
                Color all = parse(teams.getString(team));
                if (all != null) {
                    for (EquipmentSlot slot : PIECES) {
                        pieces.put(slot, new ArmorStyle(all, null, null));
                    }
                }
            } else {
                ConfigurationSection section = teams.getConfigurationSection(team);
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        if (key.equalsIgnoreCase("flag")) {
                            String flag = section.getString(key);
                            if (flag != null && !flag.isBlank()) {
                                teamFlags.put(team, flag.toLowerCase(Locale.ROOT));
                            }
                            continue;
                        }
                        EquipmentSlot slot = slotFromKey(key);
                        ArmorStyle style = slot == null ? ArmorStyle.EMPTY : parseStyle(section, key, team);
                        if (slot != null && !style.isEmpty()) {
                            pieces.put(slot, style);
                        } else {
                            plugin.getLogger().warning("Invalid entry for team " + team + ": " + key);
                        }
                    }
                }
            }
            if (!pieces.isEmpty()) {
                teamStyles.put(team, pieces);
            }
        }
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.options().pathSeparator('\u0001');
        config.set("enabled", enabled);
        ConfigurationSection teams = config.createSection("teams");
        for (String team : configuredTeams()) {
            ConfigurationSection section = teams.createSection(team);
            Map<EquipmentSlot, ArmorStyle> styles = teamStyles.get(team);
            if (styles != null) {
                for (Map.Entry<EquipmentSlot, ArmorStyle> piece : styles.entrySet()) {
                    ArmorStyle style = piece.getValue();
                    String hex = style.color() == null ? null : String.format("%06X", style.color().asRGB());
                    if (style.hasTrim()) {
                        ConfigurationSection entry = section.createSection(key(piece.getKey()));
                        entry.set("color", hex);
                        entry.set("trim-pattern", style.pattern().getKey().getKey());
                        entry.set("trim-material", style.material().getKey().getKey());
                    } else {
                        section.set(key(piece.getKey()), hex);
                    }
                }
            }
            String flag = teamFlags.get(team);
            if (flag != null) {
                section.set("flag", flag);
            }
        }
        plugin.saveConfig();
    }

    public Map<EquipmentSlot, ArmorStyle> pieces(String team) {
        if (team == null) {
            return Map.of();
        }
        Map<EquipmentSlot, ArmorStyle> pieces = teamStyles.get(team);
        return pieces == null ? Map.of() : Map.copyOf(pieces);
    }

    public ArmorStyle style(String team, EquipmentSlot slot) {
        return pieces(team).get(slot);
    }

    public Color color(String team, EquipmentSlot slot) {
        ArmorStyle style = style(team, slot);
        return style == null ? null : style.color();
    }

    private void update(String team, EquipmentSlot slot, java.util.function.UnaryOperator<ArmorStyle> change) {
        Map<EquipmentSlot, ArmorStyle> pieces = teamStyles.computeIfAbsent(team, k -> new ConcurrentHashMap<>());
        ArmorStyle updated = change.apply(pieces.getOrDefault(slot, ArmorStyle.EMPTY));
        if (updated.isEmpty()) {
            pieces.remove(slot);
            if (pieces.isEmpty()) {
                teamStyles.remove(team);
            }
        } else {
            pieces.put(slot, updated);
        }
        save();
    }

    public void setColor(String team, EquipmentSlot slot, Color color) {
        update(team, slot, style -> style.withColor(color));
    }

    public void setAll(String team, Color color) {
        for (EquipmentSlot slot : PIECES) {
            update(team, slot, style -> style.withColor(color));
        }
    }

    public void setTrim(String team, EquipmentSlot slot, TrimPattern pattern, TrimMaterial material) {
        update(team, slot, style -> style.withTrim(pattern, material));
    }

    public void setTrimAll(String team, TrimPattern pattern, TrimMaterial material) {
        for (EquipmentSlot slot : PIECES) {
            update(team, slot, style -> style.withTrim(pattern, material));
        }
    }

    public void removeTrim(String team, EquipmentSlot slot) {
        update(team, slot, ArmorStyle::withoutTrim);
    }

    public void removeTrimAll(String team) {
        for (EquipmentSlot slot : PIECES) {
            update(team, slot, ArmorStyle::withoutTrim);
        }
    }

    public void removePiece(String team, EquipmentSlot slot) {
        Map<EquipmentSlot, ArmorStyle> pieces = teamStyles.get(team);
        if (pieces == null) {
            return;
        }
        pieces.remove(slot);
        if (pieces.isEmpty()) {
            teamStyles.remove(team);
        }
        save();
    }

    public void removeTeam(String team) {
        teamStyles.remove(team);
        save();
    }

    public String flag(String team) {
        return team == null ? null : teamFlags.get(team);
    }

    public void setFlag(String team, String flag) {
        teamFlags.put(team, flag.toLowerCase(Locale.ROOT));
        save();
    }

    public void removeFlag(String team) {
        teamFlags.remove(team);
        save();
    }

    public Set<String> configuredTeams() {
        Set<String> names = new java.util.HashSet<>(teamStyles.keySet());
        names.addAll(teamFlags.keySet());
        return Set.copyOf(names);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
        save();
    }
}
