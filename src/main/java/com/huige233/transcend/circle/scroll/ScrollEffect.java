package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 古法咒卷效果接口。所有咒卷效果实现该接口。
 */
public interface ScrollEffect {

    /**
     * 执行秘卷效果。
     *
     * @param level  服务端世界
     * @param caster 施法者
     * @param pos    施法位置
     * @return true 表示成功执行（应消耗咒卷），false 表示失败
     */
    boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos);

    /** 魔力消耗量（CM） */
    int getManaCost();

    /** 效果持续时间（tick），0 = 瞬时 */
    int getDuration();
}
