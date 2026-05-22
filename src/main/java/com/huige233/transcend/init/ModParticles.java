package com.huige233.transcend.init;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.particle.TranscendDustParticleOptions;
import com.huige233.transcend.particle.TranscendGlitterParticleOptions;
import com.huige233.transcend.particle.TranscendRuneParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Transcend.MODID);

    public static final RegistryObject<ParticleType<TranscendDustParticleOptions>> TRANSCEND_DUST =
            PARTICLE_TYPES.register("transcend_dust",
                    () -> TranscendDustParticleOptions.TYPE);

    public static final RegistryObject<ParticleType<TranscendRuneParticleOptions>> TRANSCEND_RUNE =
            PARTICLE_TYPES.register("transcend_rune",
                    () -> TranscendRuneParticleOptions.TYPE);

    public static final RegistryObject<ParticleType<TranscendGlitterParticleOptions>> TRANSCEND_GLITTER =
            PARTICLE_TYPES.register("transcend_glitter",
                    () -> TranscendGlitterParticleOptions.TYPE);

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
