package com.huige233.transcend;

import com.huige233.transcend.client.RainbowLightningRenderer;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.init.ModMenus;
import com.huige233.transcend.init.ModParticles;
import com.huige233.transcend.particle.TranscendDustParticle;
import com.huige233.transcend.particle.TranscendGlitterParticle;
import com.huige233.transcend.particle.TranscendRuneParticle;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** 作为模组入口统一注册内容、配置和事件处理器，并初始化实体属性及装备联动功能。 */
@Mod(Transcend.MODID)

public class Transcend {

    public static final String MODID = "transcend";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public Transcend() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModSetup.registers(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        com.huige233.transcend.tech.assembly.AssemblyRecipeRegistration.register(modEventBus);
        com.huige233.transcend.loot.ModLootModifiers.register(modEventBus);
        com.huige233.transcend.world.structure.ModStructures.register(modEventBus);
        TranscendTab.register(modEventBus);

        
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(com.huige233.transcend.util.TranscendLinkBoost.class);
        
        com.huige233.transcend.util.TranscendMagicBoost.init();
        
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(com.huige233.transcend.util.PhaseGuard.class);

        modEventBus.addListener(this::onEntityAttributeCreation);

        MinecraftForge.EVENT_BUS.register(com.huige233.transcend.handle.SiriusCombatHandler.class);
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, com.huige233.transcend.tech.TechConfig.SPEC,
                "transcend-tech.toml");
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TEST_DUMMY.get(), com.huige233.transcend.entity.TestDummy.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[Transcend] server starting");
    }

    /** 订阅客户端初始化事件以注册菜单界面、实体渲染器、模型层和粒子提供器。 */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ModItemProperties.register();
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.ASSEMBLY.get(),
                        com.huige233.transcend.client.AssemblyScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.BLACK_HOLE_SEED_BREEDER.get(),
                        com.huige233.transcend.client.BlackHoleSeedBreederScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.MINI_UNIVERSE_GENERATOR.get(),
                        com.huige233.transcend.client.MiniUniverseGeneratorScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.RESEARCH_STATION.get(),
                        com.huige233.transcend.client.ResearchStationScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.GENERATOR.get(),
                        com.huige233.transcend.client.GeneratorScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.LONG_STORAGE.get(),
                        com.huige233.transcend.client.LongStorageScreen::new);
            });
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.RAINBOW_LIGHTNING.get(), RainbowLightningRenderer::new);
            event.registerEntityRenderer(ModEntities.PARTICLE_BOLT.get(),
                    com.huige233.transcend.client.renderer.ParticleBoltRenderer::new);
            event.registerEntityRenderer(ModEntities.TEST_DUMMY.get(),
                    com.huige233.transcend.client.renderer.TestDummyRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(com.huige233.transcend.client.model.TestDummyModel.LAYER,
                    com.huige233.transcend.client.model.TestDummyModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.TRANSCEND_DUST.get(), TranscendDustParticle.Provider::new);
            event.registerSpriteSet(ModParticles.TRANSCEND_RUNE.get(), TranscendRuneParticle.Provider::new);
            event.registerSpriteSet(ModParticles.TRANSCEND_GLITTER.get(), TranscendGlitterParticle.Provider::new);
        }
    }
}