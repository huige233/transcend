package com.huige233.transcend.gear;

import net.minecraft.ChatFormatting;

/**
 * R80: 造物之道（Path of Artifice）5 阶段 enum。
 *
 * <p>玩家约束：
 * <ul>
 *   <li><b>不可逆</b> — 任一阶段写入后不可移除/替换</li>
 *   <li><b>顺序灵活</b> — 但 {@link #CRUCIBLE} 必须首先完成（其它阶段都依赖它）</li>
 * </ul>
 *
 * @see GearForgeData#canEnterStage(net.minecraft.world.item.ItemStack, ForgeStage)
 */
public enum ForgeStage {
    /** E - 坩埚预炼 — 决定基底属性，是其它阶段的前置门 */
    CRUCIBLE("crucible", ChatFormatting.GOLD,         "✦"),
    /** B - 共鸣镶嵌 — 词槽（最多 4），可累加 */
    RESONANCE("resonance", ChatFormatting.AQUA,       "◇"),
    /** A - 灵魂注魂 — 击杀类回响（最多 3），消耗 R77 灵魂能 */
    SOUL("soul", ChatFormatting.RED,                  "◇"),
    /** C - 经历觉醒 — 持有时自动累加；按装备分类计数 */
    EXPERIENCE("experience", ChatFormatting.GREEN,    "◇"),
    /** D - 天命加冕 — 终极冠石（16 选 1） */
    CELESTIAL("celestial", ChatFormatting.LIGHT_PURPLE, "✦");

    public final String id;
    public final ChatFormatting color;
    public final String marker;

    ForgeStage(String id, ChatFormatting color, String marker) {
        this.id = id;
        this.color = color;
        this.marker = marker;
    }

    /** lang 名键。例：CRUCIBLE → "gear.transcend.forge.stage.crucible" */
    public String getNameKey() {
        return "gear.transcend.forge.stage." + id;
    }
}
