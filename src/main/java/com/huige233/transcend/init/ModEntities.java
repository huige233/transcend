package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.entity.RainbowLightning;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/** 注册彩虹闪电、测试假人和粒子弹的实体类型及尺寸与跟踪参数。 */
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

    public static final RegistryObject<EntityType<com.huige233.transcend.entity.TestDummy>> TEST_DUMMY =
            ENTITY_TYPES.register("test_dummy",
                    () -> EntityType.Builder.<com.huige233.transcend.entity.TestDummy>of(com.huige233.transcend.entity.TestDummy::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("test_dummy"));

    public static final RegistryObject<EntityType<com.huige233.transcend.entity.projectile.ParticleBolt>> PARTICLE_BOLT =
            ENTITY_TYPES.register("particle_bolt",
                    () -> EntityType.Builder.<com.huige233.transcend.entity.projectile.ParticleBolt>of(
                                    com.huige233.transcend.entity.projectile.ParticleBolt::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("particle_bolt"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}