package com.huige233.transcend.items.tech;

import net.minecraft.world.item.Item;


/** 按插件等级提供研究站能量输入速率和研究点转换加成。 */
public final class ResearchPluginItem extends Item {
    private final int tier;
    public ResearchPluginItem(int tier) {
        super(new Properties().stacksTo(1));
        this.tier = Math.max(1, tier);
    }
    public int tier() { return tier; }
    public long inputRateBonus() { return 100L * tier; }
    public long conversionBonus() { return tier; }
}
