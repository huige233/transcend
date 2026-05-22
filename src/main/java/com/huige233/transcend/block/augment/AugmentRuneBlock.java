package com.huige233.transcend.block.augment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * 法环增幅符文：被法环核心每秒一次的扫描所计数，
 * 同类型符文每多放一块就给法环 +1 对应强化等级（{@link AugmentType}），同类型上限由核心侧约束。
 * 本方块本身不携带 BE、不消耗 mana，可即时摆放和撤除。
 */
public class AugmentRuneBlock extends Block {

    /** 单一符文对应的法环强化通道；mapColor 仅控制地图显示色。 */
    public enum AugmentType {
        HASTE(MapColor.COLOR_RED),
        EFFICIENCY(MapColor.COLOR_GREEN),
        PRESERVATION(MapColor.COLOR_BLUE);

        public final MapColor mapColor;

        AugmentType(MapColor mapColor) {
            this.mapColor = mapColor;
        }
    }

    private static final VoxelShape SHAPE = Shapes.box(0.1875, 0.0, 0.1875, 0.8125, 0.5, 0.8125);

    private final AugmentType type;

    public AugmentRuneBlock(AugmentType type) {
        super(Properties.of()
                .mapColor(type.mapColor)
                .strength(2.5F, 5.0F)
                .sound(SoundType.AMETHYST_CLUSTER)
                .lightLevel(s -> 9)
                .noOcclusion()
                .randomTicks());
        this.type = type;
    }

    public AugmentType getAugmentType() {
        return type;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    /**
     * 客户端 random tick：按概率发射本符文类型对应的氛围粒子，
     * 让玩家能在世界中直观看到符文仍处于"活跃可计数"的状态。
     */
    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        if (rand.nextInt(4) != 0) return;
        ParticleOptions particle = switch (type) {
            case HASTE -> ParticleTypes.CRIT;
            case EFFICIENCY -> ParticleTypes.HAPPY_VILLAGER;
            case PRESERVATION -> ParticleTypes.ENCHANT;
        };
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.6;
        double cz = pos.getZ() + 0.5;
        double angle = rand.nextDouble() * Math.PI * 2;
        double r = 0.25 + rand.nextDouble() * 0.15;
        level.addParticle(particle,
                cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r,
                0, 0.04, 0);
    }
}
