package com.huige233.transcend.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * 法环遗迹结构定义。
 * 使用程序化方块放置而非NBT模板。
 */
public class CircleRuinStructure extends Structure {

    public static final Codec<CircleRuinStructure> CODEC = RecordCodecBuilder.<CircleRuinStructure>mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            Codec.INT.fieldOf("tier").forGetter(s -> s.tier)
        ).apply(instance, CircleRuinStructure::new)
    ).codec();

    private final int tier;

    public CircleRuinStructure(StructureSettings settings, int tier) {
        super(settings);
        this.tier = tier;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // Find a valid surface position
        BlockPos pos = new BlockPos(
            context.chunkPos().getMiddleBlockX(),
            0,
            context.chunkPos().getMiddleBlockZ()
        );

        int y = context.chunkGenerator().getFirstOccupiedHeight(
            pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
            context.heightAccessor(), context.randomState()
        );

        BlockPos surfacePos = new BlockPos(pos.getX(), y, pos.getZ());

        return Optional.of(new GenerationStub(surfacePos, builder -> {
            builder.addPiece(new CircleRuinPiece(surfacePos, tier, context.random()));
        }));
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CIRCLE_RUIN.get();
    }
}
