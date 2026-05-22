package com.huige233.transcend.mixin;

import com.google.common.collect.EvictingQueue;
import com.huige233.transcend.config.TranscendParticleConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Redirect(
            method = "tick()V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;")
    )
    private Object transcend$expandQueue(Map<ParticleRenderType, Queue<Particle>> map,
                                          Object key,
                                          Function<ParticleRenderType, Queue<Particle>> mappingFunction) {
        ParticleRenderType renderType = (ParticleRenderType) key;
        if (TranscendParticleConfig.isParticleCountInjectEnabled()) {
            int limit = TranscendParticleConfig.getParticleCountLimit();
            return map.computeIfAbsent(renderType, (type) -> EvictingQueue.create(limit));
        }
        return map.computeIfAbsent(renderType, mappingFunction);
    }
}
