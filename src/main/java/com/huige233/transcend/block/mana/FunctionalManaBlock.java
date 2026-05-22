package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.TypedManaCrystal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * Round 22: Functional Mana Block — 4 个自动化方块共用基类。
 *
 * <p>每个实例绑定一种 {@link FunctionType}：
 * <ul>
 *   <li>{@link FunctionType#FURNACE} — Aether 驱动，自动熔炼相邻容器</li>
 *   <li>{@link FunctionType#SENTINEL} — Blood 驱动，自动攻击敌怪</li>
 *   <li>{@link FunctionType#HARVESTER} — Cosmic 驱动，自动收割成熟作物</li>
 *   <li>{@link FunctionType#GENERATOR} — Tainted 驱动，从 chunk 环境抽取魔力</li>
 * </ul>
 *
 * <p>所有 sink 类型均从 6 个相邻 ManaConduitBlockEntity 中提取所需 aspect 的 mana 才能运作。
 */
public class FunctionalManaBlock extends Block implements EntityBlock {

    public enum FunctionType {
        FURNACE(TypedManaCrystal.ManaAspect.AETHER, 2, 100),   // 5s, 2 Aether/op
        SENTINEL(TypedManaCrystal.ManaAspect.BLOOD, 1, 20),    // 1s, 1 Blood/op
        HARVESTER(TypedManaCrystal.ManaAspect.COSMIC, 3, 200), // 10s, 3 Cosmic/op
        GENERATOR(TypedManaCrystal.ManaAspect.TAINTED, 0, 100); // 5s, generates Tainted

        public final TypedManaCrystal.ManaAspect aspect;
        public final int manaPerOp;
        public final int cooldownTicks;

        FunctionType(TypedManaCrystal.ManaAspect aspect, int manaPerOp, int cooldownTicks) {
            this.aspect = aspect;
            this.manaPerOp = manaPerOp;
            this.cooldownTicks = cooldownTicks;
        }
    }

    private final FunctionType type;

    public FunctionalManaBlock(FunctionType type) {
        super(Properties.of()
                .mapColor(typeMapColor(type))
                .strength(2.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 6)
                .requiresCorrectToolForDrops());
        this.type = type;
    }

    public FunctionType getFunctionType() {
        return type;
    }

    private static MapColor typeMapColor(FunctionType t) {
        return switch (t) {
            case FURNACE -> MapColor.COLOR_YELLOW;
            case SENTINEL -> MapColor.COLOR_RED;
            case HARVESTER -> MapColor.COLOR_BLUE;
            case GENERATOR -> MapColor.COLOR_PURPLE;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FunctionalManaBlockEntity(pos, state, type);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (level.isClientSide) return null;
        return beType == ModBlockEntities.FUNCTIONAL_MANA_BE.get()
                ? (lvl, pos, st, be) -> FunctionalManaBlockEntity.serverTick(lvl, pos, st, (FunctionalManaBlockEntity) be)
                : null;
    }
}
