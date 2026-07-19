package com.huige233.transcend.circle;

import com.huige233.transcend.block.circle.CatalystPlinthBlock;
import com.huige233.transcend.block.circle.CircleFoundationBlock;
import com.huige233.transcend.block.circle.CircleRuneBlock;
import com.huige233.transcend.block.circle.LeylineConduitBlock;
import com.huige233.transcend.block.circle.MagicCircleCoreBlock;
import com.huige233.transcend.block.circle.PillarCapBlock;
import com.huige233.transcend.block.circle.RunicPillarBlock;
import com.huige233.transcend.circle.CircleStructurePattern.PatternEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class CircleStructureValidator {

    public static CircleStructureCache validate(Level level, BlockPos corePos) {
        for (CircleTier tier : new CircleTier[]{
                CircleTier.PRIMORDIAL,
                CircleTier.ARCHON,
                CircleTier.MASTER,
                CircleTier.ADEPT,
                CircleTier.INITIATE
        }) {
            CircleStructureCache result = validateTier(level, corePos, tier);
            if (result.isValid()) {
                return result;
            }
        }
        return CircleStructureCache.invalid(level.getGameTime());
    }

    public static CircleStructureCache validateForTier(Level level, BlockPos corePos, CircleTier tier) {
        return validateTier(level, corePos, tier);
    }

    private static CircleStructureCache validateTier(Level level, BlockPos corePos, CircleTier tier) {
        List<PatternEntry> pattern = CircleStructurePattern.getPatternForTier(tier);
        List<BlockPos> missing = new ArrayList<>();
        List<CircleStructureCache.MissingEntry> missingEntries = new ArrayList<>();

        for (PatternEntry entry : pattern) {
            BlockPos checkPos = corePos.offset(entry.dx(), entry.dy(), entry.dz());
            if (!isValidBlock(level, checkPos, entry)) {
                missing.add(checkPos);
                missingEntries.add(new CircleStructureCache.MissingEntry(checkPos, entry.role(), entry.minBlockTier()));
            }
        }

        boolean valid = missing.isEmpty();
        return new CircleStructureCache(valid ? tier : null, valid,
                List.copyOf(missing), List.copyOf(missingEntries), level.getGameTime());
    }

    private static boolean isValidBlock(Level level, BlockPos pos, PatternEntry entry) {
        Block block = level.getBlockState(pos).getBlock();

        return switch (entry.role()) {
            case FOUNDATION -> block instanceof CircleFoundationBlock fb && fb.getTier() >= entry.minBlockTier();
            case RUNE -> block instanceof CircleRuneBlock rb && rb.getTier() >= entry.minBlockTier();
            case CATALYST_PLINTH -> block instanceof CatalystPlinthBlock;
            case CONDUIT -> block instanceof LeylineConduitBlock cb && cb.getTier() >= entry.minBlockTier();
            case PILLAR -> block instanceof RunicPillarBlock pb && pb.getTier() >= entry.minBlockTier();
            case PILLAR_CAP -> block instanceof PillarCapBlock;
            case CORE -> block instanceof MagicCircleCoreBlock;
        };
    }
}
