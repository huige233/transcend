package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

/**
 * R91: 触发型词条种类 enum — 造物之道的第 6 类独立强化（与 E/B/A/C/D 5 阶段平行）。
 *
 * <p>玩家请求："触发型词条变单独的" — 不绑 aspect/socket 组合，作为独立可装填的强化方式。
 *
 * <h2>3 大类（按事件类型分）</h2>
 * <ul>
 *   <li>{@link Category#ON_KILL} — 击杀触发（攻击者杀死目标时）</li>
 *   <li>{@link Category#ON_HURT} — 受击触发（玩家自身受到伤害时）</li>
 *   <li>{@link Category#PERIODIC} — 周期触发（基于 server tick 计数）</li>
 * </ul>
 *
 * <h2>装填规则</h2>
 * <ul>
 *   <li>每件装备 ≤ 1 个触发词条（不可叠加）</li>
 *   <li>不可逆 — 一旦铭刻不可替换/移除</li>
 *   <li>与 5 阶段独立 — 不需要 CRUCIBLE 前置（白板装备也可铭刻）</li>
 * </ul>
 */
public enum TriggerAffixKind {

    // ─── ON_KILL（击杀触发，5 种）────────────────────────────────────
    /** 余烬 — 击杀时目标位置爆炸 2×2 火焰 AOE */
    EMBER       ("ember",        Category.ON_KILL,  ChatFormatting.RED),
    /** 回响 — 击杀时玩家回 1 HP */
    REPRISE     ("reprise",      Category.ON_KILL,  ChatFormatting.GREEN),
    /** 共鸣 — 击杀后下次攻击 +50% 伤害（叠加上限 3 次）*/
    HARMONIC    ("harmonic",     Category.ON_KILL,  ChatFormatting.YELLOW),
    /** 嗜血飞溅 — 击杀时 5 格内敌人附加流血 3 秒（vanilla wither 效果 II）*/
    SANGUINE    ("sanguine",     Category.ON_KILL,  ChatFormatting.DARK_RED),
    /** 灵魂收割 — 击杀时装备 +1 灵魂能（用于 R84 灵魂铭刻消耗）*/
    SOUL_REAP   ("soul_reap",    Category.ON_KILL,  ChatFormatting.LIGHT_PURPLE),

    // ─── ON_HURT（受击触发，4 种）────────────────────────────────────
    /** 荆棘反弹 — 受击时反弹 30% 伤害给攻击者 */
    THORNBACK   ("thornback",    Category.ON_HURT,  ChatFormatting.DARK_GREEN),
    /** 绝境闪现 — HP < 30% 时一次/min 自动获得 Speed III 3 秒 */
    LAST_DASH   ("last_dash",    Category.ON_HURT,  ChatFormatting.AQUA),
    /** 护盾自愈 — absorption（黄盾）破时立即回 1 HP */
    AEGIS_HEAL  ("aegis_heal",   Category.ON_HURT,  ChatFormatting.GOLD),
    /** 死亡回响 — 致命伤一次/5min 改为留 1 HP + 击退 5 格内敌人 */
    DEATH_ECHO  ("death_echo",   Category.ON_HURT,  ChatFormatting.DARK_PURPLE),

    // ─── PERIODIC（周期触发，3 种）───────────────────────────────────
    /** 脉冲 — 每 30 秒下次攻击附带 3×3 AOE（按 SHARPNESS 模型） */
    PULSE       ("pulse",        Category.PERIODIC, ChatFormatting.BLUE),
    /** 守护光环 — 每 60 秒附近 5 格队友 +1 absorption（叠加上限 1）*/
    AEGIS_AURA  ("aegis_aura",   Category.PERIODIC, ChatFormatting.WHITE),
    /** 灵能溢出 — 每 20 秒下次攻击 +100% 伤害 */
    OVERFLOW    ("overflow",     Category.PERIODIC, ChatFormatting.LIGHT_PURPLE);

    public enum Category {
        ON_KILL, ON_HURT, PERIODIC
    }

    public final String id;
    public final Category category;
    public final ChatFormatting color;

    TriggerAffixKind(String id, Category category, ChatFormatting color) {
        this.id = id;
        this.category = category;
        this.color = color;
    }

    public String itemId()  { return "trigger_inscription_" + id; }
    public String nameKey() { return "trigger_affix.transcend." + id + ".name"; }
    public String descKey() { return "trigger_affix.transcend." + id + ".desc"; }

    @Nullable
    public static TriggerAffixKind byId(String id) {
        if (id == null) return null;
        for (TriggerAffixKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
