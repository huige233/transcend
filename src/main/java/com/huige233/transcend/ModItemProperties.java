package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.curio.TheLastTotem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** 物品属性机制注册（药水/书栏 NBT 属性等客户端钩子）。 */
public class ModItemProperties {

    public static void register() {
        ItemProperties.register(
                ModItems.thelasttotem.get(),
                new ResourceLocation("transcend", "opt"),
                (ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) -> {
                    return TheLastTotem.isOpt(stack) ? 1.0F : 0.0F;
                }
        );
    }
}
