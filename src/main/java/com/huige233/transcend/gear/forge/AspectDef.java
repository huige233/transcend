package com.huige233.transcend.gear.forge;

/**
 * R82: 坩埚 aspect 定义。
 *
 * <p>每个 aspect 描述坩埚预炼写入装备的"基底"元素表述：
 * <ul>
 *   <li>{@link #id}：装备 NBT 中存储的字符串 id（{@code "fire_pure"} / {@code "fire_wind_dual"} 等）</li>
 *   <li>{@link #langSubKey}：lang 文件的子键（{@code "fire_pure"}），实际 key = {@code aspect.transcend.<sub>.name}</li>
 *   <li>{@link #offset}：写入到 {@code GearForgeData.writeCrucible()} 的 offset 字段
 *       —— R86 战斗 hook 据此调整伤害/护甲/命中率等</li>
 *   <li>{@link #color}：tooltip / 粒子颜色（0xRRGGBB）</li>
 *   <li>{@link #dominant}/{@link #accent}：构造该 aspect 的主元素/副元素（pure 时 dominant == accent）</li>
 * </ul>
 *
 * @param id          装备 NBT 字符串 id（例：{@code "fire_pure"}）
 * @param langSubKey  lang 文件 sub-key（用于自动构造 {@code aspect.transcend.<sub>.name}）
 * @param dominant    主元素 catalyst
 * @param accent      副元素 catalyst（pure aspect 时 = dominant）
 * @param offset      数值偏移量（写入 ItemStack.NBT，由战斗 hook 读取）
 * @param color       0xRRGGBB 颜色码
 */
public record AspectDef(
        String id,
        String langSubKey,
        AspectKind dominant,
        AspectKind accent,
        float offset,
        int color
) {
    /** 是否为纯净 aspect（4 个相同 catalyst）。 */
    public boolean isPure() {
        return dominant == accent;
    }

    /** 翻译键：{@code aspect.transcend.fire_pure.name}。 */
    public String nameKey() {
        return "aspect.transcend." + langSubKey + ".name";
    }
}
