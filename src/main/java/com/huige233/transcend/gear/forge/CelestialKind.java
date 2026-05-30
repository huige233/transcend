package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

/**
 * R86: 天命加冕（D 阶段）的 4 种天命碎片 enum。
 *
 * <p>4 个碎片在加冕祭坛 4 槽位中的组合（不计槽位顺序）由 {@link BlessingRegistry}
 * 解析为 16 命名 blessing 之一（或 INDETERMINATE 回退）。
 *
 * <p>设计灵感：太阳 / 月亮 / 群星 / 深渊 — 4 种天文/宇宙概念，覆盖 vanilla 月相 +
 * 时间 + 维度的全部隐喻空间。
 */
public enum CelestialKind {
    SUN  ("sun",   ChatFormatting.GOLD),
    MOON ("moon",  ChatFormatting.AQUA),
    STAR ("star",  ChatFormatting.YELLOW),
    ABYSS("abyss", ChatFormatting.DARK_PURPLE);

    public final String id;
    public final ChatFormatting color;

    CelestialKind(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String fragmentItemId() { return "celestial_fragment_" + id; }
    public String langKey()        { return "celestial.transcend." + id + ".name"; }

    @Nullable
    public static CelestialKind byId(String id) {
        for (CelestialKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
