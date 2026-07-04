package dev.comfi.worldcuparmor;

import org.bukkit.Color;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

public record ArmorStyle(Color color, TrimPattern pattern, TrimMaterial material) {

    public static final ArmorStyle EMPTY = new ArmorStyle(null, null, null);

    public boolean hasTrim() {
        return pattern != null && material != null;
    }

    public boolean isEmpty() {
        return color == null && !hasTrim();
    }

    public ArmorTrim trim() {
        return hasTrim() ? new ArmorTrim(material, pattern) : null;
    }

    public ArmorStyle withColor(Color newColor) {
        return new ArmorStyle(newColor, pattern, material);
    }

    public ArmorStyle withTrim(TrimPattern newPattern, TrimMaterial newMaterial) {
        return new ArmorStyle(color, newPattern, newMaterial);
    }

    public ArmorStyle withoutTrim() {
        return new ArmorStyle(color, null, null);
    }
}
