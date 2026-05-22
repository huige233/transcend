package com.huige233.transcend.world.mana;

import com.huige233.transcend.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 地脉环境反应系统。
 * 当区块魔力过高或过低时触发环境效果。
 *
 * 魔力过低（&lt;1000，枯竭状态）：
 * - 作物生长减速
 * - 附近玩家获得 Mining Fatigue I
 * - 每60秒5%概率生成1只 Silverfish（地脉虫）
 * - 粒子效果：灰色凋零粒子从地面升起
 *
 * 魔力过高（&gt;8000，魔力泛滥）：
 * - 附近玩家获得 Luck I（运气）
 * - 随机方块发光（短暂的end_rod粒子）
 * - 每60秒10%概率在附近生成魔力水晶物品掉落
 * - 作物生长加速（随机tick）
 * - 如果&gt;10000：偶尔闪电（不着火），Glowing效果给附近生物
 *
 * 魔力极低（&lt;200，严重枯竭）：
 * - 附近玩家获得 Weakness I + Hunger I
 * - 草地缓慢变为泥土（每120秒一个方块）
 * - 5%概率怪物获得 Resistance I（变异强化）
 */
public class LeylineEnvironmentReaction {

    private static final int SEARCH_RADIUS = 16;
    private static final int VERTICAL_RANGE = 8;

    /** 每200tick调用一次（由ManaRegenEvents驱动） */
    public static void tickChunkReaction(ServerLevel level, ChunkPos chunkPos, float mana) {
        // 不在已加载区块中不处理
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) return;

        RandomSource random = level.getRandom();
        BlockPos center = chunkPos.getMiddleBlockPosition(level.getSeaLevel());

        // === 严重枯竭 <200 ===
        if (mana < 200) {
            applyPlayerEffects(level, center, 48, MobEffects.WEAKNESS, 400, 0);
            applyPlayerEffects(level, center, 48, MobEffects.HUNGER, 400, 0);

            // 草地退化
            if (random.nextFloat() < 0.008f) {
                degradeGrass(level, center, random);
            }

            // 怪物变异
            if (random.nextFloat() < 0.05f) {
                buffRandomMob(level, center, random);
            }
            return; // 严重枯竭不触发普通枯竭
        }

        // === 枯竭 <1000 ===
        if (mana < 1000) {
            applyPlayerEffects(level, center, 48, MobEffects.DIG_SLOWDOWN, 400, 0); // Mining Fatigue I

            // 生成地脉虫
            if (random.nextFloat() < 0.05f) {
                spawnLeylineVermin(level, center, random);
            }

            // 枯竭粒子
            spawnDepletionParticles(level, center, random);
            return;
        }

        // === 魔力泛滥 >8000 ===
        if (mana > 8000) {
            applyPlayerEffects(level, center, 48, MobEffects.LUCK, 400, 0);

            // 发光粒子
            spawnOverflowParticles(level, center, random);

            // 随机掉落水晶
            if (random.nextFloat() < 0.10f) {
                dropManaCrystal(level, center, random);
            }

            // 加速作物
            if (random.nextFloat() < 0.3f) {
                tickRandomCrop(level, center, random);
            }
        }

        // === 极高 >10000 ===
        if (mana > 10000) {
            // 偶尔闪电（不着火）
            if (random.nextFloat() < 0.03f) {
                spawnHarmlessLightning(level, center, random);
            }

            // 附近生物发光
            applyMobGlowing(level, center);
        }
    }

    // ============== 辅助方法 ==============

    private static void applyPlayerEffects(ServerLevel level, BlockPos center, int radius,
                                           MobEffect effect, int duration, int amplifier) {
        AABB box = new AABB(center).inflate(radius);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer player : players) {
            if (player.isCreative() || player.isSpectator()) continue;
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
        }
    }

    private static void degradeGrass(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int dz = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z)).below();
            BlockState state = level.getBlockState(surface);
            if (state.is(Blocks.GRASS_BLOCK)) {
                level.setBlockAndUpdate(surface, Blocks.DIRT.defaultBlockState());
                return;
            }
        }
    }

    private static void buffRandomMob(ServerLevel level, BlockPos center, RandomSource random) {
        AABB box = new AABB(center).inflate(SEARCH_RADIUS, VERTICAL_RANGE, SEARCH_RADIUS);
        List<Monster> mobs = level.getEntitiesOfClass(Monster.class, box);
        if (mobs.isEmpty()) return;
        Monster target = mobs.get(random.nextInt(mobs.size()));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, true, true));
    }

    private static void spawnLeylineVermin(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int dz = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));
            if (level.getBlockState(surface).isAir() && level.getBlockState(surface.above()).isAir()) {
                Silverfish vermin = EntityType.SILVERFISH.create(level);
                if (vermin != null) {
                    vermin.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5,
                            random.nextFloat() * 360.0F, 0.0F);
                    level.addFreshEntity(vermin);
                }
                return;
            }
        }
    }

    private static void spawnDepletionParticles(ServerLevel level, BlockPos center, RandomSource random) {
        for (int i = 0; i < 6; i++) {
            double x = center.getX() + (random.nextDouble() - 0.5) * SEARCH_RADIUS * 2;
            double z = center.getZ() + (random.nextDouble() - 0.5) * SEARCH_RADIUS * 2;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                    new BlockPos((int) x, 0, (int) z));
            level.sendParticles(ParticleTypes.ASH,
                    surface.getX() + 0.5, surface.getY() + 0.2, surface.getZ() + 0.5,
                    8, 0.5, 0.5, 0.5, 0.02);
        }
    }

    private static void spawnOverflowParticles(ServerLevel level, BlockPos center, RandomSource random) {
        for (int i = 0; i < 4; i++) {
            double x = center.getX() + (random.nextDouble() - 0.5) * SEARCH_RADIUS * 2;
            double z = center.getZ() + (random.nextDouble() - 0.5) * SEARCH_RADIUS * 2;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                    new BlockPos((int) x, 0, (int) z));
            level.sendParticles(ParticleTypes.END_ROD,
                    surface.getX() + 0.5, surface.getY() + 0.5 + random.nextDouble(), surface.getZ() + 0.5,
                    6, 0.3, 0.5, 0.3, 0.01);
        }
    }

    private static void dropManaCrystal(ServerLevel level, BlockPos center, RandomSource random) {
        int dx = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
        int dz = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
        int x = center.getX() + dx;
        int z = center.getZ() + dz;
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));

        ItemStack stack = new ItemStack(ModItems.magic_crystal.get(), 1);
        ItemEntity item = new ItemEntity(level,
                surface.getX() + 0.5, surface.getY() + 1.0, surface.getZ() + 0.5,
                stack);
        item.setDeltaMovement(
                (random.nextDouble() - 0.5) * 0.1,
                0.2,
                (random.nextDouble() - 0.5) * 0.1);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);

        // 视觉伴随
        level.sendParticles(ParticleTypes.END_ROD,
                surface.getX() + 0.5, surface.getY() + 1.2, surface.getZ() + 0.5,
                12, 0.2, 0.2, 0.2, 0.05);
    }

    private static void tickRandomCrop(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int dy = random.nextInt(VERTICAL_RANGE * 2 + 1) - VERTICAL_RANGE;
            int dz = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            BlockPos pos = center.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock) {
                state.randomTick(level, pos, random);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.0);
                return;
            }
        }
    }

    private static void spawnHarmlessLightning(ServerLevel level, BlockPos center, RandomSource random) {
        int dx = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
        int dz = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
        int x = center.getX() + dx;
        int z = center.getZ() + dz;
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private static void applyMobGlowing(ServerLevel level, BlockPos center) {
        AABB box = new AABB(center).inflate(32);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayer) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, true, false));
        }
    }
}
