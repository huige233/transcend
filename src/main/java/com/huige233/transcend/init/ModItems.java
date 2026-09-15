package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.ItemBase;
import com.huige233.transcend.items.TestDummySpawnerItem;
import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.items.armor.TranscendArmor;
import com.huige233.transcend.items.curio.AnvilCompat;
import com.huige233.transcend.items.curio.FragmentLan;
import com.huige233.transcend.items.curio.ThunderSkin;
import com.huige233.transcend.items.curio.TranscendCurio;
import com.huige233.transcend.items.curio.TheLastTotem;
import com.huige233.transcend.items.tools.TestSword;
import com.huige233.transcend.items.tools.TranscendSword;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/** 集中注册核心装备、饰品、科技材料、知识手册及机器方块对应的物品。 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS_REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, Transcend.MODID);

    public static final RegistryObject<Item> transcend_ingot = ITEMS_REGISTRY.register("transcend_ingot",
            () -> new ItemBase(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> epic_ingot = ITEMS_REGISTRY.register("epic_ingot",
            () -> new ItemBase(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> normal_ingot = ITEMS_REGISTRY.register("normal_ingot",
            () -> new ItemBase(new Item.Properties()));

    public static final RegistryObject<Item> transcend_sword = ITEMS_REGISTRY.register("transcend_sword",
            TranscendSword::new);
    public static final RegistryObject<Item> test_sword = ITEMS_REGISTRY.register("test_sword",
            TestSword::new);
    public static final RegistryObject<Item> test_dummy_spawner = ITEMS_REGISTRY.register("test_dummy_spawner",
            TestDummySpawnerItem::new);

    public static final RegistryObject<Item> transcend_curio = ITEMS_REGISTRY.register("transcend_curio",
            () -> new TranscendCurio(new Item.Properties()));
    public static final RegistryObject<Item> thelasttotem = ITEMS_REGISTRY.register("thelasttotem",
            TheLastTotem::new);

    
    public static final RegistryObject<Item> transcend_editor_device = ITEMS_REGISTRY.register("transcend_editor_device",
            com.huige233.transcend.items.tools.TranscendEditWand::new);
    public static final RegistryObject<Item> transcend_wrench = ITEMS_REGISTRY.register("transcend_wrench",
            com.huige233.transcend.items.tools.TranscendWrenchItem::new);

    
    public static final RegistryObject<Item> transcend_helmet = ITEMS_REGISTRY.register("transcend_helmet",
            () -> new TranscendArmor(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> transcend_chestplate = ITEMS_REGISTRY.register("transcend_chestplate",
            () -> new TranscendArmor(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> transcend_leggings = ITEMS_REGISTRY.register("transcend_leggings",
            () -> new TranscendArmor(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> transcend_boots = ITEMS_REGISTRY.register("transcend_boots",
            () -> new TranscendArmor(ArmorItem.Type.BOOTS));

    public static final RegistryObject<Item> transcend_shield = ITEMS_REGISTRY.register("transcend_shield",
            TranscendShield::new);

    
    public static final java.util.List<RegistryObject<Item>> MECHANICAL_KNOWLEDGE =
            java.util.stream.IntStream.rangeClosed(1, 7)
                    .mapToObj(tier -> ITEMS_REGISTRY.<Item>register("mechanical_knowledge_t" + tier,
                            () -> new com.huige233.transcend.items.tech.MechanicalKnowledgeItem(tier)))
                    .toList();

    public static final RegistryObject<Item> particle_gun = ITEMS_REGISTRY.register("particle_gun",
            com.huige233.transcend.items.tech.ParticleGun::new);
    public static final RegistryObject<Item> phase_shield = ITEMS_REGISTRY.register("phase_shield",
            com.huige233.transcend.items.tech.PhaseShield::new);
    public static final RegistryObject<Item> standard_capacitor = ITEMS_REGISTRY.register("standard_capacitor",
            com.huige233.transcend.items.tech.StandardCapacitorItem::new);
    public static final RegistryObject<Item> phantom_energy_block = ITEMS_REGISTRY.register("phantom_energy_block",
            com.huige233.transcend.items.tech.PhantomEnergyBlockItem::new);
    public static final RegistryObject<Item> portable_capacitor = ITEMS_REGISTRY.register("portable_capacitor",
            () -> new com.huige233.transcend.items.tech.LongCapacitorItem(10_000_000L));
    public static final RegistryObject<Item> portable_capacitor_advanced = ITEMS_REGISTRY.register("portable_capacitor_advanced",
            () -> new com.huige233.transcend.items.tech.LongCapacitorItem(1_000_000_000L));
    public static final RegistryObject<Item> portable_capacitor_ghost = ITEMS_REGISTRY.register("portable_capacitor_ghost",
            () -> new com.huige233.transcend.items.tech.LongCapacitorItem(com.huige233.transcend.items.tech.LongCapacitorItem.RF_PER_TECH * 1_000_000_000L));
    public static final RegistryObject<Item> shield_module = ITEMS_REGISTRY.register("shield_module",
            com.huige233.transcend.items.tech.ShieldModuleItem::new);
    
    public static final RegistryObject<Item> gun_module = ITEMS_REGISTRY.register("gun_module",
            com.huige233.transcend.items.tech.GunModuleItem::new);
    
    public static final RegistryObject<Item> sirius_module = ITEMS_REGISTRY.register("sirius_module",
            com.huige233.transcend.items.tech.SiriusModuleItem::new);
    
    public static final RegistryObject<Item> black_hole_seed = ITEMS_REGISTRY.register("black_hole_seed",
            com.huige233.transcend.items.tech.BlackHoleSeedItem::new);
    public static final RegistryObject<Item> magnetic_confinement_container = ITEMS_REGISTRY.register("magnetic_confinement_container",
            com.huige233.transcend.items.tech.MagneticConfinementContainerItem::new);
    public static final RegistryObject<Item> mini_singularity = ITEMS_REGISTRY.register("mini_singularity",
            () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));

    
    public static final RegistryObject<Item> tech_part_gear = ITEMS_REGISTRY.register("tech_part_gear",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_circuit = ITEMS_REGISTRY.register("tech_part_circuit",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_focusing = ITEMS_REGISTRY.register("tech_part_focusing",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_capacitor = ITEMS_REGISTRY.register("tech_part_capacitor",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_dust_conductive = ITEMS_REGISTRY.register("tech_dust_conductive",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_gear_blank = ITEMS_REGISTRY.register("tech_part_gear_blank",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_gear_refined = ITEMS_REGISTRY.register("tech_part_gear_refined",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_circuit_printed = ITEMS_REGISTRY.register("tech_part_circuit_printed",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_circuit_wafer = ITEMS_REGISTRY.register("tech_part_circuit_wafer",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_glass_phase = ITEMS_REGISTRY.register("tech_part_glass_phase",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_lens_phase = ITEMS_REGISTRY.register("tech_part_lens_phase",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_part_capacitor_core = ITEMS_REGISTRY.register("tech_part_capacitor_core",
            () -> new ItemBase(new Item.Properties()));
    public static final RegistryObject<Item> tech_core_pilot = ITEMS_REGISTRY.register("tech_core_pilot",
            () -> new ItemBase(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> tech_energy_cell = ITEMS_REGISTRY.register("tech_energy_cell",
            () -> new ItemBase(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> tech_energy_core = ITEMS_REGISTRY.register("tech_energy_core",
            () -> new ItemBase(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> tech_energy_matrix = ITEMS_REGISTRY.register("tech_energy_matrix",
            () -> new ItemBase(new Item.Properties().stacksTo(1).fireResistant()));

    public static final RegistryObject<Item> anvil_compat = ITEMS_REGISTRY.register("anvil_compat",
            AnvilCompat::new);
    public static final RegistryObject<Item> fragment_lan = ITEMS_REGISTRY.register("fragment_lan",
            FragmentLan::new);
    public static final RegistryObject<Item> thunder_skin = ITEMS_REGISTRY.register("thunder_skin",
            ThunderSkin::new);

    
    public static final RegistryObject<Item> assembly_table = ITEMS_REGISTRY.register("assembly_table",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ASSEMBLY.get(), new Item.Properties()));
    public static final RegistryObject<Item> black_hole_seed_breeder = ITEMS_REGISTRY.register("black_hole_seed_breeder",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.BLACK_HOLE_SEED_BREEDER.get(), new Item.Properties()));
    public static final RegistryObject<Item> mini_universe_generator = ITEMS_REGISTRY.register("mini_universe_generator",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MINI_UNIVERSE_GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> fire_generator = ITEMS_REGISTRY.register("fire_generator", () -> new net.minecraft.world.item.BlockItem(ModBlocks.FIRE_GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> wind_generator = ITEMS_REGISTRY.register("wind_generator", () -> new net.minecraft.world.item.BlockItem(ModBlocks.WIND_GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> creative_generator = ITEMS_REGISTRY.register("creative_generator", () -> new net.minecraft.world.item.BlockItem(ModBlocks.CREATIVE_GENERATOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> research_station = ITEMS_REGISTRY.register("research_station", () -> new net.minecraft.world.item.BlockItem(ModBlocks.RESEARCH_STATION.get(), new Item.Properties()));
    public static final RegistryObject<Item> research_processor = ITEMS_REGISTRY.register("research_processor", () -> new net.minecraft.world.item.BlockItem(ModBlocks.RESEARCH_PROCESSOR.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> blank_research_component = ITEMS_REGISTRY.register("blank_research_component", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> research_assist_unit = ITEMS_REGISTRY.register("research_assist_unit", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> research_quantum_computer = ITEMS_REGISTRY.register("research_quantum_computer", () -> new net.minecraft.world.item.BlockItem(ModBlocks.RESEARCH_QUANTUM_COMPUTER.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> research_cosmic_simulator = ITEMS_REGISTRY.register("research_cosmic_simulator", () -> new net.minecraft.world.item.BlockItem(ModBlocks.RESEARCH_COSMIC_SIMULATOR.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> long_storage_basic = ITEMS_REGISTRY.register("long_storage_basic", () -> new net.minecraft.world.item.BlockItem(ModBlocks.LONG_STORAGE_BASIC.get(), new Item.Properties()));
    public static final RegistryObject<Item> long_storage_advanced = ITEMS_REGISTRY.register("long_storage_advanced", () -> new net.minecraft.world.item.BlockItem(ModBlocks.LONG_STORAGE_ADVANCED.get(), new Item.Properties()));
    public static final RegistryObject<Item> long_storage_quantum = ITEMS_REGISTRY.register("long_storage_quantum", () -> new net.minecraft.world.item.BlockItem(ModBlocks.LONG_STORAGE_QUANTUM.get(), new Item.Properties()));
    public static final RegistryObject<Item> long_storage_phantom = ITEMS_REGISTRY.register("long_storage_phantom", () -> new net.minecraft.world.item.BlockItem(ModBlocks.LONG_STORAGE_PHANTOM.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS_REGISTRY.register(eventBus);
    }
}
