package com.huige233.transcend.world.arena;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import com.huige233.transcend.entity.boss.TranscendenceAvatar;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.world.TranscendDimensions;
import com.huige233.transcend.world.arena.rule.ArenaRuleTemplate;
import com.huige233.transcend.world.arena.rule.ArenaShape;
import com.huige233.transcend.world.arena.rule.DefaultArenaRuleTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class TranscendArenaManager {

    public static final int ARENA_Y = 96;
    public static final int PLATFORM_RADIUS = 36;
    public static final int WALL_RADIUS = 40;
    public static final int HARD_RADIUS = 46;
    public static final int WALL_HEIGHT = 8;
    private static final ArenaShape SHAPE = new ArenaShape(0, 0, ARENA_Y, PLATFORM_RADIUS, WALL_RADIUS, HARD_RADIUS, WALL_HEIGHT);
    private static final ArenaRuleTemplate RULES = new DefaultArenaRuleTemplate();

    private static final int ENTRY_RADIUS = 20;
    private static final String TAG_ACTIVE = "transcend_arena_active";
    private static final String TAG_RETURN_DIM = "transcend_arena_return_dim";
    private static final String TAG_RETURN_X = "transcend_arena_return_x";
    private static final String TAG_RETURN_Y = "transcend_arena_return_y";
    private static final String TAG_RETURN_Z = "transcend_arena_return_z";
    private static final String TAG_RETURN_YAW = "transcend_arena_return_yaw";
    private static final String TAG_RETURN_PITCH = "transcend_arena_return_pitch";
    private static final String TAG_CALM = "transcend_arena_calm_ticks";
    private static final String TAG_PENALTY_CD = "transcend_arena_penalty_cd";
    private static final String TAG_NOTICE_CD = "transcend_arena_notice_cd";

    private TranscendArenaManager() {
    }

    // ─── Arena Structure ─────────────────────────────────────────────

    /**
     * 确保竞技场结构已生成（幂等）。
     * 在区块加载、玩家进入等场景调用。
     */
    public static void ensureArena(ServerLevel arenaLevel) {
        ArenaSavedData data = ArenaSavedData.get(arenaLevel);
        if (!data.isBuilt()) {
            // 先标记，防止 getChunkAt 触发 onChunkLoad 时重入
            data.markBuilt();
            // 竞技场范围 -(WALL_RADIUS+1) ~ +(WALL_RADIUS+1)，需要预加载所有涉及区块
            int blockRadius = WALL_RADIUS + 1;
            int minChunk = (-blockRadius) >> 4;
            int maxChunk = blockRadius >> 4;
            for (int cx = minChunk; cx <= maxChunk; cx++) {
                for (int cz = minChunk; cz <= maxChunk; cz++) {
                    arenaLevel.getChunkAt(new BlockPos(cx << 4, ARENA_Y, cz << 4));
                }
            }
            RULES.buildArena(arenaLevel, SHAPE);
            Transcend.LOGGER.info("Arena structure built in dimension {}", arenaLevel.dimension().location());
        }
    }

    /**
     * 区块加载时检查是否需要生成竞技场。
     * 只在竞技场中心所在区块触发。
     */
    public static void onChunkLoad(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        if (level.dimension() != TranscendDimensions.ARENA_LEVEL) return;
        // 竞技场中心 (0, ARENA_Y, 0) 所在区块
        int cx = 0 >> 4;
        int cz = 0 >> 4;
        if (chunk.x == cx && chunk.z == cz) {
            ensureArena(level);
        }
    }

    // ─── Entry ───────────────────────────────────────────────────────

    public static boolean enterArena(ServerPlayer activator) {
        if (activator == null || !(activator.level() instanceof ServerLevel sourceLevel)) {
            return false;
        }

        MinecraftServer server = sourceLevel.getServer();
        ServerLevel arenaLevel = server.getLevel(TranscendDimensions.ARENA_LEVEL);
        if (arenaLevel == null) {
            activator.sendSystemMessage(Component.translatable("msg.transcend.ritual_arena_unavailable")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ensureArena(arenaLevel);

        List<ServerPlayer> participants = new ArrayList<>(sourceLevel.getPlayers(player ->
                player.isAlive()
                        && !player.isSpectator()
                        && player.distanceToSqr(activator) <= (double) ENTRY_RADIUS * ENTRY_RADIUS));
        if (!participants.contains(activator)) {
            participants.add(activator);
        }

        for (int i = 0; i < participants.size(); i++) {
            ServerPlayer participant = participants.get(i);
            if (!participant.isAlive() || participant.isSpectator()) {
                continue;
            }
            saveReturnLocation(participant);
            markActive(participant);

            double angle = (Math.PI * 2.0 * i) / Math.max(1, participants.size());
            double radius = 8.0;
            double x = 0.5 + Math.cos(angle) * radius;
            double z = 0.5 + Math.sin(angle) * radius;
            double y = ARENA_Y + 2.0;

            arenaLevel.getChunkAt(BlockPos.containing(x, y, z));
            participant.teleportTo(arenaLevel, x, y, z, participant.getYRot(), participant.getXRot());
            participant.setDeltaMovement(Vec3.ZERO);
            participant.fallDistance = 0.0F;
            RULES.applyPeriodicEffects(participant);
            participant.sendSystemMessage(Component.translatable("msg.transcend.ritual_arena_opened")
                    .withStyle(ChatFormatting.GOLD));
        }

        spawnAvatarIfMissing(arenaLevel, activator);
        return true;
    }

    public static void tickArenaPlayer(ServerPlayer player) {
        if (!isArenaActive(player)) {
            return;
        }

        ServerLevel arenaLevel = player.server.getLevel(TranscendDimensions.ARENA_LEVEL);
        if (arenaLevel == null) {
            clearArenaState(player);
            return;
        }

        if (player.level().dimension() != TranscendDimensions.ARENA_LEVEL) {
            if (hasArenaBossAlive(arenaLevel)) {
                if (!player.isCreative() && !player.isSpectator()) {
                    teleportToArenaCenter(player, arenaLevel);
                    player.hurt(player.damageSources().magic(), 6.0F);
                    player.sendSystemMessage(Component.translatable("msg.transcend.arena_cowardice")
                            .withStyle(ChatFormatting.RED));
                } else {
                    clearArenaState(player);
                }
            } else {
                returnPlayerToOrigin(player, true);
            }
            return;
        }

        RULES.applyPeriodicEffects(player);

        double dx = player.getX();
        double dz = player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        CompoundTag tag = player.getPersistentData();
        int penaltyCd = Math.max(0, tag.getInt(TAG_PENALTY_CD) - 1);
        tag.putInt(TAG_PENALTY_CD, penaltyCd);
        int noticeCd = Math.max(0, tag.getInt(TAG_NOTICE_CD) - 1);
        tag.putInt(TAG_NOTICE_CD, noticeCd);

        boolean outOfRange = RULES.isOutOfBounds(player, SHAPE);
        if (outOfRange) {
            if (penaltyCd == 0) {
                RULES.onOutOfBounds(player, SHAPE);
                tag.putInt(TAG_PENALTY_CD, 20);
            }
            if (noticeCd == 0) {
                player.sendSystemMessage(Component.translatable("msg.transcend.arena_boundary_punish")
                        .withStyle(ChatFormatting.RED));
                tag.putInt(TAG_NOTICE_CD, 40);
            }
        }

        if (hasArenaBossAlive(arenaLevel)) {
            tag.putInt(TAG_CALM, 0);
            return;
        }

        int calm = tag.getInt(TAG_CALM) + 1;
        tag.putInt(TAG_CALM, calm);
        if (calm >= 120) {
            returnPlayerToOrigin(player, true);
        }
    }

    public static boolean shouldProtectBlock(LevelAccessor level, BlockPos pos) {
        if (level == null || pos == null || !(level instanceof Level realLevel)) {
            return false;
        }
        return realLevel.dimension() == TranscendDimensions.ARENA_LEVEL
                && RULES.shouldProtectBlock(level, pos, SHAPE);
    }

    public static boolean isArenaActive(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(TAG_ACTIVE);
    }

    private static void markActive(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean(TAG_ACTIVE, true);
        tag.putInt(TAG_CALM, 0);
        tag.putInt(TAG_PENALTY_CD, 0);
        tag.putInt(TAG_NOTICE_CD, 0);
    }

    private static void clearArenaState(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove(TAG_ACTIVE);
        tag.remove(TAG_RETURN_DIM);
        tag.remove(TAG_RETURN_X);
        tag.remove(TAG_RETURN_Y);
        tag.remove(TAG_RETURN_Z);
        tag.remove(TAG_RETURN_YAW);
        tag.remove(TAG_RETURN_PITCH);
        tag.remove(TAG_CALM);
        tag.remove(TAG_PENALTY_CD);
        tag.remove(TAG_NOTICE_CD);
    }

    private static void saveReturnLocation(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.putString(TAG_RETURN_DIM, player.level().dimension().location().toString());
        tag.putDouble(TAG_RETURN_X, player.getX());
        tag.putDouble(TAG_RETURN_Y, player.getY());
        tag.putDouble(TAG_RETURN_Z, player.getZ());
        tag.putFloat(TAG_RETURN_YAW, player.getYRot());
        tag.putFloat(TAG_RETURN_PITCH, player.getXRot());
    }

    private static void returnPlayerToOrigin(ServerPlayer player, boolean victory) {
        MinecraftServer server = player.server;
        CompoundTag tag = player.getPersistentData();
        ServerLevel targetLevel = server.overworld();

        String dimId = tag.getString(TAG_RETURN_DIM);
        ResourceLocation rl = ResourceLocation.tryParse(dimId);
        if (rl != null) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
            ServerLevel resolved = server.getLevel(key);
            if (resolved != null) {
                targetLevel = resolved;
            }
        }

        double x = tag.contains(TAG_RETURN_X) ? tag.getDouble(TAG_RETURN_X) : player.getX();
        double y = tag.contains(TAG_RETURN_Y) ? tag.getDouble(TAG_RETURN_Y) : targetLevel.getSharedSpawnPos().getY() + 1;
        double z = tag.contains(TAG_RETURN_Z) ? tag.getDouble(TAG_RETURN_Z) : player.getZ();
        float yaw = tag.contains(TAG_RETURN_YAW) ? tag.getFloat(TAG_RETURN_YAW) : player.getYRot();
        float pitch = tag.contains(TAG_RETURN_PITCH) ? tag.getFloat(TAG_RETURN_PITCH) : player.getXRot();

        player.teleportTo(targetLevel, x, y, z, yaw, pitch);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        clearArenaState(player);

        if (victory) {
            player.sendSystemMessage(Component.translatable("msg.transcend.arena_recall_victory")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    private static void teleportToArenaCenter(ServerPlayer player, ServerLevel arenaLevel) {
        player.teleportTo(arenaLevel, 0.5, ARENA_Y + 2.0, 0.5, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    private static boolean hasArenaBossAlive(ServerLevel arenaLevel) {
        AABB area = new AABB(-64, ARENA_Y - 20, -64, 64, ARENA_Y + 40, 64);
        List<AbstractTranscendBoss> bosses = arenaLevel.getEntitiesOfClass(AbstractTranscendBoss.class, area, Entity::isAlive);
        return !bosses.isEmpty();
    }

    private static void spawnAvatarIfMissing(ServerLevel arenaLevel, ServerPlayer summoner) {
        if (hasArenaBossAlive(arenaLevel)) {
            return;
        }
        TranscendenceAvatar avatar = ModEntities.TRANSCENDENCE_AVATAR.get().create(arenaLevel);
        if (avatar == null) {
            Transcend.LOGGER.warn("Failed to create arena avatar entity");
            return;
        }
        avatar.moveTo(0.5, ARENA_Y + 3.0, 0.5, summoner.getYRot(), 0.0F);
        arenaLevel.addFreshEntity(avatar);
    }

}
