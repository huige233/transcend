package com.huige233.transcend.world.arena.rule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;

/**
 * 竞技场规则模板基类。
 * <p>
 * 通过模板方法统一竞技场行为：
 * 1) 场地生成
 * 2) 玩家周期 Buff
 * 3) 方块保护规则
 * 4) 边界判定与越界处理
 */
public abstract class ArenaRuleTemplate {

    /**
     * 构建竞技场场地。
     */
    public abstract void buildArena(ServerLevel level, ArenaShape shape);

    /**
     * 是否需要保护该方块（禁止放置/破坏）。
     */
    public abstract boolean shouldProtectBlock(LevelAccessor level, BlockPos pos, ArenaShape shape);

    /**
     * 对竞技场内玩家施加周期效果。
     */
    public abstract void applyPeriodicEffects(ServerPlayer player);

    /**
     * 判断玩家是否越界。
     */
    public abstract boolean isOutOfBounds(ServerPlayer player, ArenaShape shape);

    /**
     * 玩家越界时的处理（如传回中心、伤害、减益）。
     */
    public abstract void onOutOfBounds(ServerPlayer player, ArenaShape shape);
}
