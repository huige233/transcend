package com.huige233.transcend.block.circle;

import com.huige233.transcend.Transcend;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID)
/** 法阵结构变更事件处理。 */
public class CircleStructureChangeHandler {

    private static final int SEARCH_RADIUS = 16;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        markNearbyCoresDirty(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        markNearbyCoresDirty(event.getLevel(), event.getPos());
    }

    private static void markNearbyCoresDirty(LevelAccessor level, BlockPos changedPos) {
        if (level.isClientSide()) return;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int r = SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {

                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    cursor.set(changedPos.getX() + dx, changedPos.getY() + dy, changedPos.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof MagicCircleCoreBlockEntity core) {
                        core.markStructureDirty();
                    }
                }
            }
        }
    }
}
