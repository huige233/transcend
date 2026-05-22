package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class TranscendTab {
    public static final String TAB_TITLE = "creativetab.test1_tab";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Transcend.MODID);

    public static final RegistryObject<CreativeModeTab> TRANSCEND_TAB = CREATIVE_MODE_TABS.register("transcend_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.transcend_ingot.get()))
                    .title(Component.translatable(TAB_TITLE))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.normal_ingot.get());
                        pOutput.accept(ModItems.epic_ingot.get());
                        pOutput.accept(ModItems.transcend_ingot.get());
                        pOutput.accept(ModItems.test_sword.get());
                        pOutput.accept(ModItems.transcend_sword.get());
                        pOutput.accept(ModItems.transcend_shield.get());
                        pOutput.accept(ModItems.transcend_helmet.get());
                        pOutput.accept(ModItems.transcend_chestplate.get());
                        pOutput.accept(ModItems.transcend_leggings.get());
                        pOutput.accept(ModItems.transcend_boots.get());
                        pOutput.accept(ModItems.pyro_helmet.get());
                        pOutput.accept(ModItems.pyro_chestplate.get());
                        pOutput.accept(ModItems.pyro_leggings.get());
                        pOutput.accept(ModItems.pyro_boots.get());
                        pOutput.accept(ModItems.cryo_helmet.get());
                        pOutput.accept(ModItems.cryo_chestplate.get());
                        pOutput.accept(ModItems.cryo_leggings.get());
                        pOutput.accept(ModItems.cryo_boots.get());
                        pOutput.accept(ModItems.storm_helmet.get());
                        pOutput.accept(ModItems.storm_chestplate.get());
                        pOutput.accept(ModItems.storm_leggings.get());
                        pOutput.accept(ModItems.storm_boots.get());
                        pOutput.accept(ModItems.terra_helmet.get());
                        pOutput.accept(ModItems.terra_chestplate.get());
                        pOutput.accept(ModItems.terra_leggings.get());
                        pOutput.accept(ModItems.terra_boots.get());
                        pOutput.accept(ModItems.arcane_set_helmet.get());
                        pOutput.accept(ModItems.arcane_set_chestplate.get());
                        pOutput.accept(ModItems.arcane_set_leggings.get());
                        pOutput.accept(ModItems.arcane_set_boots.get());
                        pOutput.accept(ModItems.abyss_helmet.get());
                        pOutput.accept(ModItems.abyss_chestplate.get());
                        pOutput.accept(ModItems.abyss_leggings.get());
                        pOutput.accept(ModItems.abyss_boots.get());
                        pOutput.accept(ModItems.transcend_curio.get());
                        pOutput.accept(ModItems.anvil_compat.get());
                        pOutput.accept(ModItems.fragment_lan.get());
                        pOutput.accept(ModItems.thunder_skin.get());
                        pOutput.accept(ModItems.thelasttotem.get());
                        pOutput.accept(ModItems.magic_circle.get());
                        pOutput.accept(ModItems.magic_circle_alt.get());
                        pOutput.accept(ModItems.magic_circle_inferno.get());
                        pOutput.accept(ModItems.magic_circle_glacial.get());
                        pOutput.accept(ModItems.magic_circle_sanctum.get());
                        pOutput.accept(ModItems.magic_circle_gravity.get());
                        pOutput.accept(ModItems.magic_circle_thunder.get());
                        pOutput.accept(ModItems.magic_circle_tempest.get());
                        pOutput.accept(ModItems.magic_circle_terra.get());
                        pOutput.accept(ModItems.magic_circle_void.get());
                        pOutput.accept(ModItems.magic_circle_chrono.get());
                        pOutput.accept(ModItems.magic_circle_blood.get());
                        pOutput.accept(ModItems.magic_circle_divine.get());
                        pOutput.accept(ModItems.magic_circle_chaos.get());
                        pOutput.accept(ModItems.magic_circle_phantom.get());
                        pOutput.accept(ModItems.magic_circle_skybound.get());
                        pOutput.accept(ModItems.magic_crystal.get());
                        pOutput.accept(ModItems.refined_magic_crystal.get());
                        // === Round 17: 元素相位水晶 ===
                        pOutput.accept(ModItems.tainted_crystal.get());
                        pOutput.accept(ModItems.aether_crystal.get());
                        pOutput.accept(ModItems.blood_crystal.get());
                        pOutput.accept(ModItems.cosmic_crystal.get());
                        pOutput.accept(ModItems.enhance_power.get());
                        pOutput.accept(ModItems.enhance_duration.get());
                        pOutput.accept(ModItems.enhance_efficiency.get());
                        pOutput.accept(ModItems.enhance_special.get());
                        pOutput.accept(ModItems.mana_storage.get());
                        pOutput.accept(ModItems.greater_mana_storage.get());
                        pOutput.accept(ModItems.ancient_mana_vessel.get());
                        pOutput.accept(ModItems.wand_basic.get());
                        pOutput.accept(ModItems.wand_advanced.get());
                        pOutput.accept(ModItems.wand_master.get());
                        pOutput.accept(ModItems.spell_workbench_item.get());
                        pOutput.accept(ModItems.spell_base_basic.get());
                        pOutput.accept(ModItems.spell_base_advanced.get());
                        pOutput.accept(ModItems.spell_base_master.get());
                        pOutput.accept(ModItems.carrier_orb.get());
                        pOutput.accept(ModItems.carrier_arrow.get());
                        pOutput.accept(ModItems.carrier_slash.get());
                        pOutput.accept(ModItems.carrier_beam.get());
                        pOutput.accept(ModItems.carrier_nova.get());
                        pOutput.accept(ModItems.carrier_chain.get());
                        pOutput.accept(ModItems.carrier_vortex.get());
                        pOutput.accept(ModItems.carrier_spike.get());
                        pOutput.accept(ModItems.carrier_teleport.get());
                        pOutput.accept(ModItems.carrier_trap.get());
                        pOutput.accept(ModItems.carrier_barrier.get());
                        pOutput.accept(ModItems.carrier_summon.get());
                        pOutput.accept(ModItems.carrier_ring.get());
                        pOutput.accept(ModItems.carrier_breath.get());
                        pOutput.accept(ModItems.carrier_rain.get());
                        pOutput.accept(ModItems.carrier_dash.get());
                        pOutput.accept(ModItems.carrier_ground.get());
                        pOutput.accept(ModItems.element_fire.get());
                        pOutput.accept(ModItems.element_ice.get());
                        pOutput.accept(ModItems.element_thunder.get());
                        pOutput.accept(ModItems.element_wind.get());
                        pOutput.accept(ModItems.element_earth.get());
                        pOutput.accept(ModItems.element_void.get());
                        pOutput.accept(ModItems.element_holy.get());
                        pOutput.accept(ModItems.element_blood.get());
                        pOutput.accept(ModItems.element_dark.get());
                        pOutput.accept(ModItems.element_light.get());
                        pOutput.accept(ModItems.element_poison.get());
                        pOutput.accept(ModItems.element_time.get());
                        pOutput.accept(ModItems.element_space.get());
                        pOutput.accept(ModItems.element_nature.get());
                        pOutput.accept(ModItems.element_chaos.get());
                        pOutput.accept(ModItems.element_acid.get());
                        pOutput.accept(ModItems.element_sonic.get());
                        pOutput.accept(ModItems.element_eldritch.get());
                        pOutput.accept(ModItems.effect_explosion.get());
                        pOutput.accept(ModItems.effect_piercing.get());
                        pOutput.accept(ModItems.effect_split.get());
                        pOutput.accept(ModItems.effect_homing.get());
                        pOutput.accept(ModItems.effect_healing.get());
                        pOutput.accept(ModItems.effect_shield.get());
                        pOutput.accept(ModItems.effect_chain_lightning.get());
                        pOutput.accept(ModItems.effect_bounce.get());
                        pOutput.accept(ModItems.effect_delayed.get());
                        pOutput.accept(ModItems.effect_amplify.get());
                        pOutput.accept(ModItems.effect_lifesteal.get());
                        pOutput.accept(ModItems.effect_quickcast.get());
                        pOutput.accept(ModItems.effect_multishot.get());
                        pOutput.accept(ModItems.effect_slowfield.get());
                        pOutput.accept(ModItems.effect_gravity_well.get());
                        pOutput.accept(ModItems.effect_mark.get());
                        pOutput.accept(ModItems.effect_echo.get());
                        pOutput.accept(ModItems.effect_armor_break.get());
                        pOutput.accept(ModItems.effect_root.get());
                        pOutput.accept(ModItems.effect_blight.get());
                        pOutput.accept(ModItems.effect_lingering.get());
                        pOutput.accept(ModItems.effect_devour.get());
                        pOutput.accept(ModItems.effect_absorb.get());
                        pOutput.accept(ModItems.effect_reflect.get());
                        pOutput.accept(ModItems.effect_curse.get());
                        pOutput.accept(ModItems.effect_overload.get());
                        pOutput.accept(ModItems.effect_weaken.get());
                        pOutput.accept(ModItems.effect_unstable.get());
                        pOutput.accept(ModItems.effect_shatter.get());
                        pOutput.accept(ModItems.effect_summon_wisp.get());
                        pOutput.accept(ModItems.effect_summon_guardian.get());
                        pOutput.accept(ModItems.spell_scroll.get());
                        pOutput.accept(ModItems.sealed_scroll.get());
                        pOutput.accept(ModItems.spell_upgrade_stone.get());
                        pOutput.accept(ModItems.rune_mana_siphon.get());
                        pOutput.accept(ModItems.rune_rapid_fire.get());
                        pOutput.accept(ModItems.rune_overcharge.get());
                        pOutput.accept(ModItems.rune_spell_echo.get());
                        pOutput.accept(ModItems.rune_elemental_mastery.get());
                        pOutput.accept(ModItems.rune_glass_cannon.get());
                        pOutput.accept(ModItems.rune_conservation.get());
                        pOutput.accept(ModItems.rune_chain_caster.get());
                        // 飞升系统
                        pOutput.accept(ModItems.ascension_book.get());
                        pOutput.accept(ModItems.respec_potion.get());
                        // Boss 召唤物品
                        pOutput.accept(ModItems.ancient_glyph.get());
                        pOutput.accept(ModItems.rift_fragment.get());
                        pOutput.accept(ModItems.transcendence_core.get());
                        // 功能方块
                        pOutput.accept(ModItems.ritual_altar.get());
                        pOutput.accept(ModItems.ritual_pedestal.get());
                        pOutput.accept(ModItems.mana_well.get());
                        pOutput.accept(ModItems.magic_crystal_block.get());
                        pOutput.accept(ModItems.concentrated_crystal_block.get());
                        pOutput.accept(ModItems.ancient_crystal.get());
                        // === 法环结构方块 ===
                        pOutput.accept(ModItems.ancient_circle_stone.get());
                        pOutput.accept(ModItems.awakened_circle_stone.get());
                        pOutput.accept(ModItems.astral_circle_stone.get());
                        pOutput.accept(ModItems.nexus_circle_stone.get());
                        pOutput.accept(ModItems.primordial_circle_stone.get());
                        pOutput.accept(ModItems.lesser_rune_stone.get());
                        pOutput.accept(ModItems.awakened_rune_stone.get());
                        pOutput.accept(ModItems.greater_rune_stone.get());
                        pOutput.accept(ModItems.archon_rune_stone.get());
                        pOutput.accept(ModItems.primordial_rune_stone.get());
                        pOutput.accept(ModItems.circle_core_dormant.get());
                        pOutput.accept(ModItems.circle_core_wellspring.get());
                        pOutput.accept(ModItems.circle_core_sanctuary.get());
                        pOutput.accept(ModItems.circle_core_dominion.get());
                        pOutput.accept(ModItems.circle_core_waystone.get());
                        pOutput.accept(ModItems.circle_core_convergence.get());
                        pOutput.accept(ModItems.circle_core_primordial.get());
                        pOutput.accept(ModItems.catalyst_plinth.get());
                        pOutput.accept(ModItems.sealed_catalyst_plinth.get());
                        pOutput.accept(ModItems.leyline_conduit_stone.get());
                        pOutput.accept(ModItems.aether_channel_marker.get());
                        pOutput.accept(ModItems.nexus_conduit_gate.get());
                        pOutput.accept(ModItems.primordial_conduit_gate.get());
                        pOutput.accept(ModItems.runic_pillar.get());
                        pOutput.accept(ModItems.nexus_obelisk.get());
                        pOutput.accept(ModItems.primordial_pylon.get());
                        pOutput.accept(ModItems.astral_capstone.get());
                        pOutput.accept(ModItems.mana_lantern_cap.get());
                        // === 魔力储液池 ===
                        pOutput.accept(ModItems.mana_reservoir.get());
                        pOutput.accept(ModItems.greater_mana_reservoir.get());
                        // === 法环工具 ===
                        pOutput.accept(ModItems.attunement_chisel.get());
                        pOutput.accept(ModItems.mana_lens.get());
                        pOutput.accept(ModItems.bound_aether_pearl.get());
                        // === 功能符印 ===
                        pOutput.accept(ModItems.sigil_leyline_siphon.get());
                        pOutput.accept(ModItems.sigil_remote_mana_link.get());
                        pOutput.accept(ModItems.sigil_arcane_amplifier.get());
                        pOutput.accept(ModItems.sigil_wellspring_renewal.get());
                        pOutput.accept(ModItems.sigil_leyline_convergence.get());
                        pOutput.accept(ModItems.sigil_warding_aegis.get());
                        pOutput.accept(ModItems.sigil_wayfarers_haste.get());
                        pOutput.accept(ModItems.sigil_deep_sight_veil.get());
                        pOutput.accept(ModItems.sigil_verdant_restoration.get());
                        pOutput.accept(ModItems.sigil_sky_mantle.get());
                        pOutput.accept(ModItems.sigil_weather_edict.get());
                        pOutput.accept(ModItems.sigil_chrono_loom.get());
                        pOutput.accept(ModItems.sigil_quiet_boundary.get());
                        pOutput.accept(ModItems.sigil_everlight_mandala.get());
                        pOutput.accept(ModItems.sigil_twin_horizon_gate.get());
                        pOutput.accept(ModItems.sigil_hearth_stability.get());
                        pOutput.accept(ModItems.sigil_dimensional_anchor.get());
                        pOutput.accept(ModItems.sigil_elemental_crucible.get());
                        pOutput.accept(ModItems.sigil_spell_resonance_nexus.get());
                        pOutput.accept(ModItems.sigil_nexus_gatehouse.get());
                        pOutput.accept(ModItems.sigil_primordial_synchrony.get());
                        pOutput.accept(ModItems.sigil_verdant_reaping.get());
                        pOutput.accept(ModItems.sigil_mineral_convergence.get());
                        pOutput.accept(ModItems.sigil_brood_hearth.get());
                        pOutput.accept(ModItems.sigil_aegis_lattice.get());
                        pOutput.accept(ModItems.sigil_sentinel_alarm.get());
                        pOutput.accept(ModItems.sigil_trapweaver_relay.get());
                        pOutput.accept(ModItems.sigil_covenant_reservoir.get());
                        pOutput.accept(ModItems.sigil_concordant_banner.get());
                        pOutput.accept(ModItems.sigil_cartographers_eye.get());
                        pOutput.accept(ModItems.sigil_biome_resonance.get());
                        pOutput.accept(ModItems.sigil_arcanist_forge_field.get());
                        pOutput.accept(ModItems.sigil_restoration_halo.get());
                        pOutput.accept(ModItems.sigil_prismatic_attunement.get());
                        pOutput.accept(ModItems.sigil_aurora_theatre.get());
                        pOutput.accept(ModItems.sigil_void_bore.get());
                        // === 蓝图 ===
                        pOutput.accept(ModItems.circle_blueprint_fragment.get());
                        pOutput.accept(ModItems.circle_blueprint_page.get());
                        pOutput.accept(ModItems.complete_circle_schematic.get());
                        // === 古代秘卷 ===
                        pOutput.accept(ModItems.scroll_solar_judgement.get());
                        pOutput.accept(ModItems.scroll_leyline_eruption.get());
                        pOutput.accept(ModItems.scroll_chronal_stillness.get());
                        pOutput.accept(ModItems.scroll_sovereign_aegis.get());
                        pOutput.accept(ModItems.scroll_thousand_league_return.get());
                        pOutput.accept(ModItems.scroll_void_exile.get());
                        pOutput.accept(ModItems.scroll_storm_king.get());
                        pOutput.accept(ModItems.scroll_worldmender.get());
                        pOutput.accept(ModItems.scroll_eclipse_veil.get());
                        pOutput.accept(ModItems.scroll_avatar_fall.get());
                        // === 扩展秘卷 ===
                        pOutput.accept(ModItems.scroll_unbroken_arsenal.get());
                        pOutput.accept(ModItems.scroll_ordered_vault.get());
                        pOutput.accept(ModItems.scroll_oreblood_revelation.get());
                        pOutput.accept(ModItems.scroll_paper_legion.get());
                        pOutput.accept(ModItems.scroll_unremembered_fog.get());
                        pOutput.accept(ModItems.scroll_inverted_heaven.get());
                        pOutput.accept(ModItems.scroll_eighteenfold_dragon.get());
                        pOutput.accept(ModItems.scroll_leyline_resync.get());
                        pOutput.accept(ModItems.scroll_forbidden_hollow_quarry.get());
                        pOutput.accept(ModItems.scroll_forbidden_black_sun.get());
                        // === 缺失补齐 (v10 audit) ===
                        // 法环工具 / 蓝图
                        pOutput.accept(ModItems.circle_architect_wand.get());
                        pOutput.accept(ModItems.structure_blueprint_scroll.get());
                        pOutput.accept(ModItems.function_imprint_scroll.get());
                        pOutput.accept(ModItems.mana_spreader.get());
                        // Round 42: DE-style 魔力传输水晶 + 绑定器
                        pOutput.accept(ModItems.mana_transmit_crystal.get());
                        pOutput.accept(ModItems.mana_crystal_binder.get());
                        // Round 51: 增幅符文（柱冠位）— 加速/节能/减损
                        pOutput.accept(ModItems.augment_rune_haste.get());
                        pOutput.accept(ModItems.augment_rune_efficiency.get());
                        pOutput.accept(ModItems.augment_rune_preservation.get());
                        // Round 52: mana sensor + mana dew
                        pOutput.accept(ModItems.mana_sensor.get());
                        pOutput.accept(ModItems.mana_dew.get());
                        // Round 45: 血魔法风格 - 祭祀之刃
                        pOutput.accept(ModItems.sacrificial_knife.get());
                        // Round 50: Pure Daisy 风魔力花
                        pOutput.accept(ModItems.mana_blossom.get());
                        // 执笔者套装 (Curio)
                        pOutput.accept(ModItems.inscriber_hood.get());
                        pOutput.accept(ModItems.inscriber_robe.get());
                        pOutput.accept(ModItems.inscriber_stylus.get());
                        // 铭刻工具
                        pOutput.accept(ModItems.inscription_quill.get());
                        // === Round 01: Aether 轴线物品 / 方块 ===
                        // 以太碎片：散布于矿物中的古代飞升残留能量
                        pOutput.accept(ModItems.aether_shard.get());
                        pOutput.accept(ModItems.aether_ore.get());
                        pOutput.accept(ModItems.deepslate_aether_ore.get());
                        pOutput.accept(ModItems.nether_aether_ore.get());
                        pOutput.accept(ModItems.aether_block.get());
                        // === Round 18: 魔力水晶矿 (3 变体) ===
                        pOutput.accept(ModItems.magic_crystal_ore.get());
                        pOutput.accept(ModItems.deepslate_magic_crystal_ore.get());
                        pOutput.accept(ModItems.nether_magic_crystal_ore.get());
                        // 装饰魔法方块
                        pOutput.accept(ModItems.mana_lantern.get());
                        pOutput.accept(ModItems.aether_glass.get());
                        // === Round 07: 魔法建材主题包 ===
                        pOutput.accept(ModItems.runed_stone_bricks.get());
                        pOutput.accept(ModItems.aether_bricks.get());
                        pOutput.accept(ModItems.polished_aether.get());
                        pOutput.accept(ModItems.resonant_floor_tile.get());
                        // === Round 08: 元素色提灯 ===
                        pOutput.accept(ModItems.pyro_lantern.get());
                        pOutput.accept(ModItems.cryo_lantern.get());
                        pOutput.accept(ModItems.storm_lantern.get());
                        pOutput.accept(ModItems.void_lantern.get());
                        // === Round 01: Nexus Core gap fix ===
                        // 此前注册了方块但缺 BlockItem。creative-only 获取。
                        pOutput.accept(ModItems.nexus_core.get());
                        // === 预设法术卷轴 (5 个常用组合) — 直接载入法杖 ===
                        // 解决 creative 拿空白 scroll 无法装入法杖的问题
                        pOutput.accept(makePresetScroll("orb", "fire", null));
                        pOutput.accept(makePresetScroll("beam", "ice", null));
                        pOutput.accept(makePresetScroll("nova", "thunder", null));
                        pOutput.accept(makePresetScroll("arrow", "wind", null));
                        pOutput.accept(makePresetScroll("orb", "void", "amplify"));

                        // === Round 19: Apex Great Spells ===
                        pOutput.accept(ModItems.apex_solar_collapse.get());
                        pOutput.accept(ModItems.apex_blood_pact.get());
                        pOutput.accept(ModItems.apex_cosmic_anchor.get());
                        pOutput.accept(ModItems.apex_void_unmaking.get());

                        // === Round 20: Familiar Pacts ===
                        pOutput.accept(ModItems.pact_aether_wisp.get());
                        pOutput.accept(ModItems.pact_blood_hound.get());
                        pOutput.accept(ModItems.pact_cosmic_owl.get());
                        pOutput.accept(ModItems.pact_tainted_imp.get());

                        // === Round 21: Mana Conduit ===
                        pOutput.accept(ModItems.mana_conduit.get());

                        // === Round 22: Functional Mana Blocks ===
                        pOutput.accept(ModItems.mana_furnace.get());
                        pOutput.accept(ModItems.mana_sentinel.get());
                        pOutput.accept(ModItems.mana_harvester.get());
                        pOutput.accept(ModItems.mana_generator.get());

                        // === Round 24: Aether Realm 入场凭证 ===
                        pOutput.accept(ModItems.aether_travel_stone.get());

                        // === Round 25: Aether Realm 独占资源 ===
                        pOutput.accept(ModItems.aether_essence_ore.get());
                        pOutput.accept(ModItems.aether_essence.get());
                        pOutput.accept(ModItems.aether_ingot.get());

                        // === Round 26: Boss Essence + Transcendence Proof ===
                        pOutput.accept(ModItems.warden_essence.get());
                        pOutput.accept(ModItems.weaver_essence.get());
                        pOutput.accept(ModItems.avatar_essence.get());
                        pOutput.accept(ModItems.transcendence_proof.get());

                        // === Round 27: Spellbook Loadout (5 tier) ===
                        pOutput.accept(ModItems.spellbook_apprentice.get());
                        pOutput.accept(ModItems.spellbook_adept.get());
                        pOutput.accept(ModItems.spellbook_master.get());
                        pOutput.accept(ModItems.spellbook_archon.get());
                        pOutput.accept(ModItems.spellbook_transcendent.get());

                        // === Round 28: Aspect Rings (4) ===
                        pOutput.accept(ModItems.ring_aether.get());
                        pOutput.accept(ModItems.ring_blood.get());
                        pOutput.accept(ModItems.ring_cosmic.get());
                        pOutput.accept(ModItems.ring_tainted.get());

                        // === Round 29: Aspect Swords (4) ===
                        pOutput.accept(ModItems.sword_aether.get());
                        pOutput.accept(ModItems.sword_blood.get());
                        pOutput.accept(ModItems.sword_cosmic.get());
                        pOutput.accept(ModItems.sword_tainted.get());

                        // === Round 30: Ancient Manuscripts (6 lore items) ===
                        pOutput.accept(ModItems.manuscript_world_origin.get());
                        pOutput.accept(ModItems.manuscript_mana_theory.get());
                        pOutput.accept(ModItems.manuscript_aspect_lore.get());
                        pOutput.accept(ModItems.manuscript_boss_lore.get());
                        pOutput.accept(ModItems.manuscript_ascension_lore.get());
                        pOutput.accept(ModItems.manuscript_aether_lore.get());

                        // === Round 02: 数据驱动法术 ===
                        // 从 data/<ns>/spells/*.json 加载，datapack 作者可任意扩展。
                        // 仅在 reload 后该列表才非空 — mod 启动前 displayItems 可能为空。
                        for (com.huige233.transcend.spell.data.SpellDefinition def :
                                com.huige233.transcend.spell.data.SpellDefinitionRegistry.getInstance().getAllSorted()) {
                            pOutput.accept(def.toItemStack());
                        }
                    })
                    .build());

    /** 工厂方法：生成预设 NBT 的法术卷轴 (creative 用,确保能直接载入法杖) */
    private static ItemStack makePresetScroll(String carrier, String element, String effect) {
        ItemStack stack = new ItemStack(ModItems.spell_scroll.get());
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        tag.putString("carrier", carrier);
        tag.putString("element", element);
        tag.putString("effect", effect != null ? effect : "");
        tag.putFloat("base_power", 1.0F);
        tag.putFloat("base_cooldown", 1.0F);
        return stack;
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
