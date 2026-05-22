package com.huige233.transcend.spell;

public enum SpellElement {
    // === 原有 ===
    FIRE("fire", 6.0F, 2, 1.0F, 0.3F, 0.0F),
    ICE("ice", 4.0F, 3, 0.5F, 0.8F, 1.0F),
    THUNDER("thunder", 8.0F, 4, 1.0F, 1.0F, 0.3F),
    WIND("wind", 3.0F, 2, 0.3F, 0.9F, 1.0F),
    EARTH("earth", 5.0F, 3, 0.55F, 0.35F, 0.1F),
    VOID("void", 7.0F, 5, 0.15F, 0.0F, 0.2F),
    HOLY("holy", 4.0F, 3, 1.0F, 0.9F, 0.3F),
    // === 新增 ===
    BLOOD("blood", 5.0F, 3, 0.7F, 0.0F, 0.0F),
    DARK("dark", 6.0F, 4, 0.2F, 0.0F, 0.15F),
    LIGHT("light", 4.0F, 3, 1.0F, 1.0F, 0.8F),
    POISON("poison", 3.0F, 2, 0.3F, 0.7F, 0.1F),
    TIME("time", 2.0F, 5, 0.9F, 0.8F, 0.2F),
    SPACE("space", 4.0F, 5, 0.5F, 0.2F, 0.8F),
    NATURE("nature", 3.0F, 2, 0.2F, 0.8F, 0.3F),
    CHAOS("chaos", 9.0F, 6, 0.8F, 0.2F, 0.8F),
    ACID("acid", 3.0F, 3, 0.4F, 0.8F, 0.1F),
    SONIC("sonic", 5.0F, 4, 0.9F, 0.9F, 0.9F),
    ELDRITCH("eldritch", 8.0F, 5, 0.6F, 0.0F, 0.5F);

    public final String id;
    public final float baseDamage;
    public final int manaCost;
    public final float particleR, particleG, particleB;

    SpellElement(String id, float baseDamage, int manaCost, float particleR, float particleG, float particleB) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.manaCost = manaCost;
        this.particleR = particleR;
        this.particleG = particleG;
        this.particleB = particleB;
    }

    public static SpellElement getById(String id) {
        for (SpellElement element : values()) {
            if (element.id.equals(id)) return element;
        }
        return FIRE;
    }

    public String getDisplayKey() {
        return "spell.element." + id;
    }

    // === Round 10: 数据驱动覆盖访问器 ===
    // 永不返回 null；JSON 未定义该元素则返回 enum 默认。
    // 旧代码仍可直接访问 public final fields（不读覆盖）；新代码请用这些 getter。

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
}
