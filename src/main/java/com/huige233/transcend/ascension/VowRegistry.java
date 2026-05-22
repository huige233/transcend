package com.huige233.transcend.ascension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 誓约注册表 — 按飞升阶段组织的全部可选誓约。
 *
 * <p><b>Round 16 重平衡</b>：奖励与代价整体上调 2-3 倍，让每个誓约成为「定义构筑」的关键抉择。
 * 设计原则：高阶誓约获得近 endgame 级数值跃迁，但同时承担显著 trade-off。
 */
public class VowRegistry {
    private static final Map<String, AscensionVow> VOWS = new LinkedHashMap<>();

    // ===== Stage 1 vows — 觉醒誓约 (中等收益 + 中等代价) =====

    /** 专注誓约：法术伤害+35%；代价：法阵上限-2、法术魔耗+20% */
    public static final AscensionVow OATH_OF_FOCUS = register(
        AscensionVow.builder("oath_of_focus", 1)
            .spellDamage(0.35f)
            .circleLimit(-2)
            .manaCost(1.20f)
            .build()
    );

    /** 灵脉誓约：法阵上限+4、维持-25%；代价：个人法术魔耗+30% */
    public static final AscensionVow OATH_OF_LEYLINES = register(
        AscensionVow.builder("oath_of_leylines", 1)
            .circleLimit(4)
            .circleUpkeep(0.75f)
            .manaCost(1.30f)
            .build()
    );

    /** 共鸣誓约：召唤+45%、治疗+50%；代价：直接伤害-30%、魔耗+15% */
    public static final AscensionVow OATH_OF_COMMUNION = register(
        AscensionVow.builder("oath_of_communion", 1)
            .summon(0.45f)
            .healing(1.50f)
            .spellDamage(-0.30f)
            .manaCost(1.15f)
            .build()
    );

    // ===== Stage 2 vows — 淬炼誓约 (大幅强化 + 显著代价) =====

    /** 精炼之躯：魔力上限+500；代价：最大生命-30 */
    public static final AscensionVow REFINED_VESSEL = register(
        AscensionVow.builder("refined_vessel", 2)
            .manaCap(500f)
            .health(-30)
            .build()
    );

    /** 暴烈经脉：暴击率+25%、暴击下限 2.0x；代价：受到的治疗-50% */
    public static final AscensionVow VIOLENT_MERIDIAN = register(
        AscensionVow.builder("violent_meridian", 2)
            .critChance(0.25f)
            .critMult(2.0f)
            .healing(0.50f)
            .build()
    );

    /** 静心之诀：冷却缩减+25%；代价：移动速度-15% */
    public static final AscensionVow STILL_MIND = register(
        AscensionVow.builder("still_mind", 2)
            .cdr(0.25f)
            .moveSpeed(0.85f)
            .build()
    );

    // ===== Stage 3 vows — 净化誓约 (戏剧性权衡) =====

    /** 禁断透支：魔力上限+200；代价：最大生命-15 */
    public static final AscensionVow FORBIDDEN_OVERDRAFT = register(
        AscensionVow.builder("forbidden_overdraft", 3)
            .manaCap(200f)
            .health(-15)
            .build()
    );

    /** 灵脉君主：法阵上限+3、反应+25%；代价：维持+40%、魔力上限-100 */
    public static final AscensionVow LEYLINE_SOVEREIGN = register(
        AscensionVow.builder("leyline_sovereign", 3)
            .circleLimit(3)
            .reaction(0.25f)
            .circleUpkeep(1.40f)
            .manaCap(-100f)
            .build()
    );

    /** 玻璃之星：反应+50%、法术伤害+30%；代价：受治疗-30% */
    public static final AscensionVow GLASS_STAR = register(
        AscensionVow.builder("glass_star", 3)
            .reaction(0.50f)
            .spellDamage(0.30f)
            .healing(0.70f)
            .build()
    );

    // ===== Stage 4 capstone vows — 超越誓约 (终极强化, build-defining) =====

    /** 唯一化身：法术伤害+80%、暴击+10%；代价：元素特化(非主修元素 -50%) */
    public static final AscensionVow AVATAR_OF_ONE = register(
        AscensionVow.builder("avatar_of_one", 4)
            .spellDamage(0.80f)
            .critChance(0.10f)
            .build()
    );

    /** 棱镜圣徒：全元素+25%、反应+40%、魔力上限+300；代价：不可封印元素 */
    public static final AscensionVow PRISM_SAINT = register(
        AscensionVow.builder("prism_saint", 4)
            .spellDamage(0.25f)
            .reaction(0.40f)
            .manaCap(300f)
            .build()
    );

    /** 世界法阵：法阵上限+6、维持-35%、半径+1；代价：法杖施法间隔+40% */
    public static final AscensionVow WORLD_ARRAY = register(
        AscensionVow.builder("world_array", 4)
            .circleLimit(6)
            .circleUpkeep(0.65f)
            .manaCost(1.40f)
            .build()
    );

    // ===== Helper methods =====
    private static AscensionVow register(AscensionVow vow) {
        VOWS.put(vow.getId(), vow);
        return vow;
    }

    public static AscensionVow get(String id) {
        return VOWS.get(id);
    }

    public static List<AscensionVow> getVowsForStage(int stage) {
        List<AscensionVow> result = new ArrayList<>();
        for (AscensionVow v : VOWS.values()) {
            if (v.getStage() == stage) {
                result.add(v);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Collection<AscensionVow> all() {
        return Collections.unmodifiableCollection(VOWS.values());
    }
}
