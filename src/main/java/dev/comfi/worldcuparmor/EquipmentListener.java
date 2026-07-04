package dev.comfi.worldcuparmor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EquipmentListener extends PacketAdapter {

    private final WorldCupArmorPlugin plugin;

    public EquipmentListener(WorldCupArmorPlugin plugin) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.ENTITY_EQUIPMENT);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (!plugin.colors().isEnabled()) {
            return;
        }
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);
        if (event.getPlayer().getEntityId() == entityId) {
            return;
        }
        Map<EquipmentSlot, ArmorStyle> pieces = plugin.disguises().stylesFor(entityId);
        if (pieces == null) {
            return;
        }
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = packet.getSlotStackPairLists().read(0);
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> rewritten = new ArrayList<>(pairs.size());
        boolean changed = false;
        for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : pairs) {
            ArmorStyle style = styleFor(pieces, pair.getFirst());
            ItemStack disguised = style == null ? null : DisguiseService.disguiseItem(pair.getSecond(), style);
            if (disguised == null) {
                rewritten.add(pair);
            } else {
                rewritten.add(new Pair<>(pair.getFirst(), disguised));
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        PacketContainer clone = packet.shallowClone();
        clone.getSlotStackPairLists().write(0, rewritten);
        event.setPacket(clone);
    }

    private ArmorStyle styleFor(Map<EquipmentSlot, ArmorStyle> pieces, EnumWrappers.ItemSlot slot) {
        return switch (slot) {
            case HEAD -> pieces.get(EquipmentSlot.HEAD);
            case CHEST -> pieces.get(EquipmentSlot.CHEST);
            case LEGS -> pieces.get(EquipmentSlot.LEGS);
            case FEET -> pieces.get(EquipmentSlot.FEET);
            default -> null;
        };
    }

}
