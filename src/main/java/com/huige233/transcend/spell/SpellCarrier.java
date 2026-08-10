package com.huige233.transcend.spell;

import org.jetbrains.annotations.Nullable;

/** 法术承载器（杖/卷/书）枚举。 */
public enum SpellCarrier {

    ORB("orb", 4, 0.8F, 3.0, 30),
    ARROW("arrow", 8, 0.0F, 0.0, 15),
    BEAM("beam", 0, 0.0F, 0.0, 25),
    SLASH("slash", 0, 0.0F, 2.5, 20),
    NOVA("nova", 0, 0.0F, 5.0, 40),
    VORTEX("vortex", 3, 0.5F, 4.0, 45),
    TRAP("trap", 5, 1.0F, 2.0, 30),
    CHAIN("chain", 0, 0.0F, 8.0, 35),
    RAIN("rain", 0, 0.0F, 8.0, 50),
    DASH("dash", 0, 0.0F, 1.0, 25),
    BARRIER("barrier", 0, 0.0F, 3.0, 60);

    public final String id;
    private final int defaultProjectileSpeed;
    private final float defaultGravity;
    private final double defaultAoeRadius;
    private final int defaultBaseCooldown;

    SpellCarrier(String id, int projectileSpeed, float gravity, double aoeRadius, int baseCooldown) {
        this.id = id;
        this.defaultProjectileSpeed = projectileSpeed;
        this.defaultGravity = gravity;
        this.defaultAoeRadius = aoeRadius;
        this.defaultBaseCooldown = baseCooldown;
    }

    @Nullable
    public static SpellCarrier getById(String id) {
        if (id == null || id.isEmpty()) return null;
        return switch (id) {
            case "orb", "summon" -> ORB;
            case "arrow" -> ARROW;
            case "beam", "breath" -> BEAM;
            case "slash" -> SLASH;
            case "nova", "ring", "ground" -> NOVA;
            case "vortex" -> VORTEX;
            case "trap", "spike" -> TRAP;
            case "chain" -> CHAIN;
            case "rain" -> RAIN;
            case "dash", "teleport" -> DASH;
            case "barrier" -> BARRIER;
            default -> null;
        };
    }

    public String getDisplayKey() {
        return "spell.carrier." + id;
    }

    public int getProjectileSpeed() {
        return com.huige233.transcend.spell.data.CarrierStatsRegistry.getInstance().get(this).projectileSpeed();
    }

    public float getGravity() {
        return com.huige233.transcend.spell.data.CarrierStatsRegistry.getInstance().get(this).gravity();
    }

    public double getAoeRadius() {
        return com.huige233.transcend.spell.data.CarrierStatsRegistry.getInstance().get(this).aoeRadius();
    }

    public int getBaseCooldown() {
        return com.huige233.transcend.spell.data.CarrierStatsRegistry.getInstance().get(this).baseCooldown();
    }

    public int getDefaultProjectileSpeed() { return defaultProjectileSpeed; }
    public float getDefaultGravity() { return defaultGravity; }
    public double getDefaultAoeRadius() { return defaultAoeRadius; }
    public int getDefaultBaseCooldown() { return defaultBaseCooldown; }
}
