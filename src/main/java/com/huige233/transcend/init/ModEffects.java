package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.effect.AntiHealEffect;
import com.huige233.transcend.effect.AnnihilationEffect;
import com.huige233.transcend.effect.MagicWoundEffect;
import com.huige233.transcend.effect.SoulShockEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Transcend.MODID);

    public static final RegistryObject<MobEffect> ANTI_HEAL =
            MOB_EFFECTS.register("anti_heal", AntiHealEffect::new);

    public static final RegistryObject<MobEffect> ANNIHILATION =
            MOB_EFFECTS.register("annihilation", AnnihilationEffect::new);

    public static final RegistryObject<MobEffect> SOUL_SHOCK =
            MOB_EFFECTS.register("soul_shock", SoulShockEffect::new);

    /** Round 37: 魔伤痕 — 禁卷代价 / 标志性 debuff (受伤 +25%/级) */
    public static final RegistryObject<MobEffect> MAGIC_WOUND =
            MOB_EFFECTS.register("magic_wound", MagicWoundEffect::new);

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
