package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Round 43: 独立的法术 Creative Tab — 把所有法术构成项与 wand/rune/enhance/glyph
 * 从主 TranscendTab 中分出来，让创造栏不再臃肿。
 *
 * <p>顺序按学习路径：spellbook → wand → scroll → carrier → element → effect → augment glyph → rune → enhance。
 */
public class TranscendSpellTab {
    public static final String TAB_TITLE = "creativetab.transcend.spells";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Transcend.MODID);

    public static final RegistryObject<CreativeModeTab> SPELL_TAB = CREATIVE_MODE_TABS.register("spell_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.spellbook_master.get()))
                    .title(Component.translatable(TAB_TITLE))
                    .withTabsBefore(TranscendTab.TRANSCEND_TAB.getId())
                    .displayItems((params, output) -> {
                        // ─── Spellbooks (5 阶) ───
                        output.accept(ModItems.spellbook_apprentice.get());
                        output.accept(ModItems.spellbook_adept.get());
                        output.accept(ModItems.spellbook_master.get());
                        output.accept(ModItems.spellbook_archon.get());
                        output.accept(ModItems.spellbook_transcendent.get());

                        // ─── Wands (3 阶) ───
                        output.accept(ModItems.wand_basic.get());
                        output.accept(ModItems.wand_advanced.get());
                        output.accept(ModItems.wand_master.get());

                        // ─── Scrolls ───
                        output.accept(ModItems.spell_scroll.get());
                        output.accept(ModItems.sealed_scroll.get());
                        output.accept(ModItems.spell_upgrade_stone.get());

                        // ─── Spell Bases ───
                        output.accept(ModItems.spell_base_basic.get());
                        output.accept(ModItems.spell_base_advanced.get());
                        output.accept(ModItems.spell_base_master.get());

                        // ─── Carriers (17) ───
                        output.accept(ModItems.carrier_orb.get());
                        output.accept(ModItems.carrier_arrow.get());
                        output.accept(ModItems.carrier_slash.get());
                        output.accept(ModItems.carrier_beam.get());
                        output.accept(ModItems.carrier_nova.get());
                        output.accept(ModItems.carrier_chain.get());
                        output.accept(ModItems.carrier_vortex.get());
                        output.accept(ModItems.carrier_spike.get());
                        output.accept(ModItems.carrier_teleport.get());
                        output.accept(ModItems.carrier_trap.get());
                        output.accept(ModItems.carrier_barrier.get());
                        output.accept(ModItems.carrier_summon.get());
                        output.accept(ModItems.carrier_ring.get());
                        output.accept(ModItems.carrier_breath.get());
                        output.accept(ModItems.carrier_rain.get());
                        output.accept(ModItems.carrier_dash.get());
                        output.accept(ModItems.carrier_ground.get());

                        // ─── Elements (18) ───
                        output.accept(ModItems.element_fire.get());
                        output.accept(ModItems.element_ice.get());
                        output.accept(ModItems.element_thunder.get());
                        output.accept(ModItems.element_wind.get());
                        output.accept(ModItems.element_earth.get());
                        output.accept(ModItems.element_void.get());
                        output.accept(ModItems.element_holy.get());
                        output.accept(ModItems.element_blood.get());
                        output.accept(ModItems.element_dark.get());
                        output.accept(ModItems.element_light.get());
                        output.accept(ModItems.element_poison.get());
                        output.accept(ModItems.element_time.get());
                        output.accept(ModItems.element_space.get());
                        output.accept(ModItems.element_nature.get());
                        output.accept(ModItems.element_chaos.get());
                        output.accept(ModItems.element_acid.get());
                        output.accept(ModItems.element_sonic.get());
                        output.accept(ModItems.element_eldritch.get());

                        // ─── Effects (~31) ───
                        output.accept(ModItems.effect_explosion.get());
                        output.accept(ModItems.effect_piercing.get());
                        output.accept(ModItems.effect_split.get());
                        output.accept(ModItems.effect_homing.get());
                        output.accept(ModItems.effect_healing.get());
                        output.accept(ModItems.effect_shield.get());
                        output.accept(ModItems.effect_chain_lightning.get());
                        output.accept(ModItems.effect_bounce.get());
                        output.accept(ModItems.effect_delayed.get());
                        output.accept(ModItems.effect_amplify.get());
                        output.accept(ModItems.effect_lifesteal.get());
                        output.accept(ModItems.effect_quickcast.get());
                        output.accept(ModItems.effect_multishot.get());
                        output.accept(ModItems.effect_slowfield.get());
                        output.accept(ModItems.effect_gravity_well.get());
                        output.accept(ModItems.effect_mark.get());
                        output.accept(ModItems.effect_echo.get());
                        output.accept(ModItems.effect_armor_break.get());
                        output.accept(ModItems.effect_root.get());
                        output.accept(ModItems.effect_blight.get());
                        output.accept(ModItems.effect_lingering.get());
                        output.accept(ModItems.effect_devour.get());
                        output.accept(ModItems.effect_absorb.get());
                        output.accept(ModItems.effect_reflect.get());
                        output.accept(ModItems.effect_curse.get());
                        output.accept(ModItems.effect_overload.get());
                        output.accept(ModItems.effect_weaken.get());
                        output.accept(ModItems.effect_unstable.get());
                        output.accept(ModItems.effect_shatter.get());
                        output.accept(ModItems.effect_summon_wisp.get());
                        output.accept(ModItems.effect_summon_guardian.get());

                        // ─── Augment Glyphs (8) — Round 43 新 ───
                        output.accept(ModItems.glyph_amplify.get());
                        output.accept(ModItems.glyph_dampen.get());
                        output.accept(ModItems.glyph_quickfire.get());
                        output.accept(ModItems.glyph_split.get());
                        output.accept(ModItems.glyph_pierce.get());
                        output.accept(ModItems.glyph_chain.get());
                        output.accept(ModItems.glyph_extend.get());
                        output.accept(ModItems.glyph_homing.get());

                        // ─── Runes (8) ───
                        output.accept(ModItems.rune_mana_siphon.get());
                        output.accept(ModItems.rune_rapid_fire.get());
                        output.accept(ModItems.rune_overcharge.get());
                        output.accept(ModItems.rune_spell_echo.get());
                        output.accept(ModItems.rune_elemental_mastery.get());
                        output.accept(ModItems.rune_glass_cannon.get());
                        output.accept(ModItems.rune_conservation.get());
                        output.accept(ModItems.rune_chain_caster.get());

                        // ─── Enhance Stones (4) ───
                        output.accept(ModItems.enhance_power.get());
                        output.accept(ModItems.enhance_duration.get());
                        output.accept(ModItems.enhance_efficiency.get());
                        output.accept(ModItems.enhance_special.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
