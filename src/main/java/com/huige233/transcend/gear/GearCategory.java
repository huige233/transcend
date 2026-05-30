package com.huige233.transcend.gear;

import com.huige233.transcend.items.TranscendWand;
import com.huige233.transcend.items.tools.AspectSwordItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * R80: 装备分类 — 决定 C 经历觉醒 (Experience Awakening) 的计数维度。
 *
 * <p>同一件装备只追踪一个相关计数器：
 * <ul>
 *   <li>{@link #WEAPON} — 击杀计数（武器持有时杀敌 +1）</li>
 *   <li>{@link #TOOL}   — 挖掘计数（工具破坏方块 +1）</li>
 *   <li>{@link #ARMOR}  — 受击计数（穿戴时受到伤害 +1，独立于伤害量）</li>
 *   <li>{@link #OTHER}  — 不可进入造物之道管线</li>
 * </ul>
 *
 * <p>Q1 玩家决策：仅 {@code isDamageableItem()} 装备可入管线。
 * {@link #OTHER} 类别的物品一律拒绝（即使可堆叠为 1）。
 */
public enum GearCategory {
    WEAPON,
    TOOL,
    ARMOR,
    OTHER;

    /** 按物品类决定分类。武器 = sword + transcend wand；工具 = vanilla DiggerItem 子类；护甲 = ArmorItem。 */
    public static GearCategory classify(ItemStack stack) {
        if (stack.isEmpty()) return OTHER;
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof TranscendWand
                || item instanceof AspectSwordItem) {
            return WEAPON;
        }
        if (item instanceof DiggerItem) {
            // PickaxeItem / AxeItem / ShovelItem / HoeItem 都继承 DiggerItem
            return TOOL;
        }
        if (item instanceof ArmorItem) {
            return ARMOR;
        }
        return OTHER;
    }
}
