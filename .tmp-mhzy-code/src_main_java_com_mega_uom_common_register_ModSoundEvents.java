package com.mega.uom.common.register;

import com.mega.uom.ModSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ModSource.MODID);
    public static final RegistryObject<SoundEvent> FLOWER_TRANSFORMING = SOUNDS.register("flower_transforming", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "flower_transforming")));
    public static final RegistryObject<SoundEvent> FLOWER_TRANSFORMING_END = SOUNDS.register("flower_transforming_end", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "flower_transforming_end")));
    public static final RegistryObject<SoundEvent> TIME_STOP = SOUNDS.register("time_stop", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "time_stop")));
    public static final RegistryObject<SoundEvent> COMBO_1 = SOUNDS.register("sword_combo_0", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "sword_combo_0")));
    public static final RegistryObject<SoundEvent> FLASH_2 = SOUNDS.register("sword_flash_1", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "sword_flash_1")));
    public static final RegistryObject<SoundEvent> FLASH_1 = SOUNDS.register("sword_flash_0", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "sword_flash_0")));
    public static final RegistryObject<SoundEvent> FLASH_BASE = SOUNDS.register("sword_flashbase", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "sword_flashbase")));
    public static final RegistryObject<SoundEvent> SOLTRON_COLLAPSING_FINISH = SOUNDS.register("soltron_collapsing_finish", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("fantasy_ending", "soltron_collapsing_finish")));
    public static final RegistryObject<SoundEvent> ALONE_IN_THE_DARK_BGM = SOUNDS.register("alone_in_the_dark", () -> SoundEvent.createFixedRangeEvent(new ResourceLocation("fantasy_ending", "alone_in_the_dark"), 1.0F));
    public static final RegistryObject<SoundEvent> ONLY_WISH_BGM = SOUNDS.register("only_wish", () -> SoundEvent.createFixedRangeEvent(new ResourceLocation("fantasy_ending", "only_wish"), 1.0F));
}
