package com.huige233.transcend.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 法环遗迹结构片段 — 实际放置方块的组件。
 */
public class CircleRuinPiece extends StructurePiece {

    private final BlockPos center;
    private final int tier;

    public CircleRuinPiece(BlockPos center, int tier, RandomSource random) {
        super(ModStructures.CIRCLE_RUIN_PIECE.get(), 0, makeBB(center, tier));
        this.center = center;
        this.tier = tier;
    }

    public CircleRuinPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.CIRCLE_RUIN_PIECE.get(), tag);
        this.center = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.tier = tag.getInt("Tier");
    }

    private static BoundingBox makeBB(BlockPos center, int tier) {
        int r = tier <= 1 ? 3 : tier <= 2 ? 5 : 9;
        return new BoundingBox(
            center.getX() - r, center.getY() - 1, center.getZ() - r,
            center.getX() + r, center.getY() + 5, center.getZ() + r
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", center.getX());
        tag.putInt("CY", center.getY());
        tag.putInt("CZ", center.getZ());
        tag.putInt("Tier", tier);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                           ChunkGenerator generator, RandomSource random,
                           BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        // Use the server level for block placement
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        java.util.Random javaRandom = new java.util.Random(random.nextLong());
        switch (tier) {
            case 1 -> CircleRuinGenerator.generateInitiateRuin(serverLevel, center, javaRandom);
            case 2 -> CircleRuinGenerator.generateAdeptRuin(serverLevel, center, javaRandom);
            case 3 -> CircleRuinGenerator.generateObservatoryRuin(serverLevel, center, javaRandom);
        }
    }
}
