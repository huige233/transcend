package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

/**
 * R83: 共鸣镶嵌（B 阶段）的 6 种共鸣水晶 enum。
 *
 * <p>每种水晶对应一种"主题"，在装备 NBT 中作为一个 socket 写入；R86 战斗 hook
 * 读取 socket 列表后按 kind 累加效果（参考 R80 锁定：上限 4 socket，每 socket 1 个 kind + 1 个 level）。
 *
 * <h2>6 共鸣主题（玩家从中选 4 个组合到装备）</h2>
 * <ul>
 *   <li>{@link #SHARPNESS} 锋锐 — +伤害</li>
 *   <li>{@link #SWIFTNESS} 敏疾 — -冷却</li>
 *   <li>{@link #LEECH}     嗜血 — +吸血</li>
 *   <li>{@link #WARD}      护佑 — +护甲</li>
 *   <li>{@link #FOCUS}     凝神 — -法力消耗</li>
 *   <li>{@link #SPARK}     灵犀 — +暴击</li>
 * </ul>
 *
 * <p>同一种水晶可以镶嵌多次（占用多个 socket）— 由战斗 hook 决定是否叠加或饱和。
 */
public enum ResonanceKind {
    SHARPNESS("sharpness", ChatFormatting.RED),
    SWIFTNESS("swiftness", ChatFormatting.YELLOW),
    LEECH    ("leech",     ChatFormatting.DARK_RED),
    WARD     ("ward",      ChatFormatting.AQUA),
    FOCUS    ("focus",     ChatFormatting.BLUE),
    SPARK    ("spark",     ChatFormatting.LIGHT_PURPLE);

    public final String id;
    public final ChatFormatting color;

    ResonanceKind(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String crystalItemId() { return "resonance_crystal_" + id; }
    public String langKey()       { return "resonance.transcend." + id + ".name"; }
    public String descKey()       { return "resonance.transcend." + id + ".desc"; }

    @Nullable
    public static ResonanceKind byId(String id) {
        for (ResonanceKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
