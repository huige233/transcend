package com.huige233.transcend.world.arena.rule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * 默认竞技场规则实现。
 * <p>
 * 保持当前项目已有竞技场行为，并以模板方式抽离：
 * - 黑曜石/黑石竞技场结构
 * - 场内持续夜视、抗火、抗性
 * - 越界回拉 + 伤害 + 虚弱
 * - 场地方块保护
 */
public class DefaultArenaRuleTemplate extends ArenaRuleTemplate {

    @Override
    public void buildArena(ServerLevel level, ArenaShape shape) {
        int min = -shape.wallRadius() - 1;
        int max = shape.wallRadius() + 1;

        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                double dist = Math.sqrt((double) x * x + (double) z * z);

                BlockPos floorPos = new BlockPos(x, shape.arenaY(), z);
                if (dist <= shape.platformRadius()) {
                    if (dist > shape.platformRadius() - 2) {
                        level.setBlock(floorPos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                    } else {
                        level.setBlock(floorPos, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
                    }
                } else if (dist <= shape.wallRadius()) {
                    level.setBlock(floorPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                }

                if (dist <= shape.wallRadius() + 1) {
                    for (int y = shape.arenaY() + 1; y <= shape.arenaY() + 18; y++) {
                        BlockPos air = new BlockPos(x, y, z);
                        if (!level.getBlockState(air).isAir()) {
                            level.setBlock(air, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }

                if (dist > shape.wallRadius() - 0.8 && dist <= shape.wallRadius() + 0.4) {
                    for (int y = shape.arenaY() + 1; y <= shape.arenaY() + shape.wallHeight(); y++) {
                        BlockPos wallPos = new BlockPos(x, y, z);
                        level.setBlock(wallPos, Blocks.TINTED_GLASS.defaultBlockState(), 3);
                    }
                }
            }
        }

        level.setBlock(new BlockPos(shape.centerX(), shape.arenaY() + 1, shape.centerZ()),
                Blocks.LODESTONE.defaultBlockState(), 3);
        level.setBlock(new BlockPos(shape.centerX(), shape.arenaY() + 2, shape.centerZ()),
                Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean shouldProtectBlock(LevelAccessor level, BlockPos pos, ArenaShape shape) {
        if (level == null || pos == null || !(level instanceof Level realLevel)) {
            return false;
        }
        double dx = pos.getX() + 0.5 - shape.centerX();
        double dz = pos.getZ() + 0.5 - shape.centerZ();
        return Math.sqrt(dx * dx + dz * dz) <= shape.wallRadius() + 2
                && pos.getY() >= shape.arenaY() - 4
                && pos.getY() <= shape.arenaY() + shape.wallHeight() + 4;
    }

    @Override
    public void applyPeriodicEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 220, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false, true));
    }

    @Override
    public boolean isOutOfBounds(ServerPlayer player, ArenaShape shape) {
        double dx = player.getX() - shape.centerX();
        double dz = player.getZ() - shape.centerZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        return dist > shape.hardRadius()
                || player.getY() < shape.arenaY() - 6
                || player.getY() > shape.arenaY() + 30;
    }

    @Override
    public void onOutOfBounds(ServerPlayer player, ArenaShape shape) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        player.teleportTo(level, shape.centerX() + 0.5, shape.arenaY() + 2.0, shape.centerZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0F;
        player.hurt(player.damageSources().magic(), 4.0F);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true));
    }
}
