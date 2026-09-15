package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.curio.TheLastTotem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;


/** 注册枪械模块、护盾模块和最终图腾的客户端模型属性判定器。 */
public class ModItemProperties {

    public static void register() {
        ItemProperties.register(ModItems.shield_module.get(), Transcend.rl("shield_module"),
                (stack, level, entity, seed) -> {
                    var module = com.huige233.transcend.items.tech.ShieldModuleItem.getModule(stack);
                    return module == null ? 0.0F : module.ordinal() + 1.0F;
                });

        ItemProperties.register(ModItems.gun_module.get(), Transcend.rl("module"),
                (stack, level, entity, seed) -> {
                    var module = com.huige233.transcend.items.tech.GunModuleItem.getModule(stack);
                    return module == null ? 0.0F : module.modelIndex();
                });

        ItemProperties.register(
                ModItems.thelasttotem.get(),
                new ResourceLocation("transcend", "opt"),
                (ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) -> {
                    return TheLastTotem.isOpt(stack) ? 1.0F : 0.0F;
                }
        );
    }
}
