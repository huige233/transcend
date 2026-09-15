package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;


/** 注册模组创造模式物品栏，并按指定顺序展示装备、科技材料和模块变体。 */
public class TranscendTab {
    public static final String TAB_TITLE = "creativetab.test1_tab";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Transcend.MODID);

    public static final RegistryObject<CreativeModeTab> TRANSCEND_TAB = CREATIVE_MODE_TABS.register("transcend_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.transcend_ingot.get()))
                    .title(Component.translatable(TAB_TITLE))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.transcend_editor_device.get());
                        pOutput.accept(ModItems.transcend_wrench.get());
                        pOutput.accept(ModItems.normal_ingot.get());
                        pOutput.accept(ModItems.epic_ingot.get());
                        pOutput.accept(ModItems.transcend_ingot.get());
                        pOutput.accept(ModItems.test_sword.get());
                        pOutput.accept(ModItems.transcend_sword.get());
                        pOutput.accept(ModItems.transcend_shield.get());
                        pOutput.accept(ModItems.particle_gun.get());
                        pOutput.accept(ModItems.sirius_module.get());
                        pOutput.accept(ModItems.research_station.get());
                        pOutput.accept(ModItems.blank_research_component.get());
                        pOutput.accept(ModItems.research_assist_unit.get());
                        pOutput.accept(ModItems.research_processor.get());
                        pOutput.accept(ModItems.research_quantum_computer.get());
                        pOutput.accept(ModItems.research_cosmic_simulator.get());
                        pOutput.accept(ModItems.black_hole_seed.get());
                        pOutput.accept(ModItems.magnetic_confinement_container.get());
                        pOutput.accept(ModItems.mini_singularity.get());
                        pOutput.accept(ModItems.black_hole_seed_breeder.get());
                        pOutput.accept(ModItems.mini_universe_generator.get());
                        pOutput.accept(ModItems.fire_generator.get());
                        pOutput.accept(ModItems.wind_generator.get());
                        pOutput.accept(ModItems.creative_generator.get());
                        pOutput.accept(ModItems.long_storage_basic.get());
                        pOutput.accept(ModItems.long_storage_advanced.get());
                        pOutput.accept(ModItems.long_storage_quantum.get());
                        pOutput.accept(ModItems.long_storage_phantom.get());
                        ModItems.MECHANICAL_KNOWLEDGE.forEach(item -> pOutput.accept(item.get()));
                        for (com.huige233.transcend.items.tech.GunModule module
                                : com.huige233.transcend.items.tech.GunModule.values()) {
                            if (module != com.huige233.transcend.items.tech.GunModule.MUZZLE_NONE) {
                                pOutput.accept(com.huige233.transcend.items.tech.GunModuleItem.create(module));
                            }
                        }
                        pOutput.accept(ModItems.phase_shield.get());
                        pOutput.accept(com.huige233.transcend.items.tech.StandardCapacitorItem.charged());
                        pOutput.accept(ModItems.phantom_energy_block.get());
                        pOutput.accept(ModItems.portable_capacitor.get());
                        pOutput.accept(ModItems.portable_capacitor_advanced.get());
                        pOutput.accept(ModItems.portable_capacitor_ghost.get());
                        pOutput.accept(ModItems.tech_part_gear.get());
                        pOutput.accept(ModItems.tech_part_gear_blank.get());
                        pOutput.accept(ModItems.tech_part_gear_refined.get());
                        pOutput.accept(ModItems.tech_part_circuit.get());
                        pOutput.accept(ModItems.tech_part_circuit_printed.get());
                        pOutput.accept(ModItems.tech_part_circuit_wafer.get());
                        pOutput.accept(ModItems.tech_part_focusing.get());
                        pOutput.accept(ModItems.tech_part_capacitor.get());
                        pOutput.accept(ModItems.tech_part_capacitor_core.get());
                        pOutput.accept(ModItems.tech_part_glass_phase.get());
                        pOutput.accept(ModItems.tech_part_lens_phase.get());
                        pOutput.accept(ModItems.tech_dust_conductive.get());
                        pOutput.accept(ModItems.tech_core_pilot.get());
                        pOutput.accept(ModItems.tech_energy_cell.get());
                        pOutput.accept(ModItems.tech_energy_core.get());
                        pOutput.accept(ModItems.tech_energy_matrix.get());
                        for (com.huige233.transcend.items.tech.ShieldModule module
                                : com.huige233.transcend.items.tech.ShieldModule.values())
                            pOutput.accept(com.huige233.transcend.items.tech.ShieldModuleItem.create(module));
                        pOutput.accept(ModItems.transcend_helmet.get());
                        pOutput.accept(ModItems.transcend_chestplate.get());
                        pOutput.accept(ModItems.transcend_leggings.get());
                        pOutput.accept(ModItems.transcend_boots.get());
                        pOutput.accept(ModItems.transcend_curio.get());
                        pOutput.accept(ModItems.anvil_compat.get());
                        pOutput.accept(ModItems.fragment_lan.get());
                        pOutput.accept(ModItems.thunder_skin.get());
                        pOutput.accept(ModItems.thelasttotem.get());





                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}