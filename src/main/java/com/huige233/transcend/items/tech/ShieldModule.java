package com.huige233.transcend.items.tech;

import com.huige233.transcend.tech.rank.TechRank;


/** 定义可替换护盾模块的容量、耗能倍率、分类减伤、亲和增容和科技等级。 */
public enum ShieldModule {
    BASIC("basic", 100.0F, 1.0F, -1, 0.0F, 1.0F, 1, TechRank.B),
    REINFORCED("reinforced", 150.0F, 1.15F, -1, 0.0F, 1.0F, 1, TechRank.B),
    EFFICIENT("efficient", 100.0F, 0.75F, -1, 0.0F, 1.0F, 1, TechRank.B),
    
    T5_MECHANICAL("t5_mechanical", 15_000.0F, 1.0F, 0, 0.85F, 2.5F, 5, TechRank.A),
    B_MELEE("b_melee", 2_400.0F, 0.90F, 0, 0.55F, 1.25F, 4, TechRank.B),
    B_PROJECTILE("b_projectile", 2_400.0F, 0.90F, 1, 0.55F, 1.25F, 4, TechRank.B),
    B_EXPLOSION("b_explosion", 2_400.0F, 0.90F, 2, 0.55F, 1.25F, 4, TechRank.B),
    B_FIRE("b_fire", 2_400.0F, 0.90F, 3, 0.55F, 1.25F, 4, TechRank.B),
    B_MAGIC("b_magic", 2_400.0F, 0.90F, 4, 0.55F, 1.25F, 4, TechRank.B),
    B_ENVIRONMENT("b_environment", 2_400.0F, 0.90F, 5, 0.55F, 1.25F, 4, TechRank.B),
    A_OMNI("a_omni", 6_000.0F, 0.82F, -1, 0.25F, 1.5F, 6, TechRank.A),
    S_OMNI("s_omni", 14_000.0F, 0.72F, -1, 0.45F, 1.8F, 6, TechRank.S),
    EX_OMNI("ex_omni", 30_000.0F, 0.62F, -1, 0.60F, 2.0F, 7, TechRank.EX);

    private final String id;
    private final float capacity;
    private final float energyMultiplier;
    private final int category;
    private final float typeReduction;
    private final float affinityCapacityMultiplier;
    private final int tier;
    private final TechRank minimumRank;

    ShieldModule(String id, float capacity, float energyMultiplier, int category,
                 float typeReduction, float affinityCapacityMultiplier, int tier, TechRank minimumRank) {
        this.id = id;
        this.capacity = capacity;
        this.energyMultiplier = energyMultiplier;
        this.category = category;
        this.typeReduction = typeReduction;
        this.affinityCapacityMultiplier = affinityCapacityMultiplier;
        this.tier = tier;
        this.minimumRank = minimumRank;
    }

    public String id() { return id; }
    public float capacity() { return capacity * affinityCapacityMultiplier; }
    public float baseCapacity() { return capacity; }
    public float energyMultiplier() { return energyMultiplier; }
    public int category() { return category; }
    public float typeReduction() { return typeReduction; }
    public float affinityCapacityMultiplier() { return affinityCapacityMultiplier; }
    public int tier() { return tier; }

    public String texture() {
        return "shield_module_" + id;
    }

    public TechRank minimumRank() { return minimumRank; }

    public String family() {
        if (category >= 0) return "category_" + category;
        if (this == EFFICIENT) return "efficiency";
        return typeReduction > 0 ? "all_types" : "capacity";
    }

    public static ShieldModule byId(String id) {
        for (ShieldModule module : values()) if (module.id.equals(id)) return module;
        return null;
    }
}
