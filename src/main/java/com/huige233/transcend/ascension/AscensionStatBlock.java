package com.huige233.transcend.ascension;

import net.minecraft.nbt.CompoundTag;

public class AscensionStatBlock {

    public float bonusMaxHealth = 0f;

    public float spellPowerBonus = 0f;

    public float cooldownReduction = 0f;

    public int bonusManaCapacity = 0;

    public float manaRegenBonus = 0f;

    public float moveSpeedBonus = 0f;

    public float reactionBonus = 0f;

    public float critChance = 0f;

    public float critMultiplier = 1.5f;

    public float incomingSpellDamageReduction = 0f;

    public float armorPenetration = 0f;
    public float resistanceIgnore = 0f;
    public float damageReductionFlat = 0f;
    public float spellVamp = 0f;

    public float damageReductionPercent = 0f;

    public float manaCostReduction = 0f;

    public float lifesteal = 0f;

    public float xpGainMult = 0f;

    public float dodgeChance = 0f;

    public float healingReceivedBonus = 0f;

    public float naturalRegenBonus = 0f;

    public float foodConsumptionReduction = 0f;

    public float deathSaveEnabled = 0f;

    public float controlResistance = 0f;

    public float fallDamageReduction = 0f;

    private static final String T_HEALTH   = "hp";
    private static final String T_POWER    = "sp";
    private static final String T_CDR      = "cdr";
    private static final String T_MANA_CAP = "mc";
    private static final String T_MANA_REG = "mr";
    private static final String T_SPEED    = "spd";
    private static final String T_REACT    = "rxn";
    private static final String T_CRIT_C   = "cc";
    private static final String T_CRIT_M   = "cm";
    private static final String T_ISDR     = "isdr";
    private static final String T_APEN    = "apen";
    private static final String T_RIGN    = "rign";
    private static final String T_DRFL    = "drfl";
    private static final String T_SVAMP   = "svmp";
    private static final String T_LIFE    = "life";
    private static final String T_XP      = "xpm";
    private static final String T_DODGE   = "dodge";
    private static final String T_DRP     = "drp";
    private static final String T_MCR     = "mcr";
    private static final String T_HEALING = "heal";
    private static final String T_NATURAL_REGEN = "nregen";
    private static final String T_FOOD_REDUCTION = "foodred";
    private static final String T_DEATH_SAVE = "deathsave";
    private static final String T_CONTROL_RESIST = "ctrlres";
    private static final String T_FALL_REDUCTION = "fallred";

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(T_HEALTH,   bonusMaxHealth);
        tag.putFloat(T_POWER,    spellPowerBonus);
        tag.putFloat(T_CDR,      cooldownReduction);
        tag.putInt  (T_MANA_CAP, bonusManaCapacity);
        tag.putFloat(T_MANA_REG, manaRegenBonus);
        tag.putFloat(T_SPEED,    moveSpeedBonus);
        tag.putFloat(T_REACT,    reactionBonus);
        tag.putFloat(T_CRIT_C,   critChance);
        tag.putFloat(T_CRIT_M,   critMultiplier);
        tag.putFloat(T_ISDR,     incomingSpellDamageReduction);
        tag.putFloat(T_APEN,     armorPenetration);
        tag.putFloat(T_RIGN,     resistanceIgnore);
        tag.putFloat(T_DRFL,     damageReductionFlat);
        tag.putFloat(T_SVAMP,    spellVamp);
        tag.putFloat(T_LIFE,     lifesteal);
        tag.putFloat(T_XP,       xpGainMult);
        tag.putFloat(T_DODGE,    dodgeChance);
        tag.putFloat(T_DRP,      damageReductionPercent);
        tag.putFloat(T_MCR,      manaCostReduction);
        tag.putFloat(T_HEALING,  healingReceivedBonus);
        tag.putFloat(T_NATURAL_REGEN, naturalRegenBonus);
        tag.putFloat(T_FOOD_REDUCTION, foodConsumptionReduction);
        tag.putFloat(T_DEATH_SAVE, deathSaveEnabled);
        tag.putFloat(T_CONTROL_RESIST, controlResistance);
        tag.putFloat(T_FALL_REDUCTION, fallDamageReduction);
        return tag;
    }

    public void load(CompoundTag tag) {
        bonusMaxHealth     = tag.getFloat(T_HEALTH);
        spellPowerBonus    = tag.getFloat(T_POWER);
        cooldownReduction  = tag.getFloat(T_CDR);
        bonusManaCapacity  = tag.getInt  (T_MANA_CAP);
        manaRegenBonus     = tag.getFloat(T_MANA_REG);
        moveSpeedBonus     = tag.getFloat(T_SPEED);
        reactionBonus      = tag.getFloat(T_REACT);
        critChance         = tag.getFloat(T_CRIT_C);
        critMultiplier     = tag.contains(T_CRIT_M) ? tag.getFloat(T_CRIT_M) : 1.5f;
        incomingSpellDamageReduction = tag.getFloat(T_ISDR);
        armorPenetration    = tag.getFloat(T_APEN);
        resistanceIgnore    = tag.getFloat(T_RIGN);
        damageReductionFlat = tag.getFloat(T_DRFL);
        spellVamp           = tag.getFloat(T_SVAMP);
        lifesteal           = tag.getFloat(T_LIFE);
        xpGainMult          = tag.getFloat(T_XP);
        dodgeChance         = tag.getFloat(T_DODGE);
        damageReductionPercent = tag.getFloat(T_DRP);
        manaCostReduction      = tag.getFloat(T_MCR);
        healingReceivedBonus   = tag.getFloat(T_HEALING);
        naturalRegenBonus      = tag.getFloat(T_NATURAL_REGEN);
        foodConsumptionReduction = tag.getFloat(T_FOOD_REDUCTION);
        deathSaveEnabled       = tag.getFloat(T_DEATH_SAVE);
        controlResistance      = tag.getFloat(T_CONTROL_RESIST);
        fallDamageReduction    = tag.getFloat(T_FALL_REDUCTION);
    }

    public void addFrom(AscensionStatBlock other) {
        bonusMaxHealth     += other.bonusMaxHealth;
        spellPowerBonus    += other.spellPowerBonus;
        cooldownReduction  += other.cooldownReduction;
        bonusManaCapacity  += other.bonusManaCapacity;
        manaRegenBonus     += other.manaRegenBonus;
        moveSpeedBonus     += other.moveSpeedBonus;
        reactionBonus      += other.reactionBonus;
        critChance         += other.critChance;
        critMultiplier     = Math.max(critMultiplier, other.critMultiplier);
        incomingSpellDamageReduction += other.incomingSpellDamageReduction;
        armorPenetration    += other.armorPenetration;
        resistanceIgnore    += other.resistanceIgnore;
        damageReductionFlat += other.damageReductionFlat;
        spellVamp           += other.spellVamp;
        lifesteal           += other.lifesteal;
        xpGainMult          += other.xpGainMult;
        dodgeChance         += other.dodgeChance;
        damageReductionPercent += other.damageReductionPercent;
        manaCostReduction      += other.manaCostReduction;

        healingReceivedBonus   += other.healingReceivedBonus;
        naturalRegenBonus      += other.naturalRegenBonus;
        foodConsumptionReduction += other.foodConsumptionReduction;
        deathSaveEnabled       = Math.max(deathSaveEnabled, other.deathSaveEnabled);

        controlResistance      += other.controlResistance;
        fallDamageReduction    += other.fallDamageReduction;
    }

    public float getEffectiveCDR() {
        return Math.min(cooldownReduction, 0.75f);
    }

    public float getEffectiveCritChance() {
        return Math.min(critChance, 0.80f);
    }

    public float getEffectiveArmorPen() { return Math.min(armorPenetration, 0.50f); }
    public float getEffectiveResistIgnore() { return Math.min(resistanceIgnore, 0.30f); }
    public float getEffectiveSpellVamp() { return Math.min(spellVamp, 0.10f); }
    public float getEffectiveLifesteal() { return Math.min(lifesteal, 0.10f); }
    public float getEffectiveDodgeChance() { return Math.min(dodgeChance, 0.50f); }
    public float getEffectiveXpMult() { return 1.0f + Math.max(0f, xpGainMult); }
    public float getEffectiveDamageReductionPercent() { return Math.min(damageReductionPercent, 0.50f); }
    public float getEffectiveManaCostReduction() { return Math.min(manaCostReduction, 0.50f); }

    public static AscensionStatBlock fromLevel(int level, MageClass mageClass) {
        AscensionStatBlock b = new AscensionStatBlock();
        if (level <= 0) return b;
        switch (mageClass) {
            case PYROMANCER -> {
                b.bonusMaxHealth = level * 5.0f; b.spellPowerBonus = level * 0.015f;
                b.cooldownReduction = level * 0.006f; b.bonusManaCapacity = level * 12;
                b.moveSpeedBonus = level * 0.006f; b.critChance = level * 0.015f;
                b.critMultiplier = 1.4f + level * 0.012f;
                b.manaRegenBonus = level * 0.010f;
                b.armorPenetration = level * 0.003f;
            }
            case CRYOMANCER -> {
                b.bonusMaxHealth = level * 6.0f; b.spellPowerBonus = level * 0.012f;
                b.cooldownReduction = level * 0.012f; b.bonusManaCapacity = level * 13;
                b.moveSpeedBonus = level * 0.005f; b.critChance = level * 0.008f;
                b.critMultiplier = 1.4f + level * 0.008f;
                b.manaRegenBonus = level * 0.012f;
                b.incomingSpellDamageReduction = level * 0.003f;
            }
            case STORMCALLER -> {
                b.bonusMaxHealth = level * 4.0f; b.spellPowerBonus = level * 0.015f;
                b.cooldownReduction = level * 0.009f; b.bonusManaCapacity = level * 11;
                b.moveSpeedBonus = level * 0.012f; b.critChance = level * 0.012f;
                b.critMultiplier = 1.4f + level * 0.010f;
                b.manaRegenBonus = level * 0.010f;
                b.dodgeChance = level * 0.003f;
            }
            case CHRONOWEAVER -> {
                b.bonusMaxHealth = level * 5.0f; b.spellPowerBonus = level * 0.010f;
                b.cooldownReduction = level * 0.012f; b.bonusManaCapacity = level * 16;
                b.moveSpeedBonus = level * 0.006f; b.critChance = level * 0.006f;
                b.critMultiplier = 1.4f + level * 0.006f;
                b.manaRegenBonus = level * 0.018f;
                b.healingReceivedBonus = level * 0.008f;
            }
            case ABYSSWALKER -> {
                b.bonusMaxHealth = level * 5.0f; b.spellPowerBonus = level * 0.015f;
                b.cooldownReduction = level * 0.006f; b.bonusManaCapacity = level * 12;
                b.moveSpeedBonus = level * 0.006f; b.critChance = level * 0.015f;
                b.critMultiplier = 1.4f + level * 0.015f;
                b.manaRegenBonus = level * 0.010f;
                b.spellVamp = level * 0.003f;
                b.lifesteal = level * 0.002f;
            }
            case EARTHSHAPER -> {
                b.bonusMaxHealth = level * 8.0f; b.spellPowerBonus = level * 0.010f;
                b.cooldownReduction = level * 0.006f; b.bonusManaCapacity = level * 11;
                b.moveSpeedBonus = level * 0.003f; b.critChance = level * 0.006f;
                b.critMultiplier = 1.4f + level * 0.005f;
                b.manaRegenBonus = level * 0.010f;
                b.damageReductionFlat = level * 0.12f;
                b.incomingSpellDamageReduction = level * 0.004f;
            }
            case OMNISCIENT -> {
                b.bonusMaxHealth = level * 6.0f; b.spellPowerBonus = level * 0.010f;
                b.cooldownReduction = level * 0.012f; b.bonusManaCapacity = level * 14;
                b.moveSpeedBonus = level * 0.006f; b.critChance = level * 0.010f;
                b.critMultiplier = 1.4f + level * 0.009f;
                b.manaRegenBonus = level * 0.012f;
                b.reactionBonus = level * 0.008f;
                b.xpGainMult = level * 0.006f;
            }
            default -> {
                b.bonusMaxHealth = level * 5.0f; b.spellPowerBonus = level * 0.012f;
                b.cooldownReduction = level * 0.008f; b.bonusManaCapacity = level * 12;
                b.moveSpeedBonus = level * 0.008f; b.critChance = level * 0.012f;
                b.critMultiplier = 1.4f + level * 0.008f;
                b.manaRegenBonus = level * 0.010f;
            }
        }
        return b;
    }

    public static AscensionStatBlock awakeningReward() {
        AscensionStatBlock b = new AscensionStatBlock();
        b.bonusMaxHealth    = 22f;
        b.spellPowerBonus   = 0.04f;
        b.cooldownReduction = 0.02f;
        b.bonusManaCapacity = 50;
        b.manaRegenBonus    = 0.05f;
        b.moveSpeedBonus    = 0.02f;
        b.critChance        = 0.05f;
        b.critMultiplier    = 1.6f;
        b.incomingSpellDamageReduction = 0.03f;
        b.damageReductionPercent = 0.02f;
        b.manaCostReduction      = 0.04f;
        b.xpGainMult        = 0.15f;
        return b;
    }

    public static AscensionStatBlock temperingReward() {
        AscensionStatBlock b = new AscensionStatBlock();
        b.bonusMaxHealth    = 33f;
        b.spellPowerBonus   = 0.06f;
        b.cooldownReduction = 0.03f;
        b.bonusManaCapacity = 80;
        b.manaRegenBonus    = 0.10f;
        b.moveSpeedBonus    = 0.04f;
        b.critChance        = 0.08f;
        b.critMultiplier    = 1.8f;
        b.incomingSpellDamageReduction = 0.05f;
        b.damageReductionPercent = 0.03f;
        b.damageReductionFlat = 1.0f;
        b.manaCostReduction   = 0.06f;
        b.resistanceIgnore    = 0.05f;
        b.spellVamp           = 0.02f;
        b.xpGainMult        = 0.25f;
        return b;
    }

    public static AscensionStatBlock purificationReward() {
        AscensionStatBlock b = new AscensionStatBlock();
        b.bonusMaxHealth    = 50f;
        b.spellPowerBonus   = 0.09f;
        b.cooldownReduction = 0.04f;
        b.bonusManaCapacity = 110;
        b.manaRegenBonus    = 0.15f;
        b.moveSpeedBonus    = 0.06f;
        b.critChance        = 0.12f;
        b.critMultiplier    = 2.0f;
        b.reactionBonus     = 0.15f;
        b.incomingSpellDamageReduction = 0.07f;
        b.damageReductionPercent = 0.04f;
        b.damageReductionFlat = 1.0f;
        b.manaCostReduction   = 0.07f;
        b.resistanceIgnore    = 0.07f;
        b.spellVamp           = 0.03f;
        b.lifesteal           = 0.03f;
        b.xpGainMult        = 0.35f;
        return b;
    }

    public static AscensionStatBlock transcendenceReward() {
        AscensionStatBlock b = new AscensionStatBlock();
        b.bonusMaxHealth    = 65f;
        b.spellPowerBonus   = 0.11f;
        b.cooldownReduction = 0.05f;
        b.bonusManaCapacity = 160;
        b.manaRegenBonus    = 0.25f;
        b.moveSpeedBonus    = 0.07f;
        b.critChance        = 0.15f;
        b.critMultiplier    = 2.2f;
        b.reactionBonus     = 0.25f;
        b.incomingSpellDamageReduction = 0.10f;
        b.damageReductionPercent = 0.06f;
        b.damageReductionFlat = 2.0f;
        b.manaCostReduction   = 0.08f;
        b.resistanceIgnore    = 0.08f;
        b.spellVamp           = 0.03f;
        b.lifesteal           = 0.05f;
        b.dodgeChance         = 0.05f;
        b.xpGainMult        = 0.50f;

        b.healingReceivedBonus     = 0.35f;
        b.naturalRegenBonus        = 1.00f;
        b.foodConsumptionReduction = 0.40f;
        b.deathSaveEnabled         = 1.0f;

        b.controlResistance        = 0.35f;
        b.fallDamageReduction      = 0.75f;
        return b;
    }

    public static AscensionStatBlock fromMastery(ElementMastery mastery) {
        AscensionStatBlock b = new AscensionStatBlock();
        if (mastery == ElementMastery.NONE) return b;
        if (mastery == ElementMastery.OMNI) {

            b.spellPowerBonus                = 0f;
            b.bonusManaCapacity              = 60;
            b.manaRegenBonus                 = 0.25f;
            b.cooldownReduction              = 0.10f;
            b.critChance                     = 0.20f;
            b.critMultiplier                 = 2.0f;
            b.xpGainMult                     = 0.15f;
            b.moveSpeedBonus                 = 0f;
            b.resistanceIgnore               = 0.05f;
            return b;
        }

        b.spellPowerBonus                = 0.05f;
        b.bonusManaCapacity              = 60;
        b.manaRegenBonus                 = 0.15f;
        b.cooldownReduction              = 0.10f;
        b.critChance                     = 0.25f;
        b.critMultiplier                 = 2.0f;
        b.bonusMaxHealth                 = 16f;
        b.resistanceIgnore               = 0.05f;

        switch (mastery) {
            case METAL    -> { b.critChance += 0.03f; b.armorPenetration = 0.08f; }
            case WOOD     -> { b.bonusMaxHealth += 12f; b.bonusManaCapacity += 25; b.manaRegenBonus += 0.03f; b.lifesteal = 0.03f; }
            case WATER    -> { b.cooldownReduction += 0.04f; b.incomingSpellDamageReduction += 0.03f; }
            case FIRE     -> { b.spellPowerBonus += 0.05f; b.armorPenetration = 0.04f; }
            case EARTH    -> { b.bonusMaxHealth += 20f; b.damageReductionFlat = 1.5f; b.damageReductionPercent = 0.03f; }
            case CHAOS    -> { b.spellPowerBonus += 0.05f; b.critMultiplier = 2.2f; b.armorPenetration = 0.10f; }
            default -> {}
        }
        return b;
    }
}
