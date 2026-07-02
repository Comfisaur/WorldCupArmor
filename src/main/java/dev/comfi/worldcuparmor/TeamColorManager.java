package dev.comfi.worldcuparmor;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.EquipmentSlot;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamColorManager {

    public static final Set<EquipmentSlot> PIECES =
            Set.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private final WorldCupArmorPlugin plugin;
    private final Map<String, Map<EquipmentSlot, Color>> teamColors = new ConcurrentHashMap<>();
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

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        config.options().pathSeparator('\u0001');
        enabled = config.getBoolean("enabled", true);
        teamColors.clear();
        ConfigurationSection teams = config.getConfigurationSection("teams");
        if (teams == null) {
            return;
        }
        for (String team : teams.getKeys(false)) {
            Map<EquipmentSlot, Color> pieces = new EnumMap<>(EquipmentSlot.class);
            if (teams.isString(team)) {
                Color all = parse(teams.getString(team));
                if (all != null) {
                    for (EquipmentSlot slot : PIECES) {
                        pieces.put(slot, all);
                    }
                }
            } else {
                ConfigurationSection section = teams.getConfigurationSection(team);
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        EquipmentSlot slot = slotFromKey(key);
                        Color color = parse(section.getString(key));
                        if (slot != null && color != null) {
                            pieces.put(slot, color);
                        } else {
                            plugin.getLogger().warning("Invalid entry for team " + team + ": " + key);
                        }
                    }
                }
            }
            if (!pieces.isEmpty()) {
                teamColors.put(team, pieces);
            }
        }
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.options().pathSeparator('\u0001');
        config.set("enabled", enabled);
        ConfigurationSection teams = config.createSection("teams");
        for (Map.Entry<String, Map<EquipmentSlot, Color>> team : teamColors.entrySet()) {
            ConfigurationSection section = teams.createSection(team.getKey());
            for (Map.Entry<EquipmentSlot, Color> piece : team.getValue().entrySet()) {
                section.set(key(piece.getKey()), String.format("%06X", piece.getValue().asRGB()));
            }
        }
        plugin.saveConfig();
    }

    public Map<EquipmentSlot, Color> pieces(String team) {
        if (team == null) {
            return Map.of();
        }
        Map<EquipmentSlot, Color> pieces = teamColors.get(team);
        return pieces == null ? Map.of() : Map.copyOf(pieces);
    }

    public Color color(String team, EquipmentSlot slot) {
        return pieces(team).get(slot);
    }

    public void setColor(String team, EquipmentSlot slot, Color color) {
        teamColors.computeIfAbsent(team, k -> new ConcurrentHashMap<>()).put(slot, color);
        save();
    }

    public void setAll(String team, Color color) {
        Map<EquipmentSlot, Color> pieces = teamColors.computeIfAbsent(team, k -> new ConcurrentHashMap<>());
        for (EquipmentSlot slot : PIECES) {
            pieces.put(slot, color);
        }
        save();
    }

    public void removePiece(String team, EquipmentSlot slot) {
        Map<EquipmentSlot, Color> pieces = teamColors.get(team);
        if (pieces == null) {
            return;
        }
        pieces.remove(slot);
        if (pieces.isEmpty()) {
            teamColors.remove(team);
        }
        save();
    }

    public void removeTeam(String team) {
        teamColors.remove(team);
        save();
    }

    public Set<String> configuredTeams() {
        return Set.copyOf(teamColors.keySet());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
        save();
    }
}
