package com.huige233.transcend.world.nexus;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.entity.nexus.NexusGuardian;
import com.huige233.transcend.entity.nexus.NexusSentinel;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.network.S2CNexusRuleSync;
import com.huige233.transcend.world.TranscendDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * 法则之境管理器 — 负责维度进入、结构生成、枢纽摧毁、规则查询。
 * 遵循 TranscendArenaManager 的静态管理器模式。
 */
public final class NexusManager {

    private NexusManager() {}

    // ─── Entry ───────────────────────────────────────────────────────

    /**
     * 通过仪式将玩家传送至法则之境的中心（隔绝枢纽位置）。
     */
    public static boolean enterNexusDimension(ServerPlayer player) {
        if (player == null) return false;
        MinecraftServer server = player.server;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) {
            player.sendSystemMessage(Component.translatable("msg.transcend.nexus_unavailable")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Ensure center structure is built
        ensureAllStructures(nexusLevel);

        BlockPos spawn = NexusType.ISOLATION.getPlatformCenter().above(2);
        nexusLevel.getChunkAt(spawn);
        player.teleportTo(nexusLevel, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        // Apply slow falling and night vision
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));

        player.sendSystemMessage(Component.translatable("msg.transcend.nexus_entered")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // ─── Structure Generation ────────────────────────────────────────

    /**
     * 在区块加载时检查是否需要生成枢纽结构。
     */
    public static void onChunkLoad(ServerLevel level, ChunkPos chunk) {
        if (level.dimension() != TranscendDimensions.NEXUS_LEVEL) return;

        NexusSavedData data = NexusSavedData.get(level);
        for (NexusType type : NexusType.values()) {
            BlockPos center = type.getPlatformCenter();
            int cx = center.getX() >> 4;
            int cz = center.getZ() >> 4;
            if (chunk.x == cx && chunk.z == cz && !data.isPlaced(type)) {
                data.markPlaced(type);
                buildNexusStructure(level, type);
            }
        }
    }

    /**
     * 确保所有结构都已生成。
     * 先一次性标记所有未放置的类型，再批量构建，避免 getChunkAt 触发 onChunkLoad 时重入。
     */
    public static void ensureAllStructures(ServerLevel level) {
        NexusSavedData data = NexusSavedData.get(level);

        // 第一步：收集需要构建的类型并全部标记
        java.util.List<NexusType> toBuild = new java.util.ArrayList<>();
        for (NexusType type : NexusType.values()) {
            if (!data.isPlaced(type)) {
                data.markPlaced(type);
                toBuild.add(type);
            }
        }
        if (toBuild.isEmpty()) return;

        // 第二步：预加载所有涉及的区块
        for (NexusType type : toBuild) {
            level.getChunkAt(type.getPlatformCenter());
        }

        // 第三步：构建所有平台（含信标）
        for (NexusType type : toBuild) {
            buildNexusStructure(level, type);
        }
    }

    /**
     * 在指定位置生成一座枢纽结构。
     * ISOLATION（中心）使用远古水晶平台。
     * 其他4个方向枢纽使用哭泣黑曜石平台 + 信标。
     */
    private static void buildNexusStructure(ServerLevel level, NexusType type) {
        BlockPos center = type.getPlatformCenter();
        NexusSavedData data = NexusSavedData.get(level);
        boolean isCenter = (type == NexusType.ISOLATION);

        // Layer 0: 9×9 平台
        BlockState platformBlock = isCenter
                ? ModBlocks.ANCIENT_CRYSTAL.get().defaultBlockState()
                : Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState edgeBlock = isCenter
                ? ModBlocks.ANCIENT_CRYSTAL.get().defaultBlockState()
                : Blocks.REINFORCED_DEEPSLATE.defaultBlockState();

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                level.setBlock(center.offset(dx, 0, dz), edge ? edgeBlock : platformBlock, 3);
            }
        }

        // 4 pillars at (±3, 1~3, ±3) — end stone bricks + soul lantern
        int[][] pillarOffsets = {{3, 3}, {3, -3}, {-3, 3}, {-3, -3}};
        for (int[] off : pillarOffsets) {
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(center.offset(off[0], dy, off[1]),
                        Blocks.END_STONE_BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(center.offset(off[0], 4, off[1]),
                    Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        }

        // Center 3×3 信标基座（魔力水晶块） + 信标
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(center.offset(dx, 1, dz),
                        ModBlocks.MAGIC_CRYSTAL_BLOCK.get().defaultBlockState(), 3);
            }
        }
        // 信标在基座中心上方
        level.setBlock(center.offset(0, 2, 0), Blocks.BEACON.defaultBlockState(), 3);

        // NexusCoreBlock 直接在信标上方 (Y+3) — only if not already destroyed
        BlockPos corePos = center.offset(0, 5, 0);
        if (!data.isDestroyed(type)) {
            level.setBlock(corePos, ModBlocks.NEXUS_CORE.get().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(corePos);
            if (be instanceof com.huige233.transcend.block.NexusCoreBlockEntity coreBE) {
                coreBE.setNexusType(type.id);
            }
            spawnNexusGuardians(level, type);
        }
    }

    /**
     * 生成枢纽守卫。每个枢纽生成2个守卫(地面) + 2个哨兵(飞行)。
     */
    private static void spawnNexusGuardians(ServerLevel level, NexusType type) {
        BlockPos center = type.getPlatformCenter();
        double y = center.getY() + 2.0;
        double[][] guardianPositions = {{3.5, 0.5}, {-3.5, 0.5}, {0.5, 3.5}, {0.5, -3.5}};

        // 2 ground guardians
        for (int i = 0; i < 2; i++) {
            NexusGuardian guardian = ModEntities.NEXUS_GUARDIAN.get().create(level);
            if (guardian != null) {
                double[] pos = guardianPositions[i];
                guardian.moveTo(center.getX() + pos[0], y, center.getZ() + pos[1], 0, 0);
                guardian.setNexusId(type.id);
                guardian.setPersistenceRequired();
                level.addFreshEntity(guardian);
            }
        }

        // 2 flying sentinels
        for (int i = 2; i < 4; i++) {
            NexusSentinel sentinel = ModEntities.NEXUS_SENTINEL.get().create(level);
            if (sentinel != null) {
                double[] pos = guardianPositions[i];
                sentinel.moveTo(center.getX() + pos[0], y + 3.0, center.getZ() + pos[1], 0, 0);
                sentinel.setNexusId(type.id);
                sentinel.setPersistenceRequired();
                level.addFreshEntity(sentinel);
            }
        }
    }

    // ─── Nexus Destruction ───────────────────────────────────────────

    /**
     * 当枢纽核心被摧毁时调用。
     */
    public static void onNexusDestroyed(ServerLevel level, BlockPos pos,
                                         ServerPlayer player, NexusType type) {
        MinecraftServer server = level.getServer();
        NexusSavedData data = NexusSavedData.get(
                server.getLevel(TranscendDimensions.NEXUS_LEVEL));
        if (data == null) return;

        data.markDestroyed(type);

        // Set the corresponding game rule
        com.huige233.transcend.TranscendGameRules.setNexusRule(server, type, true);

        // Lightning strike at destruction point
        net.minecraft.world.entity.LightningBolt lightning =
                EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(Vec3.atBottomCenterOf(pos));
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }

        // Sound
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS,
                2.0F, 0.5F);

        // Global broadcast
        Component message = Component.literal("")
                .append(player.getDisplayName())
                .append(Component.translatable(type.brokenKey).withStyle(type.color));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(message);
        }

        // Check if all nexuses destroyed
        if (data.allDestroyed()) {
            Component allBroken = Component.translatable("msg.transcend.all_nexus_destroyed")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(allBroken);
            }
        }

        // Sync to all clients
        syncToAllPlayers(server);
    }

    /**
     * 将枢纽状态同步到所有在线客户端。
     */
    public static void syncToAllPlayers(MinecraftServer server) {
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return;
        NexusSavedData data = NexusSavedData.get(nexusLevel);
        S2CNexusRuleSync packet = new S2CNexusRuleSync(data.getDestroyedIds());
        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    /**
     * 在玩家登录时同步状态。
     */
    public static void syncToPlayer(ServerPlayer player) {
        ServerLevel nexusLevel = player.server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return;
        NexusSavedData data = NexusSavedData.get(nexusLevel);
        S2CNexusRuleSync packet = new S2CNexusRuleSync(data.getDestroyedIds());
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // ─── Rule Queries ────────────────────────────────────────────────

    /**
     * 查询某个枢纽是否已被摧毁。
     */
    public static boolean isNexusDestroyed(MinecraftServer server, NexusType type) {
        if (server == null) return false;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return false;
        return NexusSavedData.get(nexusLevel).isDestroyed(type);
    }

    /** 束缚枢纽已摧毁 → 法术CDR +30% (乘数 0.7) */
    public static float getSpellCDRMultiplier(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.BINDING) ? 0.7F : 1.0F;
    }

    /** 匮乏枢纽已摧毁 → 魔力消耗 -50% (乘数 0.5) */
    public static float getManaCostMultiplier(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.SCARCITY) ? 0.5F : 1.0F;
    }

    /** 怜悯枢纽已摧毁 → Boss限伤上限移除 */
    public static boolean isBossDamageCapRemoved(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.MERCY);
    }

    /** 脆弱枢纽已摧毁 → 元素反应伤害 +50% (乘数 1.5) */
    public static float getReactionDamageMultiplier(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.FRAILTY) ? 1.5F : 1.0F;
    }

    /** 隔绝枢纽已摧毁 → 全局法术威力 +25% (额外加成 0.25) */
    public static float getSpellPowerBonus(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.ISOLATION) ? 0.25F : 0.0F;
    }

    // ─── Boss Modifier Queries ───────────────────────────────────────

    /**
     * 获取已摧毁枢纽数量（用于削弱超越化身）。
     * 每个枢纽削弱一部分Boss能力。
     */
    public static int getDestroyedCount(MinecraftServer server) {
        if (server == null) return 0;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return 0;
        return NexusSavedData.get(nexusLevel).getDestroyedCount();
    }

    /**
     * 是否所有枢纽都已被摧毁 → 超越化身直接三阶段 + 四阶段额外生命。
     */
    public static boolean areAllNexusesDestroyed(MinecraftServer server) {
        if (server == null) return false;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return false;
        return NexusSavedData.get(nexusLevel).allDestroyed();
    }
}
