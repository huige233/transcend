package com.huige233.transcend;

import com.huige233.transcend.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** 法术相关创造模式物品栏标签页。 */
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

                        output.accept(ModItems.spellbook_apprentice.get());
                        output.accept(ModItems.spellbook_adept.get());
                        output.accept(ModItems.spellbook_master.get());
                        output.accept(ModItems.spellbook_archon.get());
                        output.accept(ModItems.spellbook_transcendent.get());

                        output.accept(ModItems.wand_basic.get());
                        output.accept(ModItems.wand_advanced.get());
                        output.accept(ModItems.wand_master.get());
                        output.accept(ModItems.wand_expert.get());
                        output.accept(ModItems.wand_legendary.get());

                        output.accept(ModItems.spell_scroll.get());
                        output.accept(ModItems.sealed_scroll.get());
                        output.accept(ModItems.spell_upgrade_stone.get());

                        output.accept(ModItems.spell_base_basic.get());
                        output.accept(ModItems.spell_base_advanced.get());
                        output.accept(ModItems.spell_base_master.get());

                        output.accept(ModItems.carrier_orb.get());
                        output.accept(ModItems.carrier_arrow.get());
                        output.accept(ModItems.carrier_slash.get());
                        output.accept(ModItems.carrier_beam.get());
                        output.accept(ModItems.carrier_nova.get());
                        output.accept(ModItems.carrier_chain.get());
                        output.accept(ModItems.carrier_vortex.get());
                        output.accept(ModItems.carrier_trap.get());
                        output.accept(ModItems.carrier_barrier.get());
                        output.accept(ModItems.carrier_rain.get());
                        output.accept(ModItems.carrier_dash.get());

                        output.accept(ModItems.element_metal.get());
                        output.accept(ModItems.element_wood.get());
                        output.accept(ModItems.element_water.get());
                        output.accept(ModItems.element_fire.get());
                        output.accept(ModItems.element_earth.get());
                        output.accept(ModItems.element_chaos.get());

                        output.accept(ModItems.effect_explosion.get());
                        output.accept(ModItems.effect_piercing.get());
                        output.accept(ModItems.effect_split.get());
                        output.accept(ModItems.effect_homing.get());
                        output.accept(ModItems.effect_healing.get());
                        output.accept(ModItems.effect_shield.get());
                        output.accept(ModItems.effect_chain_lightning.get());
                        output.accept(ModItems.effect_amplify.get());
                        output.accept(ModItems.effect_lifesteal.get());
                        output.accept(ModItems.effect_multishot.get());
                        output.accept(ModItems.effect_slowfield.get());
                        output.accept(ModItems.effect_mark.get());
                        output.accept(ModItems.effect_root.get());
                        output.accept(ModItems.effect_blight.get());
                        output.accept(ModItems.effect_curse.get());
                        output.accept(ModItems.effect_overload.get());
                        output.accept(ModItems.effect_shatter.get());

                        output.accept(ModItems.glyph_amplify.get());
                        output.accept(ModItems.glyph_dampen.get());
                        output.accept(ModItems.glyph_quickfire.get());
                        output.accept(ModItems.glyph_split.get());
                        output.accept(ModItems.glyph_pierce.get());
                        output.accept(ModItems.glyph_chain.get());
                        output.accept(ModItems.glyph_extend.get());
                        output.accept(ModItems.glyph_homing.get());

                        output.accept(ModItems.rune_mana_siphon.get());
                        output.accept(ModItems.rune_rapid_fire.get());
                        output.accept(ModItems.rune_overcharge.get());
                        output.accept(ModItems.rune_spell_echo.get());
                        output.accept(ModItems.rune_elemental_mastery.get());
                        output.accept(ModItems.rune_glass_cannon.get());
                        output.accept(ModItems.rune_conservation.get());
                        output.accept(ModItems.rune_chain_caster.get());

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
