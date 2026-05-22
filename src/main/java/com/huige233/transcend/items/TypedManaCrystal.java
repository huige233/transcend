package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Round 17: 多样化法力体系 — 元素相位 mana 水晶。
 *
 * <p>继承 {@link MagicCrystalItem} 让现有抽魔管线 (MagicCrystalHelper.consumeMana)
 * 通过 {@code instanceof MagicCrystalItem} 自动识别。每种 aspect 有不同储值。
 *
 * <p>储值梯度（与 raw=1 / refined=3 对照）：
 * <ul>
 *   <li>{@link ManaAspect#TAINTED}  = 2  — 廉价大量产，附带轻微混乱风味</li>
 *   <li>{@link ManaAspect#AETHER}   = 5  — 中阶相位，配合 aether_shard 制取</li>
 *   <li>{@link ManaAspect#BLOOD}    = 8  — 高阶，原料含 ghast_tear 暗示生命献祭</li>
 *   <li>{@link ManaAspect#COSMIC}   = 15 — 终局，nether_star 凝缩</li>
 * </ul>
 */
public class TypedManaCrystal extends MagicCrystalItem {

    public enum ManaAspect {
        AETHER ("aether",  5,  ChatFormatting.AQUA,         Rarity.EPIC,        false),
        BLOOD  ("blood",   8,  ChatFormatting.DARK_RED,     Rarity.EPIC,        true),
        COSMIC ("cosmic",  15, ChatFormatting.LIGHT_PURPLE, ModRarities.COSMIC, true),
        TAINTED("tainted", 2,  ChatFormatting.DARK_GREEN,   Rarity.RARE,        false);

        public final String id;
        public final int value;
        public final ChatFormatting nameColor;
        public final Rarity rarity;
        public final boolean foilGlow;

        ManaAspect(String id, int value, ChatFormatting nameColor, Rarity rarity, boolean foilGlow) {
            this.id = id;
            this.value = value;
            this.nameColor = nameColor;
            this.rarity = rarity;
            this.foilGlow = foilGlow;
        }
    }

    private final ManaAspect aspect;

    public TypedManaCrystal(ManaAspect aspect) {
        super(false); // 父类的 refined 标志不使用，只是借其结构 + ITEMS 注册
        this.aspect = aspect;
    }

    public ManaAspect getAspect() {
        return aspect;
    }

    @Override
    public int getCrystalValue() {
        return aspect.value;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return aspect.foilGlow;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return aspect.rarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        // 不调用 super — super 用 raw/refined 双态 tooltip key, 此处需要 aspect-specific
        tooltip.add(Component.translatable("tooltip.transcend.crystal_" + aspect.id + ".desc")
                .withStyle(aspect.nameColor));
        tooltip.add(Component.translatable("tooltip.transcend.crystal.value", aspect.value)
                .withStyle(ChatFormatting.GRAY));
    }
}
