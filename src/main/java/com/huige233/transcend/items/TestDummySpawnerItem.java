package com.huige233.transcend.items;

import com.huige233.transcend.entity.TestDummy;
import com.huige233.transcend.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** 木桩测试生成器物品。 */
public final class TestDummySpawnerItem extends Item {
    public TestDummySpawnerItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer creator)) {
            return InteractionResult.FAIL;
        }

        TestDummy dummy = ModEntities.TEST_DUMMY.get().create(level);
        if (dummy == null || !dummy.bindToCreator(creator)) {
            return InteractionResult.FAIL;
        }
        var spawnPos = context.getClickedPos().relative(context.getClickedFace());
        dummy.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                creator.getYRot(), 0.0F);
        if (!level.addFreshEntity(dummy)) {
            return InteractionResult.FAIL;
        }
        if (!creator.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
