package dev.comfi.worldcuparmor.gui;

import dev.comfi.worldcuparmor.ArmorStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

final class GuiItems {

    private GuiItems() {
    }

    static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>(lore.size());
        for (Component line : lore) {
            lines.add(line.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack styled(Material material, ArmorStyle style, Component name, List<Component> lore) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (style.color() != null && meta instanceof LeatherArmorMeta leather) {
            leather.setColor(style.color());
        }
        if (style.hasTrim() && meta instanceof ArmorMeta armor) {
            armor.setTrim(style.trim());
        }
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack selected(ItemStack item, boolean selected) {
        if (selected) {
            ItemMeta meta = item.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
