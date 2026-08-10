package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/** 禁戒虚空采掘卷轴效果实现。 */
public class ForbiddenHollowQuarryEffect implements ScrollEffect {

    private static final int HALF = 4;
    private static final int MAX_BLOCKS = 64;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {

        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, HALF + 1, 0.55F, 0.35F, 0.15F, 400, "pentagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shockwave(level, pos, HALF + 2, 0.4F, 0.25F, 0.1F, 400);

        int destroyed = 0;
        int y = pos.getY() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Vec3 dropAt = Vec3.atCenterOf(pos);

        outer:
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                cursor.set(pos.getX() + dx, y, pos.getZ() + dz);
                BlockState state = level.getBlockState(cursor);
                if (state.isAir()) continue;
                if (!isStoneLike(state)) continue;
                if (state.getDestroySpeed(level, cursor) < 0) continue;

                BlockPos immutable = cursor.immutable();

                for (ItemStack drop : Block.getDrops(state, level, immutable, level.getBlockEntity(immutable))) {
                    if (!drop.isEmpty()) {
                        ItemEntity ie = new ItemEntity(level, dropAt.x, dropAt.y, dropAt.z, drop);
                        ie.setDefaultPickUpDelay();
                        level.addFreshEntity(ie);
                    }
                }
                level.removeBlock(immutable, false);
                destroyed++;
                if (destroyed >= MAX_BLOCKS) break outer;
            }
        }

        caster.addEffect(com.huige233.transcend.effect.MagicWoundEffect.defaultInstance());
        return destroyed > 0;
    }

    private static boolean isStoneLike(BlockState state) {
        if (state.is(Tags.Blocks.STONE)) return true;
        if (state.is(Tags.Blocks.COBBLESTONE)) return true;
        if (state.is(Tags.Blocks.ORES)) return true;
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)) return true;
        if (state.is(BlockTags.BASE_STONE_NETHER)) return true;
        return false;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.forbidden_hollow_quarry_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
