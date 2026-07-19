package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.entity.RainbowLightning;
import com.huige233.transcend.entity.SpellWisp;
import com.huige233.transcend.entity.SpellGuardian;
import com.huige233.transcend.entity.nexus.NexusCrystalEntity;
import com.huige233.transcend.entity.nexus.NexusGuardian;
import com.huige233.transcend.entity.nexus.NexusSentinel;
import com.huige233.transcend.entity.boss.ElementalWarden;
import com.huige233.transcend.entity.boss.VoidWeaver;
import com.huige233.transcend.entity.boss.TranscendenceAvatar;
import com.huige233.transcend.spell.SpellProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Transcend.MODID);

    public static final RegistryObject<EntityType<RainbowLightning>> RAINBOW_LIGHTNING =
            ENTITY_TYPES.register("rainbow_lightning",
                    () -> EntityType.Builder.<RainbowLightning>of(RainbowLightning::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(16)
                            .updateInterval(Integer.MAX_VALUE)
                            .build("rainbow_lightning"));

    public static final RegistryObject<EntityType<SpellProjectile>> SPELL_PROJECTILE =
            ENTITY_TYPES.register("spell_projectile",
                    () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("spell_projectile"));

    public static final RegistryObject<EntityType<SpellWisp>> SPELL_WISP =
            ENTITY_TYPES.register("spell_wisp",
                    () -> EntityType.Builder.<SpellWisp>of(SpellWisp::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("spell_wisp"));

    public static final RegistryObject<EntityType<SpellGuardian>> SPELL_GUARDIAN =
            ENTITY_TYPES.register("spell_guardian",
                    () -> EntityType.Builder.<SpellGuardian>of(SpellGuardian::new, MobCategory.MISC)
                            .sized(0.8F, 1.8F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("spell_guardian"));

    public static final RegistryObject<EntityType<ElementalWarden>> ELEMENTAL_WARDEN =
            ENTITY_TYPES.register("elemental_warden",
                    () -> EntityType.Builder.<ElementalWarden>of(ElementalWarden::new, MobCategory.MONSTER)
                            .sized(1.0F, 2.4F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                            .fireImmune()
                            .build("elemental_warden"));

    public static final RegistryObject<EntityType<VoidWeaver>> VOID_WEAVER =
            ENTITY_TYPES.register("void_weaver",
                    () -> EntityType.Builder.<VoidWeaver>of(VoidWeaver::new, MobCategory.MONSTER)
                            .sized(1.0F, 2.6F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                            .fireImmune()
                            .build("void_weaver"));

    public static final RegistryObject<EntityType<TranscendenceAvatar>> TRANSCENDENCE_AVATAR =
            ENTITY_TYPES.register("transcendence_avatar",
                    () -> EntityType.Builder.<TranscendenceAvatar>of(TranscendenceAvatar::new, MobCategory.MONSTER)
                            .sized(1.2F, 3.0F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                            .fireImmune()
                            .build("transcendence_avatar"));

    public static final RegistryObject<EntityType<com.huige233.transcend.entity.SpellPillar>> SPELL_PILLAR =
            ENTITY_TYPES.register("spell_pillar",
                    () -> EntityType.Builder.<com.huige233.transcend.entity.SpellPillar>of(com.huige233.transcend.entity.SpellPillar::new, MobCategory.MISC)
                            .sized(0.8F, 3.0F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .fireImmune()
                            .build("spell_pillar"));

    public static final RegistryObject<EntityType<com.huige233.transcend.entity.TestDummy>> TEST_DUMMY =
            ENTITY_TYPES.register("test_dummy",
                    () -> EntityType.Builder.<com.huige233.transcend.entity.TestDummy>of(com.huige233.transcend.entity.TestDummy::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("test_dummy"));

    public static final RegistryObject<EntityType<NexusGuardian>> NEXUS_GUARDIAN =
            ENTITY_TYPES.register("nexus_guardian",
                    () -> EntityType.Builder.<NexusGuardian>of(NexusGuardian::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.0F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .fireImmune()
                            .build("nexus_guardian"));

    public static final RegistryObject<EntityType<NexusSentinel>> NEXUS_SENTINEL =
            ENTITY_TYPES.register("nexus_sentinel",
                    () -> EntityType.Builder.<NexusSentinel>of(NexusSentinel::new, MobCategory.MONSTER)
                            .sized(0.6F, 0.6F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .fireImmune()
                            .build("nexus_sentinel"));

    public static final RegistryObject<EntityType<NexusCrystalEntity>> NEXUS_CRYSTAL =
            ENTITY_TYPES.register("nexus_crystal",
                    () -> EntityType.Builder.<NexusCrystalEntity>of(NexusCrystalEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.5F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                            .fireImmune()
                            .build("nexus_crystal"));

    public static final RegistryObject<EntityType<com.huige233.transcend.entity.familiar.TranscendFamiliar>> FAMILIAR =
            ENTITY_TYPES.register("familiar",
                    () -> EntityType.Builder.<com.huige233.transcend.entity.familiar.TranscendFamiliar>of(
                                    com.huige233.transcend.entity.familiar.TranscendFamiliar::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                            .build("familiar"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
