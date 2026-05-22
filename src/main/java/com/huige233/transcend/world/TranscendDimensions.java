package com.huige233.transcend.world;

import com.huige233.transcend.Transcend;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class TranscendDimensions {

    public static final ResourceKey<Level> ARENA_LEVEL =
            ResourceKey.create(Registries.DIMENSION, Transcend.rl("arena"));

    public static final ResourceKey<Level> NEXUS_LEVEL =
            ResourceKey.create(Registries.DIMENSION, Transcend.rl("nexus"));

    private TranscendDimensions() {
    }
}

