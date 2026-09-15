package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/** 注册装配、研究、黑洞培育、发电及分级储能设备的方块实例。 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Transcend.MODID);

    public static final RegistryObject<Block> ASSEMBLY = BLOCKS.register("assembly_table",
            com.huige233.transcend.block.AssemblyBlock::new);
    public static final RegistryObject<Block> MINI_UNIVERSE_GENERATOR = BLOCKS.register("mini_universe_generator",
            com.huige233.transcend.block.MiniUniverseGeneratorBlock::new);
    public static final RegistryObject<Block> BLACK_HOLE_SEED_BREEDER = BLOCKS.register("black_hole_seed_breeder",
            com.huige233.transcend.block.BlackHoleSeedBreederBlock::new);
    public static final RegistryObject<Block> FIRE_GENERATOR = BLOCKS.register("fire_generator", () -> new com.huige233.transcend.block.FEGeneratorBlock(com.huige233.transcend.block.FEGeneratorBlockEntity.Mode.FIRE, 40, net.minecraft.world.level.material.MapColor.COLOR_RED));
    public static final RegistryObject<Block> WIND_GENERATOR = BLOCKS.register("wind_generator", () -> new com.huige233.transcend.block.FEGeneratorBlock(com.huige233.transcend.block.FEGeneratorBlockEntity.Mode.WIND, 80, net.minecraft.world.level.material.MapColor.COLOR_BLUE));
    public static final RegistryObject<Block> CREATIVE_GENERATOR = BLOCKS.register("creative_generator", () -> new com.huige233.transcend.block.FEGeneratorBlock(com.huige233.transcend.block.FEGeneratorBlockEntity.Mode.CREATIVE, 160, net.minecraft.world.level.material.MapColor.COLOR_PURPLE));

    public static final RegistryObject<Block> RESEARCH_STATION = BLOCKS.register("research_station", com.huige233.transcend.block.ResearchStationBlock::new);
    public static final RegistryObject<Block> RESEARCH_PROCESSOR = BLOCKS.register("research_processor", () -> new com.huige233.transcend.block.ResearchPluginBlock(10));
    public static final RegistryObject<Block> RESEARCH_QUANTUM_COMPUTER = BLOCKS.register("research_quantum_computer", () -> new com.huige233.transcend.block.ResearchPluginBlock(1_000));
    public static final RegistryObject<Block> RESEARCH_COSMIC_SIMULATOR = BLOCKS.register("research_cosmic_simulator", () -> new com.huige233.transcend.block.ResearchPluginBlock(1_000_000));
    public static final RegistryObject<Block> LONG_STORAGE_BASIC = BLOCKS.register("long_storage_basic", () -> new com.huige233.transcend.block.LongStorageBlock(100_000_000L, net.minecraft.world.level.material.MapColor.METAL));
    public static final RegistryObject<Block> LONG_STORAGE_ADVANCED = BLOCKS.register("long_storage_advanced", () -> new com.huige233.transcend.block.LongStorageBlock(10_000_000_000L, net.minecraft.world.level.material.MapColor.COLOR_BLUE));
    public static final RegistryObject<Block> LONG_STORAGE_QUANTUM = BLOCKS.register("long_storage_quantum", () -> new com.huige233.transcend.block.LongStorageBlock(1_000_000_000_000_000L, net.minecraft.world.level.material.MapColor.COLOR_CYAN));
    public static final RegistryObject<Block> LONG_STORAGE_PHANTOM = BLOCKS.register("long_storage_phantom", () -> new com.huige233.transcend.block.LongStorageBlock(Long.MAX_VALUE, net.minecraft.world.level.material.MapColor.COLOR_PURPLE));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
