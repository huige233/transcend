package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface ScrollEffect {

    boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos);

    int getManaCost();

    int getDuration();
}
