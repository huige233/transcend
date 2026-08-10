package com.huige233.transcend.world.arena.rule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;

/** 竞技场规则模板抽象基类。 */
public abstract class ArenaRuleTemplate {

    public abstract void buildArena(ServerLevel level, ArenaShape shape);

    public abstract boolean shouldProtectBlock(LevelAccessor level, BlockPos pos, ArenaShape shape);

    public abstract void applyPeriodicEffects(ServerPlayer player);

    public abstract boolean isOutOfBounds(ServerPlayer player, ArenaShape shape);

    public abstract void onOutOfBounds(ServerPlayer player, ArenaShape shape);
}
