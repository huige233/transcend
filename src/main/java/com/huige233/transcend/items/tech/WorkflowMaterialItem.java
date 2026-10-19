package com.huige233.transcend.items.tech;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class WorkflowMaterialItem extends Item {
    private final int tier, color;
    public WorkflowMaterialItem(int tier, int color) { super(new Properties()); this.tier = tier; this.color = color; }
    public int tier() { return tier; }
    public int color() { return color; }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.material_tier", tier));
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath();
        if (path.startsWith("tech_metal_")) tooltip.add(Component.translatable("tooltip.transcend.generator_upgrade", tier,
                com.huige233.transcend.tech.assembly.AssemblyRules.generatorUpgradeCost(tier)));
    }
}
