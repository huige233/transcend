package com.huige233.transcend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


/** 保留旧版简易发电机实体入口，并使用外部传入的注册类型复用通用发电实现。 */
public class SimpleGeneratorBlockEntity extends FEGeneratorBlockEntity {
    public SimpleGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                      int production, Mode mode) {
        super(type, pos, state, production, mode);
    }
}
