package com.huige233.transcend.world.structure;

import com.huige233.transcend.Transcend;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 法环遗迹结构注册。
 */
public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Transcend.MODID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Transcend.MODID);

    public static final RegistryObject<StructureType<CircleRuinStructure>> CIRCLE_RUIN =
            STRUCTURE_TYPES.register("circle_ruin", () -> () -> CircleRuinStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CIRCLE_RUIN_PIECE =
            PIECE_TYPES.register("circle_ruin_piece",
                () -> (ctx, tag) -> new CircleRuinPiece(ctx, tag));

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        PIECE_TYPES.register(eventBus);
    }
}
