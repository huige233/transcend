package com.huige233.transcend.ascension;

import net.minecraft.nbt.CompoundTag;

/**
 * 飞升带来的持久化基础属性强化
 *
 * 这些属性通过 AscensionHandler 在玩家加入/飞升等级变化时
 * 以 AttributeModifier 的形式注入到玩家属性中。
 *
 * 属性来源叠加规则（加法）：
 *  1. 飞升等级被动（每级线性增长）
 *  2. 仪式完成奖励（一次性大幅提升）
 *  3. 天赋节点（每个节点贡献固定量）
 *  4. 元素专精（选择专精时额外加成）
 */
public class AscensionStatBlock {

    // ── 生命值上限额外加成（心数，原版2HP=1心）──────────────────────────
    public float bonusMaxHealth = 0f;

    // ── 法术强度乘数（叠加到damage计算链）──────────────────────────────
    // 最终伤害 = baseDamage * (1 + spellPowerBonus + masteryBonus + armorBonus)
    public float spellPowerBonus = 0f;

    // ── 施法冷却减少（减少百分比，0~0.8上限）────────────────────────────
    public float cooldownReduction = 0f;

    // ── 魔力上限额外增加（魔力晶体容量）────────────────────────────────
    public int bonusManaCapacity = 0;

    // ── 魔力恢复速率加成（每tick恢复多少魔力，未来扩展用）──────────────
    public float manaRegenBonus = 0f;

    // ── 移动速度加成（0.05 = +5%）────────────────────────────────────
    public float moveSpeedBonus = 0f;

    // ── 元素反应伤害加成（0.2 = 元素反应额外+20%）────────────────────
    public float reactionBonus = 0f;

    // ── 召唤物伤害加成 ────────────────────────────────────────────────
    public float summonDamageBonus = 0f;

    // ── 暴击率（0.0~1.0，每次法术命中有几率造成1.5x伤害）──────────────
    public float critChance = 0f;

    // ── 暴击倍率（默认1.5，可通过节点/专精提升）──────────────────────
    public float critMultiplier = 1.5f;

    // ── 受到法术伤害减少（专精赋予对专精元素的抗性，全系获得全元素小幅抗性）
    // 0.15 = 受到对应元素法术伤害减少15%
    public float incomingSpellDamageReduction = 0f;

    public float armorPenetration = 0f;
    public float resistanceIgnore = 0f;
    public float damageReductionFlat = 0f;
    public float spellVamp = 0f;

    // ── 新增（v6 精算）─────────────────────────────────────────────────
    /** 百分比通用减伤（multiplicative pass：dmg *= (1 - drPercent)；上限 0.50） */
    public float damageReductionPercent = 0f;

    /** 法术魔力消耗减免（multiplicative pass：cost *= (1 - costRed)；上限 0.50） */
    public float manaCostReduction = 0f;

    // ── 新增（v3 全面增强）─────────────────────────────────────────────
    /** 物理/法术命中目标时回复造成伤害的百分比（0.10 = 10%） */
    public float lifesteal = 0f;

    /** XP 获取倍率叠加值（0.20 = +20%；最终倍率 = 1 + xpGainMult） */
    public float xpGainMult = 0f;

    /** 闪避概率（0.10 = 10% 完全免伤；上限 0.50） */
    public float dodgeChance = 0f;

    // ── R74 新增（完全体追加属性）─────────────────────────────────────
    /** AoE 范围伤害加成（0.25 = 范围类法术多打 25%） */
    public float aoeDamageBonus = 0f;

    /** 治疗效果增强（0.35 = 收到的治疗 +35%；vanilla regen / 药水 / 法术） */
    public float healingReceivedBonus = 0f;

    /** 自然恢复加速（1.00 = vanilla 食物 regen 速度 ×2） */
    public float naturalRegenBonus = 0f;

    /** 饱食度消耗减免（0.40 = exhaustion 增加 ×0.60） */
    public float foodConsumptionReduction = 0f;

    /** 死亡保命开关（>0 = 启用，且需 stage 4 + level 10；冷却 5 分钟一次） */
    public float deathSaveEnabled = 0f;

    // ── R75 新增 ───────────────────────────────────────────────────────
    /** 控制抗性（0.35 = 受到的负面 vanilla MobEffect 持续时间 -35%） */
    public float controlResistance = 0f;

    /** 摔落伤害减免（0.75 = 摔落伤害 -75%） */
    public float fallDamageReduction = 0f;

    // ─── 序列化 ───────────────────────────────────────────────────────

    private static final String T_HEALTH   = "hp";
    private static final String T_POWER    = "sp";
    private static final String T_CDR      = "cdr";
    private static final String T_MANA_CAP = "mc";
    private static final String T_MANA_REG = "mr";
    private static final String T_SPEED    = "spd";
    private static final String T_REACT    = "rxn";
    private static final String T_SUMMON   = "sum";
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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(T_HEALTH,   bonusMaxHealth);
        tag.putFloat(T_POWER,    spellPowerBonus);
        tag.putFloat(T_CDR,      cooldownReduction);
        tag.putInt  (T_MANA_CAP, bonusManaCapacity);
        tag.putFloat(T_MANA_REG, manaRegenBonus);
        tag.putFloat(T_SPEED,    moveSpeedBonus);
        tag.putFloat(T_REACT,    reactionBonus);
        tag.putFloat(T_SUMMON,   summonDamageBonus);
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
        summonDamageBonus  = tag.getFloat(T_SUMMON);
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
    }

    /** 将 other 的所有字段加到 this 上 */
    public void addFrom(AscensionStatBlock other) {
        bonusMaxHealth     += other.bonusMaxHealth;
        spellPowerBonus    += other.spellPowerBonus;
        cooldownReduction  += other.cooldownReduction;
        bonusManaCapacity  += other.bonusManaCapacity;
        manaRegenBonus     += other.manaRegenBonus;
        moveSpeedBonus     += other.moveSpeedBonus;
        reactionBonus      += other.reactionBonus;
        summonDamageBonus  += other.summonDamageBonus;
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
        // R74 新增
        aoeDamageBonus         += other.aoeDamageBonus;
        healingReceivedBonus   += other.healingReceivedBonus;
        naturalRegenBonus      += other.naturalRegenBonus;
        foodConsumptionReduction += other.foodConsumptionReduction;
        deathSaveEnabled       = Math.max(deathSaveEnabled, other.deathSaveEnabled);
        // R75 新增
        controlResistance      += other.controlResistance;
        fallDamageReduction    += other.fallDamageReduction;
    }

    /** 冷却减少上限截断 */
    public float getEffectiveCDR() {
        return Math.min(cooldownReduction, 0.75f);
    }

    /** 暴击率上限截断 */
    public float getEffectiveCritChance() {
        return Math.min(critChance, 0.80f);
    }

    public float getEffectiveArmorPen() { return Math.min(armorPenetration, 0.50f); }
    public float getEffectiveResistIgnore() { return Math.min(resistanceIgnore, 0.30f); }
    public float getEffectiveSpellVamp() { return Math.min(spellVamp, 0.20f); }
    public float getEffectiveLifesteal() { return Math.min(lifesteal, 0.25f); }
    public float getEffectiveDodgeChance() { return Math.min(dodgeChance, 0.50f); }
    public float getEffectiveXpMult() { return 1.0f + Math.max(0f, xpGainMult); }
    public float getEffectiveDamageReductionPercent() { return Math.min(damageReductionPercent, 0.50f); }
    public float getEffectiveManaCostReduction() { return Math.min(manaCostReduction, 0.50f); }

    // ─── 飞升等级被动增长 ──────────────────────────────────────────────

    /**
     * 根据飞升等级计算等级被动加成（不含仪式/天赋）
     * v5 重平衡: 每层贡献约 25% 的终极目标 (level + ritual + mastery + nodes ≈ 100%)
     */
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
            case ARCANIST -> {
                b.bonusMaxHealth = level * 6.0f; b.spellPowerBonus = level * 0.012f;
                b.cooldownReduction = level * 0.010f; b.bonusManaCapacity = level * 16;
                b.moveSpeedBonus = level * 0.004f; b.critChance = level * 0.008f;
                b.critMultiplier = 1.4f + level * 0.006f;
                b.manaRegenBonus = level * 0.018f;
                b.xpGainMult = level * 0.008f;
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
            case OMNIMANCER -> {
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

    // ─── 仪式奖励（一次性，叠加）──────────────────────────────────────

    /** 完成觉醒仪式的奖励 (v6: 4 阶段精算 — 累计达完全体目标) */
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

    /** 完成磨砺仪式的奖励 */
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

    /** 完成净化仪式的奖励 */
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

    /** 完成超越仪式的奖励 (T4 capstone — 推完终点：完全体里程碑) */
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
        b.summonDamageBonus = 0.30f;
        b.incomingSpellDamageReduction = 0.10f;
        b.damageReductionPercent = 0.06f;
        b.damageReductionFlat = 2.0f;
        b.manaCostReduction   = 0.08f;
        b.resistanceIgnore    = 0.08f;
        b.spellVamp           = 0.03f;
        b.lifesteal           = 0.05f;
        b.dodgeChance         = 0.05f;
        b.xpGainMult        = 0.50f;
        // R74: 完全体专属（仅完成 TRANSCENDENCE 仪式 + 最高等级时解锁）
        b.aoeDamageBonus           = 0.25f;
        b.healingReceivedBonus     = 0.35f;
        b.naturalRegenBonus        = 1.00f;
        b.foodConsumptionReduction = 0.40f;
        b.deathSaveEnabled         = 1.0f;
        // R75: 完全体专属（控制抗性 + 摔落减免）
        b.controlResistance        = 0.35f;
        b.fallDamageReduction      = 0.75f;
        return b;
    }

    // ─── 天赋节点贡献（已迁移到 TreeRegistry.computeNodeStats()）────────

    // ─── 元素专精贡献 ─────────────────────────────────────────────────

    public static AscensionStatBlock fromMastery(ElementMastery mastery) {
        AscensionStatBlock b = new AscensionStatBlock();
        if (mastery == ElementMastery.NONE) return b;
        if (mastery == ElementMastery.OMNI) {
            // 全能：spellPowerBonus stat 极少（masteredBonus 0.25 已经覆盖大部分），其余属性补齐完全体目标
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
        // 元素专精基础（masteredBonus 0.60 已经覆盖大部分本职法术，stat 仅补一点）
        b.spellPowerBonus                = 0.05f;
        b.bonusManaCapacity              = 60;
        b.manaRegenBonus                 = 0.15f;
        b.cooldownReduction              = 0.10f;
        b.critChance                     = 0.25f;
        b.critMultiplier                 = 2.0f;
        b.bonusMaxHealth                 = 16f;
        b.resistanceIgnore               = 0.05f;
        // 元素特化加成（小幅，构建多样性）
        switch (mastery) {
            case BLOOD    -> { b.bonusMaxHealth += 20f; b.critChance += 0.03f; b.lifesteal = 0.06f; }
            case VOID     -> { b.spellPowerBonus += 0.05f; b.reactionBonus = 0.12f; b.dodgeChance = 0.05f; }
            case TIME     -> { b.cooldownReduction += 0.04f; b.xpGainMult = 0.15f; b.manaRegenBonus += 0.03f; }
            case SPACE    -> { b.moveSpeedBonus = 0.08f; b.cooldownReduction += 0.02f; b.dodgeChance = 0.03f; }
            case CHAOS    -> { b.spellPowerBonus += 0.05f; b.critMultiplier = 2.2f; b.armorPenetration = 0.10f; }
            case ELDRITCH -> { b.spellPowerBonus += 0.05f; b.reactionBonus = 0.15f; b.spellVamp = 0.05f; }
            case HOLY     -> { b.bonusMaxHealth += 15f; b.manaRegenBonus += 0.03f; b.damageReductionFlat = 1.0f; }
            case NATURE   -> { b.bonusMaxHealth += 12f; b.bonusManaCapacity += 25; b.manaRegenBonus += 0.02f; b.lifesteal = 0.03f; }
            case EARTH    -> { b.bonusMaxHealth += 20f; b.damageReductionFlat = 1.5f; b.damageReductionPercent = 0.03f; }
            case FIRE     -> { b.spellPowerBonus += 0.05f; b.armorPenetration = 0.04f; }
            case ICE      -> { b.cooldownReduction += 0.03f; b.incomingSpellDamageReduction += 0.03f; }
            case THUNDER  -> { b.critChance += 0.03f; b.armorPenetration = 0.04f; }
            case WIND     -> { b.moveSpeedBonus = 0.05f; b.dodgeChance = 0.04f; }
            case DARK     -> { b.spellPowerBonus += 0.05f; b.spellVamp = 0.03f; }
            case LIGHT    -> { b.spellPowerBonus += 0.03f; b.critChance += 0.03f; }
            case POISON   -> { b.spellPowerBonus += 0.03f; b.lifesteal = 0.03f; }
            case ACID     -> { b.armorPenetration = 0.05f; b.spellPowerBonus += 0.03f; }
            case SONIC    -> { b.spellPowerBonus += 0.03f; b.dodgeChance = 0.03f; }
            default -> {}
        }
        return b;
    }
}
