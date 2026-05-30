package com.huige233.transcend.gear.forge;

/**
 * R87: 造物之道战斗 hook 数值集中地（5 阶段 → 战斗效果的系数表）。
 *
 * <p>所有系数均为加法或乘法 multiplier — R87 战斗 hook 直接读取本类常量；
 * 后续可改为 data-driven JSON（与 BalanceConfig 同模式），但 MVP 用编译时常量。
 *
 * <h2>设计约束（R35 硬规则）</h2>
 * 所有 multiplier 都设有上限以防止过度叠加：
 * <ul>
 *   <li>sharpness 4 sockets × 5% = 20% 上限</li>
 *   <li>ward 4 sockets × 3% = 12% 上限</li>
 *   <li>spark 4 sockets × 2% = 8% 暴击率（每暴击 +50% 伤害）</li>
 *   <li>swiftness 4 sockets × 4% = 16% 冷却减免（占位，本轮未接入）</li>
 *   <li>leech 4 sockets × 2% = 8% 吸血</li>
 *   <li>focus 4 sockets × 4% = 16% 法力减免（占位）</li>
 *   <li>soul echo per match = 25%，最多 3 × 25% = 75%（同 mob 时极强）</li>
 *   <li>experience tier 0/1/2/3 = 0/+5%/+12%/+25%</li>
 *   <li>aspect.offset 已在 AspectRegistry 定义（pure +20%、dual +8~15%）</li>
 *   <li>blessing buff（D 阶段）= 各 blessing 不同（详见 applyAttackerBlessing）</li>
 * </ul>
 */
public final class ForgeBattleConfig {

    private ForgeBattleConfig() {}

    // ── 共鸣 socket 系数 ─────────────────────────────────────────────
    public static final float SHARPNESS_PER_SOCKET = 0.05f;
    public static final float WARD_PER_SOCKET      = 0.03f;
    public static final float SPARK_CRIT_PER_SOCKET = 0.02f;
    public static final float SPARK_CRIT_BONUS_DAMAGE = 0.50f;
    public static final float SWIFTNESS_COOLDOWN_PER_SOCKET = 0.04f; // 占位，R87 不接入施法
    public static final float LEECH_HEAL_PER_SOCKET = 0.02f;          // 击中目标时治疗 = dmg * LEECH * sockets
    public static final float FOCUS_MANA_PER_SOCKET = 0.04f;          // 占位

    // ── 灵魂回响系数 ─────────────────────────────────────────────────
    public static final float SOUL_ECHO_DAMAGE_BONUS = 0.25f; // 同 mob 类型时每 echo +25%

    // ── 经历觉醒 tier 系数 ───────────────────────────────────────────
    /** [tier 0..3] → 全属性乘数（包括攻击 + 减伤）。 */
    public static final float[] TIER_MULT = { 0.00f, 0.05f, 0.12f, 0.25f };

    // ── 天命 blessing 系数 ───────────────────────────────────────────
    public static final float BLESSING_PURE_BONUS = 0.30f;   // 4 pure 默认 +30% 主属性
    public static final float BLESSING_DUAL_BONUS = 0.15f;   // 12 dual 默认 +15%
    public static final float BLESSING_INDETERMINATE_BONUS = 0.0f;

    // ── 条件加成（blessing 触发条件）─────────────────────────────────
    public static final float SOLAR_DAY_BONUS = 0.20f;   // solar_crown 在白天额外 +20%
    public static final float LUNAR_NIGHT_BONUS = 0.20f; // lunar_crown 在夜晚额外 +20%
}
