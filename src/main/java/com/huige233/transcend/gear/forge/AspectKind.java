package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

/**
 * R82: 坩埚预炼的 6 种基础元素催化剂（Catalyst Element）。
 *
 * <p>每种 catalyst item 对应一个 AspectKind。4 个 catalyst 在坩埚槽位中的
 * 组合（重数集合，不计槽位顺序）会被 {@link AspectRegistry} 解析为 24 种
 * 命名 aspect 之一（或回退到 INDETERMINATE）。
 */
public enum AspectKind {
    FIRE  ("fire",   ChatFormatting.RED),
    WATER ("water",  ChatFormatting.AQUA),
    EARTH ("earth",  ChatFormatting.GOLD),
    WIND  ("wind",   ChatFormatting.GREEN),
    SPIRIT("spirit", ChatFormatting.YELLOW),
    VOID  ("void",   ChatFormatting.DARK_PURPLE);

    public final String id;
    public final ChatFormatting color;

    AspectKind(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    /** 物品 registry name：{@code catalyst_fire} 等。 */
    public String catalystItemId() {
        return "catalyst_" + id;
    }

    /** 翻译键：{@code catalyst.transcend.fire.name} 等。 */
    public String langKey() {
        return "catalyst.transcend." + id + ".name";
    }

    @Nullable
    public static AspectKind byId(String id) {
        for (AspectKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
