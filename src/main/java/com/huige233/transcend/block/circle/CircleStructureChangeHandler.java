package com.huige233.transcend.block.circle;

import com.huige233.transcend.Transcend;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Round 44: 修复"等级下降不检测"bug。
 *
 * <p>之前 {@link MagicCircleCoreBlockEntity#markStructureDirty()} 仅在
 * `AttunementChiselItem` / `CircleArchitectWandItem` / 世界加载时调用 — 玩家手动破坏
 * 结构方块时 BE 不知道，{@code detectedTier} 永远不下调，导致已损坏的结构仍享有
 * 高 tier 权益。
 *
 * <p>本类监听 Forge {@link BlockEvent.BreakEvent} 和 {@link BlockEvent.EntityPlaceEvent}：
 * 任何方块改变 → 扫描 {@link #SEARCH_RADIUS} 块内所有 MagicCircleCoreBlockEntity →
 * 调 {@code markStructureDirty()}。下次 server tick 它们会重新 validate。
 *
 * <p>性能：每事件 O(R^3) 即 ~17×17×17 = 4913 次 chunkBE 查找，但 Forge 内部用
 * chunk-bound entity map，实际开销很低。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID)
public class CircleStructureChangeHandler {

    /** 法环结构最大半径（PRIMORDIAL T5 占地 17×17 + 高度 9，搜索半径取 16 包含全部）*/
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
        // 扫描 changedPos 周围 SEARCH_RADIUS 球体内所有 BE
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int r = SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    // 圆球剔除 — 减少 ~50% 检查
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
