package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.block.AncientCrystalBlock;
import com.huige233.transcend.block.ConcentratedCrystalBlock;
import com.huige233.transcend.block.MagicCrystalBlock;
import com.huige233.transcend.block.NexusCoreBlock;
import com.huige233.transcend.block.ManaWellBlock;
import com.huige233.transcend.block.RitualAltarBlock;
import com.huige233.transcend.block.RitualPedestalBlock;
import com.huige233.transcend.block.SpellWorkbenchBlock;
import com.huige233.transcend.block.aether.AetherBlock;
import com.huige233.transcend.block.aether.AetherOreBlock;
import com.huige233.transcend.block.circle.*;
import com.huige233.transcend.block.decor.AetherGlassBlock;
import com.huige233.transcend.block.decor.ManaLanternBlock;
import com.huige233.transcend.block.mana.ManaReservoirBlock;
import com.huige233.transcend.block.mana.ManaSpreaderBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Transcend.MODID);

    public static final RegistryObject<Block> SPELL_WORKBENCH = BLOCKS.register("spell_workbench",
            SpellWorkbenchBlock::new);

    public static final RegistryObject<Block> RITUAL_ALTAR = BLOCKS.register("ritual_altar",
            RitualAltarBlock::new);

    public static final RegistryObject<Block> RITUAL_PEDESTAL = BLOCKS.register("ritual_pedestal",
            RitualPedestalBlock::new);

    public static final RegistryObject<Block> NEXUS_CORE = BLOCKS.register("nexus_core",
            NexusCoreBlock::new);

    public static final RegistryObject<Block> MANA_WELL = BLOCKS.register("mana_well",
            ManaWellBlock::new);

    public static final RegistryObject<Block> ANCIENT_CRYSTAL = BLOCKS.register("ancient_crystal",
            AncientCrystalBlock::new);

    public static final RegistryObject<Block> MAGIC_CRYSTAL_BLOCK = BLOCKS.register("magic_crystal_block",
            MagicCrystalBlock::new);

    public static final RegistryObject<Block> CONCENTRATED_CRYSTAL_BLOCK = BLOCKS.register("concentrated_crystal_block",
            ConcentratedCrystalBlock::new);

    // === Circle Structure Blocks ===

    // Foundation blocks (5 tiers)
    public static final RegistryObject<Block> ANCIENT_CIRCLE_STONE = BLOCKS.register("ancient_circle_stone",
            () -> new CircleFoundationBlock(1));
    public static final RegistryObject<Block> AWAKENED_CIRCLE_STONE = BLOCKS.register("awakened_circle_stone",
            () -> new CircleFoundationBlock(2));
    public static final RegistryObject<Block> ASTRAL_CIRCLE_STONE = BLOCKS.register("astral_circle_stone",
            () -> new CircleFoundationBlock(3));
    public static final RegistryObject<Block> NEXUS_CIRCLE_STONE = BLOCKS.register("nexus_circle_stone",
            () -> new CircleFoundationBlock(4));
    public static final RegistryObject<Block> PRIMORDIAL_CIRCLE_STONE = BLOCKS.register("primordial_circle_stone",
            () -> new CircleFoundationBlock(5));

    // Rune blocks (5 tiers)
    public static final RegistryObject<Block> LESSER_RUNE_STONE = BLOCKS.register("lesser_rune_stone",
            () -> new CircleRuneBlock(1));
    public static final RegistryObject<Block> AWAKENED_RUNE_STONE = BLOCKS.register("awakened_rune_stone",
            () -> new CircleRuneBlock(2));
    public static final RegistryObject<Block> GREATER_RUNE_STONE = BLOCKS.register("greater_rune_stone",
            () -> new CircleRuneBlock(3));
    public static final RegistryObject<Block> ARCHON_RUNE_STONE = BLOCKS.register("archon_rune_stone",
            () -> new CircleRuneBlock(4));
    public static final RegistryObject<Block> PRIMORDIAL_RUNE_STONE = BLOCKS.register("primordial_rune_stone",
            () -> new CircleRuneBlock(5));

    // Core blocks (7 types)
    public static final RegistryObject<Block> CIRCLE_CORE_DORMANT = BLOCKS.register("circle_core_dormant",
            () -> new MagicCircleCoreBlock("dormant"));
    public static final RegistryObject<Block> CIRCLE_CORE_WELLSPRING = BLOCKS.register("circle_core_wellspring",
            () -> new MagicCircleCoreBlock("wellspring"));
    public static final RegistryObject<Block> CIRCLE_CORE_SANCTUARY = BLOCKS.register("circle_core_sanctuary",
            () -> new MagicCircleCoreBlock("sanctuary"));
    public static final RegistryObject<Block> CIRCLE_CORE_DOMINION = BLOCKS.register("circle_core_dominion",
            () -> new MagicCircleCoreBlock("dominion"));
    public static final RegistryObject<Block> CIRCLE_CORE_WAYSTONE = BLOCKS.register("circle_core_waystone",
            () -> new MagicCircleCoreBlock("waystone"));
    public static final RegistryObject<Block> CIRCLE_CORE_CONVERGENCE = BLOCKS.register("circle_core_convergence",
            () -> new MagicCircleCoreBlock("convergence"));
    public static final RegistryObject<Block> CIRCLE_CORE_PRIMORDIAL = BLOCKS.register("circle_core_primordial",
            () -> new MagicCircleCoreBlock("primordial"));

    // Catalyst plinths
    public static final RegistryObject<Block> CATALYST_PLINTH = BLOCKS.register("catalyst_plinth",
            () -> new CatalystPlinthBlock(false));
    public static final RegistryObject<Block> SEALED_CATALYST_PLINTH = BLOCKS.register("sealed_catalyst_plinth",
            () -> new CatalystPlinthBlock(true));

    // Conduit blocks (4 tiers)
    public static final RegistryObject<Block> LEYLINE_CONDUIT_STONE = BLOCKS.register("leyline_conduit_stone",
            () -> new LeylineConduitBlock(2));
    public static final RegistryObject<Block> AETHER_CHANNEL_MARKER = BLOCKS.register("aether_channel_marker",
            () -> new LeylineConduitBlock(3));
    public static final RegistryObject<Block> NEXUS_CONDUIT_GATE = BLOCKS.register("nexus_conduit_gate",
            () -> new LeylineConduitBlock(4));
    public static final RegistryObject<Block> PRIMORDIAL_CONDUIT_GATE = BLOCKS.register("primordial_conduit_gate",
            () -> new LeylineConduitBlock(5));

    // Pillars and caps
    public static final RegistryObject<Block> RUNIC_PILLAR = BLOCKS.register("runic_pillar",
            () -> new RunicPillarBlock(3));
    public static final RegistryObject<Block> NEXUS_OBELISK = BLOCKS.register("nexus_obelisk",
            () -> new RunicPillarBlock(4));
    public static final RegistryObject<Block> PRIMORDIAL_PYLON = BLOCKS.register("primordial_pylon",
            () -> new RunicPillarBlock(5));
    public static final RegistryObject<Block> ASTRAL_CAPSTONE = BLOCKS.register("astral_capstone",
            () -> new PillarCapBlock(3));
    public static final RegistryObject<Block> MANA_LANTERN_CAP = BLOCKS.register("mana_lantern_cap",
            () -> new PillarCapBlock(4));

    // === Mana Reservoir Blocks ===
    public static final RegistryObject<Block> MANA_RESERVOIR = BLOCKS.register("mana_reservoir",
            () -> new ManaReservoirBlock(2048, 16));
    public static final RegistryObject<Block> GREATER_MANA_RESERVOIR = BLOCKS.register("greater_mana_reservoir",
            () -> new ManaReservoirBlock(8192, 64));

    // === Mana Spreader Block ===
    public static final RegistryObject<Block> MANA_SPREADER = BLOCKS.register("mana_spreader",
            ManaSpreaderBlock::new);

    // === Round 42: 魔力传输水晶 (DE-style P2P) — 装在 mana_reservoir 顶部 ===
    public static final RegistryObject<Block> MANA_TRANSMIT_CRYSTAL = BLOCKS.register("mana_transmit_crystal",
            com.huige233.transcend.block.mana.ManaTransmitCrystalBlock::new);

    // === Round 51: 增幅符文 — 安装在水晶头顶（柱冠位）增强 P2P 性能 ===
    public static final RegistryObject<Block> AUGMENT_RUNE_HASTE = BLOCKS.register("augment_rune_haste",
            () -> new com.huige233.transcend.block.augment.AugmentRuneBlock(
                    com.huige233.transcend.block.augment.AugmentRuneBlock.AugmentType.HASTE));
    public static final RegistryObject<Block> AUGMENT_RUNE_EFFICIENCY = BLOCKS.register("augment_rune_efficiency",
            () -> new com.huige233.transcend.block.augment.AugmentRuneBlock(
                    com.huige233.transcend.block.augment.AugmentRuneBlock.AugmentType.EFFICIENCY));
    public static final RegistryObject<Block> AUGMENT_RUNE_PRESERVATION = BLOCKS.register("augment_rune_preservation",
            () -> new com.huige233.transcend.block.augment.AugmentRuneBlock(
                    com.huige233.transcend.block.augment.AugmentRuneBlock.AugmentType.PRESERVATION));

    // === Round 52: Mana Dew — 水面被动产 mana ===
    public static final RegistryObject<Block> MANA_DEW = BLOCKS.register("mana_dew",
            com.huige233.transcend.block.ManaDewBlock::new);

    // === Aether Line (Round 01) ===
    // 以太轴线：碎片矿物 → 存储块。融入世界观的"古代飞升残留能量"。
    public static final RegistryObject<Block> AETHER_ORE = BLOCKS.register("aether_ore",
            () -> new AetherOreBlock(AetherOreBlock.Variant.OVERWORLD));
    public static final RegistryObject<Block> DEEPSLATE_AETHER_ORE = BLOCKS.register("deepslate_aether_ore",
            () -> new AetherOreBlock(AetherOreBlock.Variant.DEEPSLATE));
    public static final RegistryObject<Block> NETHER_AETHER_ORE = BLOCKS.register("nether_aether_ore",
            () -> new AetherOreBlock(AetherOreBlock.Variant.NETHER));
    public static final RegistryObject<Block> AETHER_BLOCK = BLOCKS.register("aether_block",
            AetherBlock::new);

    // === Round 18: 魔力水晶矿 ===
    public static final RegistryObject<Block> MAGIC_CRYSTAL_ORE = BLOCKS.register("magic_crystal_ore",
            () -> new com.huige233.transcend.block.aether.MagicCrystalOreBlock(
                    com.huige233.transcend.block.aether.MagicCrystalOreBlock.Variant.STONE));
    public static final RegistryObject<Block> DEEPSLATE_MAGIC_CRYSTAL_ORE = BLOCKS.register("deepslate_magic_crystal_ore",
            () -> new com.huige233.transcend.block.aether.MagicCrystalOreBlock(
                    com.huige233.transcend.block.aether.MagicCrystalOreBlock.Variant.DEEPSLATE));
    public static final RegistryObject<Block> NETHER_MAGIC_CRYSTAL_ORE = BLOCKS.register("nether_magic_crystal_ore",
            () -> new com.huige233.transcend.block.aether.MagicCrystalOreBlock(
                    com.huige233.transcend.block.aether.MagicCrystalOreBlock.Variant.NETHER));

    // === Decorative Magic Blocks (Round 01) ===
    public static final RegistryObject<Block> MANA_LANTERN = BLOCKS.register("mana_lantern",
            ManaLanternBlock::new);
    public static final RegistryObject<Block> AETHER_GLASS = BLOCKS.register("aether_glass",
            AetherGlassBlock::new);

    // === Magical Building Set (Round 07) ===
    // 4 装饰方块。无 axis 简洁块，全部使用通用 Block + Properties。
    public static final RegistryObject<Block> RUNED_STONE_BRICKS = BLOCKS.register("runed_stone_bricks",
            () -> new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.STONE)
                    .strength(2.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.STONE)
                    .lightLevel(state -> 3)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> AETHER_BRICKS = BLOCKS.register("aether_bricks",
            () -> new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
                    .lightLevel(state -> 6)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> POLISHED_AETHER = BLOCKS.register("polished_aether",
            () -> new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE)
                    .strength(4.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RESONANT_FLOOR_TILE = BLOCKS.register("resonant_floor_tile",
            () -> new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.5F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.GLASS)
                    .lightLevel(state -> 10)
                    .requiresCorrectToolForDrops()));

    // === Round 08: Element-Themed Lanterns ===
    // 4 元素色变体，复用 ManaLanternBlock 行为（光 15、雨中不灭）
    public static final RegistryObject<Block> PYRO_LANTERN = BLOCKS.register("pyro_lantern", ManaLanternBlock::new);
    public static final RegistryObject<Block> CRYO_LANTERN = BLOCKS.register("cryo_lantern", ManaLanternBlock::new);
    public static final RegistryObject<Block> STORM_LANTERN = BLOCKS.register("storm_lantern", ManaLanternBlock::new);
    public static final RegistryObject<Block> VOID_LANTERN = BLOCKS.register("void_lantern", ManaLanternBlock::new);

    // === Round 21: Typed Mana Conduit (γ 路线 - 类型化魔力输送网络) ===
    public static final RegistryObject<Block> MANA_CONDUIT = BLOCKS.register("mana_conduit",
            com.huige233.transcend.block.mana.ManaConduitBlock::new);

    // === Round 22: Functional Mana Blocks (γ 自动化 - 4 个功能 sink) ===
    public static final RegistryObject<Block> MANA_FURNACE = BLOCKS.register("mana_furnace",
            () -> new com.huige233.transcend.block.mana.FunctionalManaBlock(
                    com.huige233.transcend.block.mana.FunctionalManaBlock.FunctionType.FURNACE));
    public static final RegistryObject<Block> MANA_SENTINEL = BLOCKS.register("mana_sentinel",
            () -> new com.huige233.transcend.block.mana.FunctionalManaBlock(
                    com.huige233.transcend.block.mana.FunctionalManaBlock.FunctionType.SENTINEL));
    public static final RegistryObject<Block> MANA_HARVESTER = BLOCKS.register("mana_harvester",
            () -> new com.huige233.transcend.block.mana.FunctionalManaBlock(
                    com.huige233.transcend.block.mana.FunctionalManaBlock.FunctionType.HARVESTER));
    public static final RegistryObject<Block> MANA_GENERATOR = BLOCKS.register("mana_generator",
            () -> new com.huige233.transcend.block.mana.FunctionalManaBlock(
                    com.huige233.transcend.block.mana.FunctionalManaBlock.FunctionType.GENERATOR));

    // === Round 25: Aether Realm 独占矿石 ===
    public static final RegistryObject<Block> AETHER_ESSENCE_ORE = BLOCKS.register("aether_essence_ore",
            com.huige233.transcend.block.aether.AetherEssenceOreBlock::new);

    // === Round 50: Mana Blossom (Pure Daisy 风) — in-place 方块转化 ===
    public static final RegistryObject<Block> MANA_BLOSSOM = BLOCKS.register("mana_blossom",
            com.huige233.transcend.block.ManaBlossomBlock::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
