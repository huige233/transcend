package com.huige233.transcend.spell;

public enum SpellCarrier {
    // === 原有 ===
    ORB("orb", 4, 0.8F, 3.0, 30),
    ARROW("arrow", 8, 0.0F, 0.0, 15),
    SLASH("slash", 0, 0.0F, 2.5, 20),
    BEAM("beam", 0, 0.0F, 0.0, 25),
    NOVA("nova", 0, 0.0F, 5.0, 40),
    // === 新增 ===
    CHAIN("chain", 0, 0.0F, 8.0, 35),
    VORTEX("vortex", 3, 0.5F, 4.0, 45),
    SPIKE("spike", 0, 0.0F, 1.5, 20),
    TELEPORT("teleport", 0, 0.0F, 0.0, 50),
    TRAP("trap", 5, 1.0F, 2.0, 30),
    BARRIER("barrier", 0, 0.0F, 3.0, 60),
    SUMMON("summon", 0, 0.0F, 0.0, 55),
    RING("ring", 0, 0.0F, 6.0, 35),
    BREATH("breath", 0, 0.0F, 0.0, 20),
    RAIN("rain", 0, 0.0F, 8.0, 50),
    DASH("dash", 0, 0.0F, 1.0, 25),
    GROUND("ground", 0, 0.0F, 4.0, 30);

    public final String id;
    public final int projectileSpeed;
    public final float gravity;
    public final double aoeRadius;
    public final int baseCooldown;

    SpellCarrier(String id, int projectileSpeed, float gravity, double aoeRadius, int baseCooldown) {
        this.id = id;
        this.projectileSpeed = projectileSpeed;
        this.gravity = gravity;
        this.aoeRadius = aoeRadius;
        this.baseCooldown = baseCooldown;
    }

    public static SpellCarrier getById(String id) {
        for (SpellCarrier carrier : values()) {
            if (carrier.id.equals(id)) return carrier;
        }
        return ORB;
    }

    public String getDisplayKey() {
        return "spell.carrier." + id;
    }

    // === Round 11: 数据驱动覆盖访问器 ===
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
}
