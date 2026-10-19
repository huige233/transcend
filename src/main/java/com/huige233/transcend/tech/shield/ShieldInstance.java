package com.huige233.transcend.tech.shield;

import com.huige233.transcend.tech.TechConfig;
import net.minecraft.nbt.CompoundTag;


/** 维护单层护盾的容量、能量、恢复率、防御等级、类别减伤和转换比例，并支持 NBT 存取。 */
public final class ShieldInstance {
    private static final String TIER = "Tier";
    private static final String MAX = "Max";
    private static final String ENERGY = "Energy";
    private static final String REGEN = "Regen";
    private static final String RATIO = "Ratio";
    private static final String OMNI = "Omni";
    private static final String CATEGORY = "Category";
    private static final String DEFENSE_TIER = "DefenseTier";
    private static final String TYPE_REDUCTION = "TypeReduction";
    private static final String TYPE_CATEGORY = "TypeCategory";
    private static final String AFFINITY_MULTIPLIER = "AffinityMultiplier";

    private final String tier;
    private float maxEnergy;
    private float energy;
    private float regen;
    private float focusedRatio;
    private ShieldDefenseMode defenseMode;
    private final int defenseTier;
    private float typeReduction;
    private int typeCategory;
    private float damageReduction;

    public ShieldInstance(String tier, float maxEnergy, float energy, float regen,
                          ShieldDefenseMode defenseMode, float focusedRatio) {
        this(tier, 1, maxEnergy, energy, regen, defenseMode, focusedRatio);
    }

    public ShieldInstance(String tier, int defenseTier, float maxEnergy, float energy, float regen,
                          ShieldDefenseMode defenseMode, float focusedRatio) {
        this.tier = tier == null ? "unknown" : tier;
        this.maxEnergy = finite(maxEnergy);
        this.energy = clamp(energy, 0.0F, this.maxEnergy);
        this.regen = finite(regen);
        this.defenseMode = defenseMode == null ? ShieldDefenseMode.omni() : defenseMode;
        this.focusedRatio = Math.max(1.0F, Math.min(TechConfig.shieldFocusRatioCap(), finite(focusedRatio)));
        this.defenseTier = Math.max(1, defenseTier);
    }

    public String tier() { return tier; }
    public float maxEnergy() { return maxEnergy; }
    public float energy() { return energy; }
    public float regen() { return regen; }
    public float focusedRatio() { return focusedRatio; }
    public int defenseTier() { return defenseTier; }
    public ShieldDefenseMode defenseMode() { return defenseMode; }
    public float typeReduction() { return typeReduction; }
    public float damageReduction() { return damageReduction; }
    public int typeCategory() { return typeCategory; }
    public void setTypeDefense(int category, float reduction) {
        typeCategory = category;
        typeReduction = Math.min(0.85F, finite(reduction));
    }
    public void setDamageReduction(float reduction) {
        damageReduction = Math.min(0.85F, finite(reduction));
    }
    public void setEnergy(float value) { energy = clamp(value, 0.0F, maxEnergy); }
    public void setMaxEnergy(float value) {
        float ratio = maxEnergy <= 0.0F ? 0.0F : energy / maxEnergy;
        maxEnergy = finite(value);
        energy = clamp(maxEnergy * ratio, 0.0F, maxEnergy);
    }
    public void recharge(float amount) { setEnergy(energy + finite(amount)); }
    public float drain(float amount) {
        float drained = Math.min(energy, finite(amount));
        energy -= drained;
        return drained;
    }

    public void save(CompoundTag tag) {
        tag.putString(TIER, tier);
        tag.putFloat(MAX, maxEnergy);
        tag.putFloat(ENERGY, energy);
        tag.putFloat(REGEN, regen);
        tag.putFloat(RATIO, focusedRatio);
        tag.putBoolean(OMNI, defenseMode.omnidirectional());
        tag.putInt(CATEGORY, defenseMode.categoryIndex());
        tag.putInt("CategoryMask", defenseMode.categoryMask());
        tag.putInt(DEFENSE_TIER, defenseTier);
        tag.putFloat(TYPE_REDUCTION, typeReduction);
        tag.putInt(TYPE_CATEGORY, typeCategory);
        tag.putFloat("DamageReduction", damageReduction);
        tag.putFloat(AFFINITY_MULTIPLIER, 1.0F);
    }

    public static ShieldInstance load(CompoundTag tag) {
        boolean omni = tag.getBoolean(OMNI);
        int category = tag.getInt(CATEGORY);
        ShieldDefenseMode mode = new ShieldDefenseMode(omni, Math.max(0, category), tag.getInt("CategoryMask"));
        ShieldInstance instance = new ShieldInstance(tag.getString(TIER), Math.max(1, tag.contains(DEFENSE_TIER) ? tag.getInt(DEFENSE_TIER) : 1),
                tag.getFloat(MAX), tag.getFloat(ENERGY), tag.getFloat(REGEN), mode, tag.getFloat(RATIO));
        if (tag.contains(TYPE_REDUCTION)) instance.setTypeDefense(tag.getInt(TYPE_CATEGORY), tag.getFloat(TYPE_REDUCTION));
        if (tag.contains("DamageReduction")) instance.setDamageReduction(tag.getFloat("DamageReduction"));
        return instance;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, finite(value)));
    }

    private static float finite(float value) { return Float.isFinite(value) ? Math.max(0, value) : 0; }
}
