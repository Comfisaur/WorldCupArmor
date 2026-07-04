package dev.comfi.worldcuparmor;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ColorableArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DisguiseService implements Listener {

    private static final List<EquipmentSlot> ARMOR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private final WorldCupArmorPlugin plugin;
    private final TeamColorManager colors;
    private final Map<Integer, Map<EquipmentSlot, ArmorStyle>> entityStyles = new ConcurrentHashMap<>();
    private BukkitTask task;

    public DisguiseService(WorldCupArmorPlugin plugin, TeamColorManager colors) {
        this.plugin = plugin;
        this.colors = colors;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        entityStyles.clear();
    }

    public Map<EquipmentSlot, ArmorStyle> stylesFor(int entityId) {
        return entityStyles.get(entityId);
    }

    private Map<EquipmentSlot, ArmorStyle> computeStyles(Player player) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        return team == null ? Map.of() : colors.pieces(team.getName());
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<EquipmentSlot, ArmorStyle> current = computeStyles(player);
            Map<EquipmentSlot, ArmorStyle> previous = entityStyles.get(player.getEntityId());
            if (!Objects.equals(current.isEmpty() ? null : current, previous)) {
                refresh(player);
            }
        }
    }

    /**
     * Builds the item other players should see for this piece, or null when
     * the real item is not disguisable. Used both here and for rewriting the
     * natural equipment packets in {@link EquipmentListener}.
     */
    static ItemStack disguiseItem(ItemStack item, ArmorStyle style) {
        if (item == null || style.color() == null) {
            return null;
        }
        Material leather = switch (item.getType()) {
            case NETHERITE_HELMET -> Material.LEATHER_HELMET;
            case NETHERITE_CHESTPLATE -> Material.LEATHER_CHESTPLATE;
            case NETHERITE_LEGGINGS -> Material.LEATHER_LEGGINGS;
            case NETHERITE_BOOTS -> Material.LEATHER_BOOTS;
            default -> null;
        };
        if (leather == null) {
            return null;
        }
        ItemStack fake = new ItemStack(leather);
        ColorableArmorMeta meta = (ColorableArmorMeta) fake.getItemMeta();
        meta.setColor(style.color());
        if (style.hasTrim()) {
            meta.setTrim(style.trim());
        }
        meta.addItemFlags(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM);
        if (!item.getEnchantments().isEmpty()) {
            meta.setEnchantmentGlintOverride(true);
        }
        fake.setItemMeta(meta);
        return fake;
    }

    public void refresh(Player player) {
        Map<EquipmentSlot, ArmorStyle> pieces = computeStyles(player);
        if (pieces.isEmpty()) {
            entityStyles.remove(player.getEntityId());
        } else {
            entityStyles.put(player.getEntityId(), pieces);
        }
        // Send the disguised items directly instead of counting on the packet
        // listener to rewrite them, so viewers update without relogging.
        boolean active = colors.isEnabled();
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);
            ItemStack shown = item == null ? new ItemStack(Material.AIR) : item.clone();
            ArmorStyle style = pieces.get(slot);
            if (active && style != null) {
                ItemStack disguised = disguiseItem(shown, style);
                if (disguised != null) {
                    shown = disguised;
                }
            }
            equipment.put(slot, shown);
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) {
                continue;
            }
            if (!viewer.getWorld().equals(player.getWorld())) {
                continue;
            }
            if (!viewer.canSee(player)) {
                continue;
            }
            viewer.sendEquipmentChange(player, equipment);
        }
    }

    public void refreshTeam(String teamName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
            if (team != null && team.getName().equals(teamName)) {
                refresh(player);
            }
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Map<EquipmentSlot, ArmorStyle> pieces = computeStyles(event.getPlayer());
        if (!pieces.isEmpty()) {
            entityStyles.put(event.getPlayer().getEntityId(), pieces);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        entityStyles.remove(event.getPlayer().getEntityId());
    }
}
