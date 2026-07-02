package dev.comfi.worldcuparmor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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
        Map<EquipmentSlot, Color> pieces = plugin.disguises().colorsFor(entityId);
        if (pieces == null) {
            return;
        }
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = packet.getSlotStackPairLists().read(0);
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> rewritten = new ArrayList<>(pairs.size());
        boolean changed = false;
        for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : pairs) {
            Color color = colorFor(pieces, pair.getFirst());
            ItemStack disguised = color == null ? null : disguise(pair.getSecond(), color);
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

    private Color colorFor(Map<EquipmentSlot, Color> pieces, EnumWrappers.ItemSlot slot) {
        return switch (slot) {
            case HEAD -> pieces.get(EquipmentSlot.HEAD);
            case CHEST -> pieces.get(EquipmentSlot.CHEST);
            case LEGS -> pieces.get(EquipmentSlot.LEGS);
            case FEET -> pieces.get(EquipmentSlot.FEET);
            default -> null;
        };
    }

    private ItemStack disguise(ItemStack item, Color color) {
        if (item == null) {
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
        LeatherArmorMeta meta = (LeatherArmorMeta) fake.getItemMeta();
        meta.setColor(color);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        if (!item.getEnchantments().isEmpty()) {
            meta.setEnchantmentGlintOverride(true);
        }
        fake.setItemMeta(meta);
        return fake;
    }
}
