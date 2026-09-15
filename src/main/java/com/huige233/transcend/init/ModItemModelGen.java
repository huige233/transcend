package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;


/** 生成模组物品模型、科技模块属性变体及机器物品继承的方块模型。 */
public class ModItemModelGen extends ItemModelProvider {
    public static final String GENERATED = "item/generated";

    public ModItemModelGen(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Transcend.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        
        itemGenerateModel(ModItems.normal_ingot.get());
        itemGenerateModel(ModItems.epic_ingot.get());
        itemGenerateModel(ModItems.transcend_ingot.get());
        
        itemGenerateModel(ModItems.transcend_sword.get());
        itemGenerateModel(ModItems.test_sword.get());
        itemGenerateModel(ModItems.test_dummy_spawner.get());
        itemGenerateModel(ModItems.transcend_shield.get());
        
        itemGenerateModel(ModItems.particle_gun.get());
        itemGenerateModel(ModItems.black_hole_seed.get(), resourceItem("black_hole_seed"));
        itemGenerateModel(ModItems.magnetic_confinement_container.get());
        itemGenerateModel(ModItems.mini_singularity.get());
        itemGenerateModel(ModItems.black_hole_seed_breeder.get());
        itemGenerateModel(ModItems.mini_universe_generator.get());
        itemGenerateModel(ModItems.blank_research_component.get(), resourceItem("blank_research_component"));
        itemGenerateModel(ModItems.research_assist_unit.get(), resourceItem("research_assist_unit"));
        withExistingParent("research_station", modLoc("block/research_station"));
        withExistingParent("fire_generator", modLoc("block/fire_generator"));
        withExistingParent("wind_generator", modLoc("block/wind_generator"));
        withExistingParent("creative_generator", modLoc("block/creative_generator"));
        withExistingParent("long_storage_advanced", modLoc("block/long_storage_advanced"));
        withExistingParent("long_storage_quantum", modLoc("block/long_storage_quantum"));
        withExistingParent("long_storage_phantom", modLoc("block/long_storage_phantom"));
        withExistingParent("wind_generator", modLoc("block/wind_generator"));
        withExistingParent("creative_generator", modLoc("block/creative_generator"));
        var moduleModel = withExistingParent("gun_module", GENERATED)
                .texture("layer0", resourceItem("gun_module"));
        for (var module : com.huige233.transcend.items.tech.GunModule.values()) {
            if (module.id.isEmpty()) continue;
            var variant = withExistingParent("gun_module/" + module.id, GENERATED)
                    .texture("layer0", resourceItem(module.id));
            moduleModel.override().predicate(Transcend.rl("module"), module.modelIndex()).model(variant).end();
        }
        itemGenerateModel(ModItems.phase_shield.get());
        itemGenerateModel(ModItems.standard_capacitor.get());
        itemGenerateModel(ModItems.phantom_energy_block.get());
        itemGenerateModel(ModItems.portable_capacitor.get());
        itemGenerateModel(ModItems.portable_capacitor_advanced.get());
        itemGenerateModel(ModItems.portable_capacitor_ghost.get());
        
        itemGenerateModel(ModItems.tech_part_gear.get());
        itemGenerateModel(ModItems.tech_part_gear_blank.get());
        itemGenerateModel(ModItems.tech_part_gear_refined.get());
        itemGenerateModel(ModItems.tech_part_circuit.get());
        itemGenerateModel(ModItems.tech_part_circuit_printed.get());
        itemGenerateModel(ModItems.tech_part_circuit_wafer.get());
        itemGenerateModel(ModItems.tech_part_focusing.get());
        itemGenerateModel(ModItems.tech_part_capacitor.get());
        itemGenerateModel(ModItems.tech_part_capacitor_core.get());
        itemGenerateModel(ModItems.tech_part_glass_phase.get());
        itemGenerateModel(ModItems.tech_part_lens_phase.get());
        itemGenerateModel(ModItems.tech_dust_conductive.get());
        itemGenerateModel(ModItems.tech_core_pilot.get());
        itemGenerateModel(ModItems.tech_energy_cell.get());
        itemGenerateModel(ModItems.tech_energy_core.get());
        itemGenerateModel(ModItems.tech_energy_matrix.get());
        var shieldModuleModel = withExistingParent("shield_module", GENERATED)
                .texture("layer0", resourceItem("shield_module"));
        for (var module : com.huige233.transcend.items.tech.ShieldModule.values()) {
            var variant = withExistingParent("shield_module/" + module.id(), GENERATED)
                    .texture("layer0", resourceItem("shield_module_" + module.id()));
            shieldModuleModel.override().predicate(Transcend.rl("shield_module"), module.ordinal() + 1.0F).model(variant).end();
        }
        itemGenerateModel(ModItems.sirius_module.get(), resourceItem("sirius_module"));
        
        itemGenerateModel(ModItems.transcend_helmet.get());
        itemGenerateModel(ModItems.transcend_chestplate.get());
        itemGenerateModel(ModItems.transcend_leggings.get());
        itemGenerateModel(ModItems.transcend_boots.get());
        
        itemGenerateModel(ModItems.transcend_editor_device.get());
        
        itemGenerateModel(ModItems.transcend_curio.get());
        itemGenerateModel(ModItems.thelasttotem.get());
        itemGenerateModel(ModItems.anvil_compat.get());
        itemGenerateModel(ModItems.fragment_lan.get());
        itemGenerateModel(ModItems.thunder_skin.get());
        withExistingParent("research_station", modLoc("block/research_station"));
        withExistingParent("research_processor", modLoc("block/research_processor"));
        withExistingParent("research_quantum_computer", modLoc("block/research_quantum_computer"));
        withExistingParent("research_cosmic_simulator", modLoc("block/research_cosmic_simulator"));
    }

    public void itemGenerateModel(Item item) {
        itemGenerateModel(item, resourceItem(itemName(item)));
    }

    public void itemGenerateModel(Item item, ResourceLocation texture) {
        withExistingParent(itemName(item), GENERATED).texture("layer0", texture);
    }

    public String itemName(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).getPath();
    }

    public ResourceLocation resourceItem(String path) {
        return ResourceLocation.fromNamespaceAndPath(Transcend.MODID, "item/" + path);
    }
}
