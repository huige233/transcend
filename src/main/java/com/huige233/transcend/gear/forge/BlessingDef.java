package com.huige233.transcend.gear.forge;

/**
 * R86: 天命加冕 blessing 定义（终极冠石 16 选 1）。
 *
 * @param id          NBT blessing 字符串 id（例：{@code "solar_crown"}）
 * @param langSubKey  lang 子键（构造 {@code blessing.transcend.<sub>.name}）
 * @param dominant    主元素
 * @param accent      副元素（pure 时 = dominant）
 * @param color       0xRRGGBB
 */
public record BlessingDef(
        String id,
        String langSubKey,
        CelestialKind dominant,
        CelestialKind accent,
        int color
) {
    public boolean isPure() { return dominant == accent; }
    public String nameKey() { return "blessing.transcend." + langSubKey + ".name"; }
}
