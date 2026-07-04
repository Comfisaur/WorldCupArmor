package dev.comfi.worldcuparmor;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Country flag sprites from the CartCup resource pack, applied as scoreboard
 * team prefixes built from the MiniMessage format in the config.
 */
public final class FlagService {

    // The client only stitches its built in atlases, so the pack injects the
    // flag textures into minecraft:gui instead of defining its own atlas.
    private static final Key ATLAS = Key.key("minecraft", "gui");
    private static final String DEFAULT_FORMAT = "<flag> ";

    private final WorldCupArmorPlugin plugin;
    private final List<String> available = new ArrayList<>();
    // Team prefix as the scoreboard reports it back after we set it, in JSON
    // form, so the ensure task can tell when something else changed it.
    private final Map<String, String> appliedPrefixes = new ConcurrentHashMap<>();
    private BukkitTask task;

    public FlagService(WorldCupArmorPlugin plugin) {
        this.plugin = plugin;
        try (InputStream in = plugin.getResource("flags.txt")) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        available.add(line.trim());
                    }
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not read flags.txt: " + ex.getMessage());
        }
    }

    public List<String> available() {
        return List.copyOf(available);
    }

    public boolean exists(String flag) {
        return flag != null && available.contains(flag.toLowerCase(Locale.ROOT));
    }

    public static String pretty(String flag) {
        String[] words = flag.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    public Component sprite(String flag) {
        try {
            // Explicit white, otherwise the sprite inherits the team color
            // (or any surrounding color) and gets tinted by it.
            return Component.object(ObjectContents.sprite(ATLAS, Key.key("cartcup", "flag/" + flag)))
                    .color(NamedTextColor.WHITE);
        } catch (Exception ex) {
            return Component.empty();
        }
    }

    /**
     * MiniMessage tag for flag sprites: {@code <flag>} inserts the given
     * default flag, {@code <flag:brazil>} inserts a specific one.
     */
    public TagResolver flagTag(String defaultFlag) {
        return TagResolver.resolver("flag", (args, context) -> {
            String name = args.hasNext() ? args.pop().value() : defaultFlag;
            return Tag.selfClosingInserting(name == null ? Component.empty() : sprite(name));
        });
    }

    private Component prefix(String flag) {
        String format = plugin.getConfig().getString("flag-prefix", DEFAULT_FORMAT);
        try {
            return MiniMessage.miniMessage().deserialize(format, flagTag(flag));
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid flag-prefix format: " + ex.getMessage());
            return sprite(flag).append(Component.space());
        }
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::ensureAll, 40L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        appliedPrefixes.clear();
    }

    /** Sets or clears the scoreboard team prefix to match the configured flag. */
    public void apply(String teamName) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
        if (team == null) {
            appliedPrefixes.remove(teamName);
            return;
        }
        String flag = plugin.colors().flag(teamName);
        if (flag == null) {
            team.prefix(Component.empty());
            appliedPrefixes.remove(teamName);
        } else {
            team.prefix(prefix(flag));
            appliedPrefixes.put(teamName, GsonComponentSerializer.gson().serialize(team.prefix()));
        }
    }

    public void applyAll() {
        for (String teamName : plugin.colors().configuredTeams()) {
            apply(teamName);
        }
    }

    /**
     * Re-applies prefixes that drifted from what this plugin set, so flags
     * survive teams being recreated or other plugins overwriting the prefix.
     */
    private void ensureAll() {
        for (String teamName : plugin.colors().configuredTeams()) {
            if (plugin.colors().flag(teamName) == null) {
                continue;
            }
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
            if (team == null) {
                appliedPrefixes.remove(teamName);
                continue;
            }
            String current = GsonComponentSerializer.gson().serialize(team.prefix());
            if (!current.equals(appliedPrefixes.get(teamName))) {
                apply(teamName);
            }
        }
    }
}
