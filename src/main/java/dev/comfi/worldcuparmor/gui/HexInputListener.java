package dev.comfi.worldcuparmor.gui;

import dev.comfi.worldcuparmor.WorldCupArmorPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HexInputListener implements Listener {

    private record Pending(String team, EquipmentSlot target) {
    }

    private final WorldCupArmorPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public HexInputListener(WorldCupArmorPlugin plugin) {
        this.plugin = plugin;
    }

    public void await(Player player, String team, EquipmentSlot target) {
        pending.put(player.getUniqueId(), new Pending(team, target));
        player.sendMessage(Component.text("Type a hex color in chat, for example #1E90FF, or type cancel.", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Pending request = pending.remove(event.getPlayer().getUniqueId());
        if (request == null) {
            return;
        }
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(Component.text("Cancelled.", NamedTextColor.RED));
                new PieceMenu(plugin, request.team()).open(player);
                return;
            }
            String hex = input.startsWith("#") ? input.substring(1) : input;
            if (!hex.matches("[0-9a-fA-F]{6}")) {
                pending.put(player.getUniqueId(), request);
                player.sendMessage(Component.text("Invalid hex code, expected six hex digits like #FF8800. Try again or type cancel.", NamedTextColor.RED));
                return;
            }
            Color color = Color.fromRGB(Integer.parseInt(hex, 16));
            if (request.target() == null) {
                plugin.colors().setAll(request.team(), color);
            } else {
                plugin.colors().setColor(request.team(), request.target(), color);
            }
            plugin.disguises().refreshTeam(request.team());
            player.sendMessage(Component.text("Set " + request.team() + " to #" + hex.toUpperCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
            new PieceMenu(plugin, request.team()).open(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
