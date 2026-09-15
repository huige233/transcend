package com.huige233.transcend.tech.quality;

import java.util.Locale;
import java.util.Random;

   
                          
                               
   
/** 定义装备品质的显示颜色和词缀槽位倍率，并支持品质升降及旧版标识解析。 */
public enum TechQuality {
    COMMON(1.00F, 0xFFFFFF),
    UNCOMMON(1.15F, 0x55FF55),
    RARE(1.30F, 0x5555FF),
    EPIC(1.50F, 0xAA55FF),
    LEGENDARY(1.75F, 0xFFAA00),
    MYTHIC(2.00F, 0xFF8800);

    public final float slotMul;
    public final int color;

    TechQuality(float slotMul, int color) { this.slotMul = slotMul; this.color = color; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public int level() { return ordinal(); }
    public boolean atLeast(TechQuality other) { return compareTo(other == null ? COMMON : other) >= 0; }
    public TechQuality next() { return this == MYTHIC ? MYTHIC : values()[ordinal() + 1]; }
    public TechQuality previous() { return this == COMMON ? COMMON : values()[ordinal() - 1]; }

    
    public int slots(int baseSlots) { return Math.max(0, Math.round(Math.max(0, baseSlots) * slotMul)); }

    public static TechQuality byId(String id) {
        if (id == null || id.isBlank()) return COMMON;
        String normalized = id.trim().toUpperCase(Locale.ROOT);
        
        normalized = switch (normalized) {
            case "WHITE", "GRAY", "GREY", "NORMAL" -> "COMMON";
            case "GREEN" -> "UNCOMMON";
            case "BLUE" -> "RARE";
            case "PURPLE" -> "EPIC";
            case "GOLD", "YELLOW" -> "LEGENDARY";
            case "ORANGE", "RED", "COSMIC" -> "MYTHIC";
            default -> normalized;
        };
        try { return valueOf(normalized); }
        catch (IllegalArgumentException ignored) { return COMMON; }
    }

    public static TechQuality byLevel(int level) {
        return values()[Math.max(0, Math.min(values().length - 1, level))];
    }

    
    public static TechQuality random(Random random) {
        return values()[(random == null ? new Random() : random).nextInt(values().length)];
    }
}
