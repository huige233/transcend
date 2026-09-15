package com.huige233.transcend.items.tech;


/** 定义可替换护盾模块的容量、耗能倍率、分类减伤、亲和增容和科技等级。 */
public enum ShieldModule {
    BASIC("basic", 100.0F, 1.0F, -1, 0.0F, 1.0F, 1),
    REINFORCED("reinforced", 150.0F, 1.15F, -1, 0.0F, 1.0F, 1),
    EFFICIENT("efficient", 100.0F, 0.75F, -1, 0.0F, 1.0F, 1),
    
    T5_MECHANICAL("t5_mechanical", 15_000.0F, 1.0F, 0, 0.85F, 2.5F, 5);

    private final String id;
    private final float capacity;
    private final float energyMultiplier;
    private final int category;
    private final float typeReduction;
    private final float affinityCapacityMultiplier;
    private final int tier;

    ShieldModule(String id, float capacity, float energyMultiplier, int category,
                 float typeReduction, float affinityCapacityMultiplier, int tier) {
        this.id = id;
        this.capacity = capacity;
        this.energyMultiplier = energyMultiplier;
        this.category = category;
        this.typeReduction = typeReduction;
        this.affinityCapacityMultiplier = affinityCapacityMultiplier;
        this.tier = tier;
    }

    public String id() { return id; }
    public float capacity() { return capacity * affinityCapacityMultiplier; }
    public float baseCapacity() { return capacity; }
    public float energyMultiplier() { return energyMultiplier; }
    public int category() { return category; }
    public float typeReduction() { return typeReduction; }
    public float affinityCapacityMultiplier() { return affinityCapacityMultiplier; }
    public int tier() { return tier; }

    public static ShieldModule byId(String id) {
        for (ShieldModule module : values()) if (module.id.equals(id)) return module;
        return null;
    }
}
