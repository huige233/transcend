package com.huige233.transcend.loot;

import com.huige233.transcend.Transcend;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 全局战利品修饰器注册。
 * 实际的战利品条目通过 data/transcend/loot_modifiers/ 下的 JSON 配置。
 */
public class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Transcend.MODID);

    public static final RegistryObject<Codec<SpellComponentLootModifier>> SPELL_COMPONENT =
            LOOT_MODIFIERS.register("spell_component",
                    () -> SpellComponentLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}
