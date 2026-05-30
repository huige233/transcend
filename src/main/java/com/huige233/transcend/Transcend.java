package com.huige233.transcend;

import com.huige233.transcend.client.RainbowLightningRenderer;
import com.huige233.transcend.entity.SpellGuardian;
import com.huige233.transcend.entity.SpellWisp;
import com.huige233.transcend.spell.SpellProjectileRenderer;
import com.huige233.transcend.block.SpellWorkbenchMenu;
import com.huige233.transcend.client.SpellWorkbenchScreen;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Transcend.MODID)
public class Transcend {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "transcend";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }


    public Transcend() {
        TranscendGameRules.init();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        //modEventBus.addListener(ModSetup::registerEvents);
        ModSetup.registers(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        com.huige233.transcend.init.ModEffects.register(modEventBus);
        com.huige233.transcend.loot.ModLootModifiers.register(modEventBus);
        com.huige233.transcend.world.structure.ModStructures.register(modEventBus);
        TranscendTab.register(modEventBus);
        // Round 43: 独立法术 Creative Tab
        TranscendSpellTab.register(modEventBus);
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(com.huige233.transcend.mana.ManaHandlerCapability::register);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.ascension.tree.TreeRegistry::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.ascension.tree.TreeRegistry::onPlayerLogin);
        // Round 02: 数据驱动法术定义注册表 (data/<ns>/spells/*.json)
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.spell.data.SpellDefinitionLoader::onAddReloadListeners);
        // Round 03: 数据驱动法环结构追加 (data/<ns>/circle_patterns/*.json)
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.circle.data.CirclePatternAdditionLoader::onAddReloadListeners);
        // Round 10: 数据驱动元素数值覆盖 (data/<ns>/element_stats/*.json)
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.spell.data.ElementStatsLoader::onAddReloadListeners);
        // Round 11: 数据驱动 carrier/effect 数值覆盖
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.spell.data.CarrierStatsLoader::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.spell.data.EffectStatsLoader::onAddReloadListeners);
        // Round 35: 数值平衡数据驱动 (data/<ns>/balance/values.json) - R19-R30 全表降档
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.balance.BalanceLoader::onAddReloadListeners);
        // Round 58: 数据驱动 mana_blossom 转化表 + mana_dew 产能配置
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.block.data.BlossomTransformLoader::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.block.data.DewProductionLoader::onAddReloadListeners);
        // Round 69: 数据驱动 ascension_anchor 图案配置（4 进阶仪式的 N/R/mana 可调）
        MinecraftForge.EVENT_BUS.addListener(com.huige233.transcend.block.ascension.AscensionPatternLoader::onAddReloadListeners);


        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(com.huige233.transcend.ritual.RitualRegistry::init);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SPELL_WISP.get(), SpellWisp.createAttributes().build());
        event.put(ModEntities.SPELL_GUARDIAN.get(), SpellGuardian.createAttributes().build());
        event.put(ModEntities.ELEMENTAL_WARDEN.get(), com.huige233.transcend.entity.boss.ElementalWarden.createAttributes().build());
        event.put(ModEntities.VOID_WEAVER.get(), com.huige233.transcend.entity.boss.VoidWeaver.createAttributes().build());
        event.put(ModEntities.TRANSCENDENCE_AVATAR.get(), com.huige233.transcend.entity.boss.TranscendenceAvatar.createAttributes().build());
        event.put(ModEntities.SPELL_PILLAR.get(), com.huige233.transcend.entity.SpellPillar.createAttributes().build());
        event.put(ModEntities.TEST_DUMMY.get(), com.huige233.transcend.entity.TestDummy.createAttributes().build());
        event.put(ModEntities.NEXUS_GUARDIAN.get(), com.huige233.transcend.entity.nexus.NexusGuardian.createAttributes().build());
        event.put(ModEntities.NEXUS_SENTINEL.get(), com.huige233.transcend.entity.nexus.NexusSentinel.createAttributes().build());
        event.put(ModEntities.NEXUS_CRYSTAL.get(), com.huige233.transcend.entity.nexus.NexusCrystalEntity.createAttributes().build());
        event.put(ModEntities.FAMILIAR.get(), com.huige233.transcend.entity.familiar.TranscendFamiliar.createAttributes().build());
    }



    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            event.enqueueWork(ModItemProperties::register);
            event.enqueueWork(() -> MenuScreens.register(ModMenus.SPELL_WORKBENCH_MENU.get(), SpellWorkbenchScreen::new));
            event.enqueueWork(() -> MenuScreens.register(ModMenus.CIRCLE_CORE_MENU.get(), com.huige233.transcend.client.circle.CircleCoreScreen::new));
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.RAINBOW_LIGHTNING.get(), RainbowLightningRenderer::new);
            event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(),
                    com.huige233.transcend.client.renderer.SpellProjectileShaderRenderer::new);
            event.registerEntityRenderer(ModEntities.SPELL_WISP.get(),
                    com.huige233.transcend.client.renderer.SpellWispRenderer::new);
            event.registerEntityRenderer(ModEntities.SPELL_GUARDIAN.get(),
                    com.huige233.transcend.client.renderer.SpellGuardianRenderer::new);
            event.registerEntityRenderer(ModEntities.SPELL_PILLAR.get(),
                    com.huige233.transcend.client.renderer.SpellPillarRenderer::new);
            event.registerEntityRenderer(ModEntities.TEST_DUMMY.get(),
                    com.huige233.transcend.client.renderer.TestDummyRenderer::new);

            // Nexus entities — model-based renderers
            event.registerEntityRenderer(ModEntities.NEXUS_GUARDIAN.get(),
                    com.huige233.transcend.client.renderer.NexusGuardianRenderer::new);
            event.registerEntityRenderer(ModEntities.NEXUS_SENTINEL.get(),
                    com.huige233.transcend.client.renderer.NexusSentinelRenderer::new);
            event.registerEntityRenderer(ModEntities.NEXUS_CRYSTAL.get(),
                    com.huige233.transcend.client.renderer.NexusCrystalRenderer::new);
            event.registerEntityRenderer(ModEntities.FAMILIAR.get(),
                    com.huige233.transcend.client.renderer.TranscendFamiliarRenderer::new);

            // 法环核心方块实体渲染器
            event.registerBlockEntityRenderer(ModBlockEntities.CIRCLE_CORE_BE.get(),
                    com.huige233.transcend.client.circle.CircleCoreRenderer::new);

            // R62: 魔力传输水晶 Beam 渲染器（链接水晶之间的激光束）
            event.registerBlockEntityRenderer(ModBlockEntities.MANA_TRANSMIT_CRYSTAL_BE.get(),
                    com.huige233.transcend.client.mana.ManaTransmitCrystalRenderer::new);

            // R81: 魔力储液池 — 池面漂浮魔力水晶
            event.registerBlockEntityRenderer(ModBlockEntities.MANA_RESERVOIR_BE.get(),
                    com.huige233.transcend.client.renderer.ManaReservoirRenderer::new);

            event.registerEntityRenderer(ModEntities.ELEMENTAL_WARDEN.get(), ctx ->
                    new com.huige233.transcend.client.renderer.TranscendBossRenderer<>(ctx,
                            new com.huige233.transcend.client.model.TranscendBossModel<>(
                                    ctx.bakeLayer(com.huige233.transcend.client.model.TranscendBossModel.WARDEN_LAYER), false),
                            new ResourceLocation(MODID, "textures/entity/elemental_warden.png"),
                            new ResourceLocation(MODID, "textures/entity/elemental_warden_glow.png")));

            event.registerEntityRenderer(ModEntities.VOID_WEAVER.get(), ctx ->
                    new com.huige233.transcend.client.renderer.TranscendBossRenderer<>(ctx,
                            new com.huige233.transcend.client.model.TranscendBossModel<>(
                                    ctx.bakeLayer(com.huige233.transcend.client.model.TranscendBossModel.WEAVER_LAYER), false),
                            new ResourceLocation(MODID, "textures/entity/void_weaver.png"),
                            new ResourceLocation(MODID, "textures/entity/void_weaver_glow.png")));

            event.registerEntityRenderer(ModEntities.TRANSCENDENCE_AVATAR.get(), ctx ->
                    new com.huige233.transcend.client.renderer.TranscendBossRenderer<>(ctx,
                            new com.huige233.transcend.client.model.TranscendBossModel<>(
                                    ctx.bakeLayer(com.huige233.transcend.client.model.TranscendBossModel.AVATAR_LAYER), true),
                            new ResourceLocation(MODID, "textures/entity/transcendence_avatar.png"),
                            new ResourceLocation(MODID, "textures/entity/transcendence_avatar_glow.png")));
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(com.huige233.transcend.client.model.TranscendBossModel.WARDEN_LAYER,
                    com.huige233.transcend.client.model.TranscendBossModel::createWardenMesh);
            event.registerLayerDefinition(com.huige233.transcend.client.model.TranscendBossModel.WEAVER_LAYER,
                    com.huige233.transcend.client.model.TranscendBossModel::createWeaverMesh);
            event.registerLayerDefinition(com.huige233.transcend.client.model.TranscendBossModel.AVATAR_LAYER,
                    com.huige233.transcend.client.model.TranscendBossModel::createAvatarMesh);
            event.registerLayerDefinition(com.huige233.transcend.client.model.TestDummyModel.LAYER,
                    com.huige233.transcend.client.model.TestDummyModel::createBodyLayer);
            event.registerLayerDefinition(com.huige233.transcend.client.model.NexusGuardianModel.LAYER,
                    com.huige233.transcend.client.model.NexusGuardianModel::createBodyLayer);
            event.registerLayerDefinition(com.huige233.transcend.client.model.NexusSentinelModel.LAYER,
                    com.huige233.transcend.client.model.NexusSentinelModel::createBodyLayer);
            // R93: 5 new entity model layers
            event.registerLayerDefinition(com.huige233.transcend.client.model.SpellWispModel.LAYER,
                    com.huige233.transcend.client.model.SpellWispModel::createBodyLayer);
            event.registerLayerDefinition(com.huige233.transcend.client.model.SpellGuardianModel.LAYER,
                    com.huige233.transcend.client.model.SpellGuardianModel::createBodyLayer);
            event.registerLayerDefinition(com.huige233.transcend.client.model.SpellPillarModel.LAYER,
                    com.huige233.transcend.client.model.SpellPillarModel::createBodyLayer);
            event.registerLayerDefinition(com.huige233.transcend.client.model.NexusCrystalModel.LAYER,
                    com.huige233.transcend.client.model.NexusCrystalModel::createBodyLayer);
            event.registerLayerDefinition(com.huige233.transcend.client.model.TranscendFamiliarModel.LAYER,
                    com.huige233.transcend.client.model.TranscendFamiliarModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.TRANSCEND_DUST.get(), TranscendDustParticle.Provider::new);
            event.registerSpriteSet(ModParticles.TRANSCEND_RUNE.get(), TranscendRuneParticle.Provider::new);
            event.registerSpriteSet(ModParticles.TRANSCEND_GLITTER.get(), TranscendGlitterParticle.Provider::new);
        }
    }
}
