package com.huige233.transcend.spell;

/**
 * Round 43: 法术增益 Glyph 类型枚举 — 取代 Ars Nouveau Augment 概念。
 *
 * <p>Glyph 通过 NBT IntArrayTag {@code augments} 存储在 spellbook slot 中。
 * 每个 augment 有最大叠加层数限制（由 {@link #maxStack} 控制）。
 *
 * <p>4 个目前已实现的（Round 43）：
 * <ul>
 *   <li>{@link #AMPLIFY} — 伤害 +25% / 层；mana +20% / 层；cooldown +5% / 层（max 4）</li>
 *   <li>{@link #DAMPEN} — mana ×0.7；cooldown ×1.4（unique）</li>
 *   <li>{@link #QUICKFIRE} — mana ×1.3；cooldown ×0.5（unique）</li>
 *   <li>{@link #SPLIT} — shotCount 1→3 扇形（unique）</li>
 * </ul>
 *
 * <p>4 个 declarable but-not-yet-active（Round 44+ 将在 SpellProjectile 中实现）：
 * <ul>
 *   <li>{@link #PIERCE} — 投射穿透敌人</li>
 *   <li>{@link #CHAIN} — 命中后跳到另一敌人</li>
 *   <li>{@link #EXTEND} — 持续效果时长 +50%</li>
 *   <li>{@link #HOMING} — 投射追踪</li>
 * </ul>
 */
public enum SpellAugment {

    AMPLIFY("amplify", 4),
    DAMPEN("dampen", 1),
    QUICKFIRE("quickfire", 1),
    SPLIT("split", 1),

    PIERCE("pierce", 3),
    CHAIN("chain", 1),
    EXTEND("extend", 3),
    HOMING("homing", 1);

    public final String id;
    public final int maxStack;

    SpellAugment(String id, int maxStack) {
        this.id = id;
        this.maxStack = maxStack;
    }

    public static SpellAugment byOrdinal(int ord) {
        SpellAugment[] all = values();
        if (ord < 0 || ord >= all.length) return AMPLIFY;
        return all[ord];
    }
}
