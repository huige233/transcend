package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/** 注册装配台、研究站、发电设备和储能设备的方块实体类型及其适用方块。 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Transcend.MODID);

    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.AssemblyBlockEntity>> ASSEMBLY =
            BLOCK_ENTITIES.register("assembly_table", () -> BlockEntityType.Builder.of(
                    com.huige233.transcend.block.AssemblyBlockEntity::new, ModBlocks.ASSEMBLY.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.MiniUniverseGeneratorBlockEntity>> MINI_UNIVERSE_GENERATOR =
            BLOCK_ENTITIES.register("mini_universe_generator", () -> BlockEntityType.Builder.of(
                    com.huige233.transcend.block.MiniUniverseGeneratorBlockEntity::new, ModBlocks.MINI_UNIVERSE_GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.BlackHoleSeedBreederBlockEntity>> BLACK_HOLE_SEED_BREEDER =
            BLOCK_ENTITIES.register("black_hole_seed_breeder", () -> BlockEntityType.Builder.of(
                    com.huige233.transcend.block.BlackHoleSeedBreederBlockEntity::new, ModBlocks.BLACK_HOLE_SEED_BREEDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.FEGeneratorBlockEntity.Generator>> FIRE_GENERATOR = BLOCK_ENTITIES.register("fire_generator", () -> BlockEntityType.Builder.of((pos, state) -> new com.huige233.transcend.block.FEGeneratorBlockEntity.Generator(ModBlockEntities.FIRE_GENERATOR.get(), pos, state, 40, com.huige233.transcend.block.FEGeneratorBlockEntity.Mode.FIRE), ModBlocks.FIRE_GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.FEGeneratorBlockEntity.Generator>> WIND_GENERATOR = BLOCK_ENTITIES.register("wind_generator", () -> BlockEntityType.Builder.of((pos, state) -> new com.huige233.transcend.block.FEGeneratorBlockEntity.Generator(ModBlockEntities.WIND_GENERATOR.get(), pos, state, 80, com.huige233.transcend.block.FEGeneratorBlockEntity.Mode.WIND), ModBlocks.WIND_GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.FEGeneratorBlockEntity.Generator>> CREATIVE_GENERATOR = BLOCK_ENTITIES.register("creative_generator", () -> BlockEntityType.Builder.of((pos, state) -> new com.huige233.transcend.block.FEGeneratorBlockEntity.Generator(ModBlockEntities.CREATIVE_GENERATOR.get(), pos, state, 160, com.huige233.transcend.block.FEGeneratorBlockEntity.Mode.CREATIVE), ModBlocks.CREATIVE_GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.ResearchStationBlockEntity>> RESEARCH_STATION = BLOCK_ENTITIES.register("research_station", () -> BlockEntityType.Builder.of(com.huige233.transcend.block.ResearchStationBlockEntity::new, ModBlocks.RESEARCH_STATION.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.huige233.transcend.block.LongStorageBlockEntity>> LONG_STORAGE = BLOCK_ENTITIES.register("long_storage", () -> BlockEntityType.Builder.of((pos, state) -> new com.huige233.transcend.block.LongStorageBlockEntity(pos, state, state.getBlock() instanceof com.huige233.transcend.block.LongStorageBlock b ? b.capacity() : 0L), ModBlocks.LONG_STORAGE_BASIC.get(), ModBlocks.LONG_STORAGE_ADVANCED.get(), ModBlocks.LONG_STORAGE_QUANTUM.get(), ModBlocks.LONG_STORAGE_PHANTOM.get()).build(null));
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
