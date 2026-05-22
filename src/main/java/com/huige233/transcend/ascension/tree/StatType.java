package com.huige233.transcend.ascension.tree;

import com.huige233.transcend.ascension.AscensionStatBlock;

import java.util.HashMap;
import java.util.Map;

public enum StatType {
    BONUS_MAX_HEALTH("bonus_max_health") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.bonusMaxHealth += value; }
    },
    SPELL_POWER_BONUS("spell_power_bonus") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.spellPowerBonus += value; }
    },
    COOLDOWN_REDUCTION("cooldown_reduction") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.cooldownReduction += value; }
    },
    BONUS_MANA_CAPACITY("bonus_mana_capacity") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.bonusManaCapacity += (int) value; }
    },
    MANA_REGEN_BONUS("mana_regen_bonus") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.manaRegenBonus += value; }
    },
    MOVE_SPEED_BONUS("move_speed_bonus") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.moveSpeedBonus += value; }
    },
    REACTION_BONUS("reaction_bonus") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.reactionBonus += value; }
    },
    SUMMON_DAMAGE_BONUS("summon_damage_bonus") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.summonDamageBonus += value; }
    },
    CRIT_CHANCE("crit_chance") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.critChance += value; }
    },
    CRIT_MULTIPLIER("crit_multiplier") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.critMultiplier = Math.max(block.critMultiplier, value); }
    },
    INCOMING_SPELL_DAMAGE_REDUCTION("incoming_spell_damage_reduction") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.incomingSpellDamageReduction += value; }
    },
    ARMOR_PENETRATION("armor_penetration") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.armorPenetration += value; }
    },
    RESISTANCE_IGNORE("resistance_ignore") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.resistanceIgnore += value; }
    },
    DAMAGE_REDUCTION_FLAT("damage_reduction_flat") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.damageReductionFlat += value; }
    },
    SPELL_VAMP("spell_vamp") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.spellVamp += value; }
    },
    LIFESTEAL("lifesteal") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.lifesteal += value; }
    },
    XP_GAIN_MULT("xp_gain_mult") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.xpGainMult += value; }
    },
    DODGE_CHANCE("dodge_chance") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.dodgeChance += value; }
    },
    DAMAGE_REDUCTION_PERCENT("damage_reduction_percent") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.damageReductionPercent += value; }
    },
    MANA_COST_REDUCTION("mana_cost_reduction") {
        @Override public void applyTo(AscensionStatBlock block, float value) { block.manaCostReduction += value; }
    };

    public final String jsonKey;

    private static final Map<String, StatType> BY_KEY = new HashMap<>();
    static {
        for (StatType t : values()) BY_KEY.put(t.jsonKey, t);
    }

    StatType(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public abstract void applyTo(AscensionStatBlock block, float value);

    public static StatType getByKey(String key) {
        return BY_KEY.get(key);
    }
}
