package dev.comfi.worldcuparmor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds the glowing flag to entity metadata sent to spectators for players on
 * a team. Only packets that already carry the shared flags byte are touched;
 * {@link GlowService} re-sends that byte every second to cover the rest.
 */
public final class GlowListener extends PacketAdapter {

    private final WorldCupArmorPlugin plugin;

    public GlowListener(WorldCupArmorPlugin plugin) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.ENTITY_METADATA);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (!plugin.colors().isEnabled()) {
            return;
        }
        Player viewer = event.getPlayer();
        if (viewer.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);
        if (entityId == viewer.getEntityId() || !plugin.glow().shouldGlow(entityId)) {
            return;
        }
        List<WrappedDataValue> values = packet.getDataValueCollectionModifier().read(0);
        if (values == null) {
            return;
        }
        List<WrappedDataValue> rewritten = new ArrayList<>(values.size());
        boolean changed = false;
        for (WrappedDataValue value : values) {
            if (value.getIndex() == 0 && value.getValue() instanceof Byte flags
                    && (flags & GlowService.GLOWING_BIT) == 0) {
                rewritten.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class),
                        (byte) (flags | GlowService.GLOWING_BIT)));
                changed = true;
            } else {
                rewritten.add(value);
            }
        }
        if (!changed) {
            return;
        }
        PacketContainer clone = packet.shallowClone();
        clone.getDataValueCollectionModifier().write(0, rewritten);
        event.setPacket(clone);
    }
}
