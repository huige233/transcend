package com.huige233.transcend;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/** 注册可同步的超越伤害属性，并将其附加到玩家实体。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class TranscendAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Transcend.MODID);
    public static final RegistryObject<Attribute> TRANSCEND_DAMAGE = ATTRIBUTES.register("transcend.transcend_damage",
            () -> new RangedAttribute("transcend.transcend_damage", 0.0D, 0.0D, 1024.0D).setSyncable(true));

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, TRANSCEND_DAMAGE.get());
    }

}
