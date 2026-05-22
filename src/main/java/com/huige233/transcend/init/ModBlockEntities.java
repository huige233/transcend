package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.block.NexusCoreBlockEntity;
import com.huige233.transcend.block.ManaWellBlockEntity;
import com.huige233.transcend.block.RitualAltarBlockEntity;
import com.huige233.transcend.block.RitualPedestalBlockEntity;
import com.huige233.transcend.block.SpellWorkbenchBlockEntity;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.block.mana.ManaReservoirBlockEntity;
import com.huige233.transcend.block.mana.ManaSpreaderBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Transcend.MODID);

    public static final RegistryObject<BlockEntityType<SpellWorkbenchBlockEntity>> SPELL_WORKBENCH_BE =
            BLOCK_ENTITIES.register("spell_workbench_be",
                    () -> BlockEntityType.Builder.of(SpellWorkbenchBlockEntity::new,
                            ModBlocks.SPELL_WORKBENCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<RitualAltarBlockEntity>> RITUAL_ALTAR_BE =
            BLOCK_ENTITIES.register("ritual_altar_be",
                    () -> BlockEntityType.Builder.of(RitualAltarBlockEntity::new,
                            ModBlocks.RITUAL_ALTAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<RitualPedestalBlockEntity>> RITUAL_PEDESTAL_BE =
            BLOCK_ENTITIES.register("ritual_pedestal_be",
                    () -> BlockEntityType.Builder.of(RitualPedestalBlockEntity::new,
                            ModBlocks.RITUAL_PEDESTAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<NexusCoreBlockEntity>> NEXUS_CORE_BE =
            BLOCK_ENTITIES.register("nexus_core_be",
                    () -> BlockEntityType.Builder.of(NexusCoreBlockEntity::new,
                            ModBlocks.NEXUS_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ManaWellBlockEntity>> MANA_WELL_BE =
            BLOCK_ENTITIES.register("mana_well_be",
                    () -> BlockEntityType.Builder.of(ManaWellBlockEntity::new,
                            ModBlocks.MANA_WELL.get()).build(null));

    // === 法环核心方块实体 ===
    public static final RegistryObject<BlockEntityType<MagicCircleCoreBlockEntity>> CIRCLE_CORE_BE =
            BLOCK_ENTITIES.register("circle_core_be",
                    () -> BlockEntityType.Builder.of(MagicCircleCoreBlockEntity::new,
                            ModBlocks.CIRCLE_CORE_DORMANT.get(),
                            ModBlocks.CIRCLE_CORE_WELLSPRING.get(),
                            ModBlocks.CIRCLE_CORE_SANCTUARY.get(),
                            ModBlocks.CIRCLE_CORE_DOMINION.get(),
                            ModBlocks.CIRCLE_CORE_WAYSTONE.get(),
                            ModBlocks.CIRCLE_CORE_CONVERGENCE.get(),
                            ModBlocks.CIRCLE_CORE_PRIMORDIAL.get()).build(null));

    // === 魔力储液池方块实体 ===
    public static final RegistryObject<BlockEntityType<ManaReservoirBlockEntity>> MANA_RESERVOIR_BE =
            BLOCK_ENTITIES.register("mana_reservoir_be",
                    () -> BlockEntityType.Builder.of(ManaReservoirBlockEntity::new,
                            ModBlocks.MANA_RESERVOIR.get(),
                            ModBlocks.GREATER_MANA_RESERVOIR.get()).build(null));

    // === 魔力散发器方块实体 ===
    public static final RegistryObject<BlockEntityType<ManaSpreaderBlockEntity>> MANA_SPREADER_BE =
            BLOCK_ENTITIES.register("mana_spreader_be",
                    () -> BlockEntityType.Builder.of(ManaSpreaderBlockEntity::new,
                            ModBlocks.MANA_SPREADER.get()).build(null));

    // === Round 42: 魔力传输水晶 BE ===
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.mana.ManaTransmitCrystalBlockEntity>> MANA_TRANSMIT_CRYSTAL_BE =
            BLOCK_ENTITIES.register("mana_transmit_crystal_be",
                    () -> BlockEntityType.Builder.of(
                            com.huige233.transcend.block.mana.ManaTransmitCrystalBlockEntity::new,
                            ModBlocks.MANA_TRANSMIT_CRYSTAL.get()).build(null));

    // === Round 50: Mana Blossom BE (Pure Daisy 风) ===
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.ManaBlossomBlockEntity>> MANA_BLOSSOM_BE =
            BLOCK_ENTITIES.register("mana_blossom_be",
                    () -> BlockEntityType.Builder.of(
                            com.huige233.transcend.block.ManaBlossomBlockEntity::new,
                            ModBlocks.MANA_BLOSSOM.get()).build(null));

    // === Round 52: Mana Dew BE (水面被动产 mana) ===
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.ManaDewBlockEntity>> MANA_DEW_BE =
            BLOCK_ENTITIES.register("mana_dew_be",
                    () -> BlockEntityType.Builder.of(
                            com.huige233.transcend.block.ManaDewBlockEntity::new,
                            ModBlocks.MANA_DEW.get()).build(null));

    // === Round 21: Typed Mana Conduit BE ===
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.mana.ManaConduitBlockEntity>> MANA_CONDUIT_BE =
            BLOCK_ENTITIES.register("mana_conduit_be",
                    () -> BlockEntityType.Builder.of(
                            com.huige233.transcend.block.mana.ManaConduitBlockEntity::new,
                            ModBlocks.MANA_CONDUIT.get()).build(null));

    // === Round 22: Functional Mana BE (4 blocks, 1 BE type) ===
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.mana.FunctionalManaBlockEntity>> FUNCTIONAL_MANA_BE =
            BLOCK_ENTITIES.register("functional_mana_be",
                    () -> BlockEntityType.Builder.of(
                            com.huige233.transcend.block.mana.FunctionalManaBlockEntity::new,
                            ModBlocks.MANA_FURNACE.get(),
                            ModBlocks.MANA_SENTINEL.get(),
                            ModBlocks.MANA_HARVESTER.get(),
                            ModBlocks.MANA_GENERATOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
