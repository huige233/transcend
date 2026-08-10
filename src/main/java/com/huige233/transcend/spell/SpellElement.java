package com.huige233.transcend.spell;

import org.jetbrains.annotations.Nullable;

/** 法术元素五相枚举（火/金/木/水/土/混沌）。 */
public enum SpellElement {
    METAL("metal", 5.5F, 3, 0.85F, 0.85F, 0.75F),
    WOOD("wood", 4.0F, 2, 0.25F, 0.75F, 0.25F),
    WATER("water", 4.5F, 3, 0.20F, 0.55F, 1.0F),
    FIRE("fire", 6.0F, 2, 1.0F, 0.3F, 0.0F),
    EARTH("earth", 5.0F, 3, 0.55F, 0.35F, 0.1F),
    CHAOS("chaos", 9.0F, 6, 0.8F, 0.2F, 0.8F);

    public final String id;
    private final float baseDamage;
    private final int manaCost;
    private final float particleR, particleG, particleB;
    SpellElement(String id, float baseDamage, int manaCost, float particleR, float particleG, float particleB) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.manaCost = manaCost;
        this.particleR = particleR;
        this.particleG = particleG;
        this.particleB = particleB;
    }

    @Nullable
    public static SpellElement getById(String id) {
        if (id == null || id.isEmpty()) return null;
        return switch (id) {
            case "metal", "holy", "arcane", "light", "sonic" -> METAL;
            case "wood", "wind", "nature", "blood" -> WOOD;
            case "water", "ice" -> WATER;
            case "fire", "thunder", "lightning" -> FIRE;
            case "earth", "poison", "dark", "acid" -> EARTH;
            case "chaos", "void", "time", "space", "eldritch" -> CHAOS;
            default -> null;
        };
    }

    public SpellElement canonical() {
        return this;
    }

    public boolean isCanonical() {
        return this == canonical();
    }

    public static SpellElement[] canonicalValues() {
        return new SpellElement[] {METAL, WOOD, WATER, FIRE, EARTH, CHAOS};
    }

    public String getDisplayKey() {
        return "spell.element." + id;
    }

    public float getBaseDamage() {
        return com.huige233.transcend.spell.data.ElementStatsRegistry.getInstance().get(this).baseDamage();
    }

    public int getManaCost() {
        return com.huige233.transcend.spell.data.ElementStatsRegistry.getInstance().get(this).manaCost();
    }

    public float getParticleR() {
        return com.huige233.transcend.spell.data.ElementStatsRegistry.getInstance().get(this).particleR();
    }

    public float getParticleG() {
        return com.huige233.transcend.spell.data.ElementStatsRegistry.getInstance().get(this).particleG();
    }

    public float getParticleB() {
        return com.huige233.transcend.spell.data.ElementStatsRegistry.getInstance().get(this).particleB();
    }

    public float getDefaultBaseDamage() { return baseDamage; }
    public int getDefaultManaCost() { return manaCost; }
    public float getDefaultParticleR() { return particleR; }
    public float getDefaultParticleG() { return particleG; }
    public float getDefaultParticleB() { return particleB; }
}
