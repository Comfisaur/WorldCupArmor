package dev.comfi.worldcuparmor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players should glow for spectators and re-sends their entity
 * flags when a viewer switches game mode. The glow bit itself is injected per
 * viewer by {@link GlowListener}.
 */
public final class GlowService implements Listener {

    private static final int FLAGS_INDEX = 0;
    static final byte GLOWING_BIT = 0x40;

    private final WorldCupArmorPlugin plugin;
    private final Set<Integer> glowing = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public GlowService(WorldCupArmorPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getGameMode() == GameMode.SPECTATOR) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.equals(viewer) && target.getWorld().equals(viewer.getWorld())) {
                        sendFlags(viewer, target, baseFlags(target));
                    }
                }
            }
        }
        glowing.clear();
    }

    public boolean shouldGlow(int entityId) {
        return glowing.contains(entityId);
    }

    private boolean isOnTeam(Player player) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        return team != null;
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isOnTeam(player)) {
                glowing.add(player.getEntityId());
            } else {
                glowing.remove(player.getEntityId());
            }
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getGameMode() == GameMode.SPECTATOR) {
                refreshViewer(viewer);
            }
        }
    }

    /**
     * Re-sends every other player's flags byte to this viewer. The packet runs
     * through {@link GlowListener}, so this both applies and clears the glow.
     */
    public void refreshViewer(Player viewer) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer) || !target.getWorld().equals(viewer.getWorld())) {
                continue;
            }
            sendFlags(viewer, target, baseFlags(target));
        }
    }

    private byte baseFlags(Player player) {
        byte flags = 0;
        if (player.getFireTicks() > 0) {
            flags |= 0x01;
        }
        if (player.isSneaking()) {
            flags |= 0x02;
        }
        if (player.isSprinting()) {
            flags |= 0x08;
        }
        if (player.isSwimming()) {
            flags |= 0x10;
        }
        if (player.isInvisible()) {
            flags |= 0x20;
        }
        if (player.isGlowing()) {
            flags |= GLOWING_BIT;
        }
        if (player.isGliding()) {
            flags |= (byte) 0x80;
        }
        return flags;
    }

    private void sendFlags(Player viewer, Player target, byte flags) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());
        packet.getDataValueCollectionModifier().write(0, List.of(
                new WrappedDataValue(FLAGS_INDEX, WrappedDataWatcher.Registry.get(Byte.class), flags)));
        ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                refreshViewer(player);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isOnTeam(player)) {
            glowing.add(player.getEntityId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        glowing.remove(event.getPlayer().getEntityId());
    }
}
