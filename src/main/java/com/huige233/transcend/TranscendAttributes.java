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

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Transcend.MODID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class TranscendAttributes {
    public static final HashMap<RegistryObject<Attribute>, UUID> UUIDS = new HashMap<>();
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Transcend.MODID);
    public static final RegistryObject<Attribute> TRANSCEND_DAMAGE = registerAttribute("transcend.transcend_damage", (id) -> new RangedAttribute(id, 0.0D, 0.0D, 1024.0D).setSyncable(true), "949c8758-de15-44dd-a17c-ac44aa0a6b27");

    public static final RegistryObject<Attribute> SPELL_POWER = registerAttribute(
            "transcend.spell_power",
            (id) -> new RangedAttribute(id, 0.0D, 0.0D, 10.0D).setSyncable(true),
            "11111111-2222-3333-4444-555555555501");

    public static final RegistryObject<Attribute> SPELL_RESIST = registerAttribute(
            "transcend.spell_resist",
            (id) -> new RangedAttribute(id, 0.0D, 0.0D, 0.95D).setSyncable(true),
            "11111111-2222-3333-4444-555555555502");

    public static final RegistryObject<Attribute> MAX_MANA = registerAttribute(
            "transcend.max_mana",
            (id) -> new RangedAttribute(id, 0.0D, 0.0D, 65536.0D).setSyncable(true),
            "11111111-2222-3333-4444-555555555503");

    public static final RegistryObject<Attribute> MANA_REGEN = registerAttribute(
            "transcend.mana_regen",
            (id) -> new RangedAttribute(id, 0.1D, 0.0D, 100.0D).setSyncable(true),
            "11111111-2222-3333-4444-555555555504");

    public static final RegistryObject<Attribute> CRIT_CHANCE = registerAttribute(
            "transcend.crit_chance",
            (id) -> new RangedAttribute(id, 0.0D, 0.0D, 0.80D).setSyncable(true),
            "11111111-2222-3333-4444-555555555505");

    public static final RegistryObject<Attribute> COOLDOWN_REDUCTION = registerAttribute(
            "transcend.cooldown_reduction",
            (id) -> new RangedAttribute(id, 0.0D, 0.0D, 0.75D).setSyncable(true),
            "11111111-2222-3333-4444-555555555506");

    public static RegistryObject<Attribute> registerAttribute(String name, Function<String, Attribute> attribute, String uuid) {
        return registerAttribute(name, attribute, UUID.fromString(uuid));
    }

    public static RegistryObject<Attribute> registerAttribute(String name, Function<String, Attribute> attribute, UUID uuid) {
        RegistryObject<Attribute> registryObject = ATTRIBUTES.register(name, () -> attribute.apply(name));
        UUIDS.put(registryObject, uuid);
        return registryObject;
    }

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().stream().filter(e -> e == EntityType.PLAYER).forEach(e -> {
            ATTRIBUTES.getEntries().forEach((v) -> {
                event.add(e, v.get());
            });
        });
    }

}
