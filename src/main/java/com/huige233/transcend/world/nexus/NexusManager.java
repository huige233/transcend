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

/** 次元管理器。 */
public final class NexusManager {

    private NexusManager() {}

    public static boolean enterNexusDimension(ServerPlayer player) {
        if (player == null) return false;
        MinecraftServer server = player.server;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) {
            player.sendSystemMessage(Component.translatable("msg.transcend.nexus_unavailable")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ensureAllStructures(nexusLevel);

        BlockPos spawn = NexusType.SILENCE.getPlatformCenter().above(2);
        nexusLevel.getChunkAt(spawn);
        player.teleportTo(nexusLevel, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));

        player.sendSystemMessage(Component.translatable("msg.transcend.nexus_entered")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

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

    public static void ensureAllStructures(ServerLevel level) {
        NexusSavedData data = NexusSavedData.get(level);

        java.util.List<NexusType> toBuild = new java.util.ArrayList<>();
        for (NexusType type : NexusType.values()) {
            if (!data.isPlaced(type)) {
                data.markPlaced(type);
                toBuild.add(type);
            }
        }
        if (toBuild.isEmpty()) return;

        for (NexusType type : toBuild) {
            level.getChunkAt(type.getPlatformCenter());
        }

        for (NexusType type : toBuild) {
            buildNexusStructure(level, type);
        }
    }

    private static void buildNexusStructure(ServerLevel level, NexusType type) {
        BlockPos center = type.getPlatformCenter();
        NexusSavedData data = NexusSavedData.get(level);
        boolean isCenter = (type == NexusType.SILENCE);

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

        int[][] pillarOffsets = {{3, 3}, {3, -3}, {-3, 3}, {-3, -3}};
        for (int[] off : pillarOffsets) {
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(center.offset(off[0], dy, off[1]),
                        Blocks.END_STONE_BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(center.offset(off[0], 4, off[1]),
                    Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(center.offset(dx, 1, dz),
                        ModBlocks.MAGIC_CRYSTAL_BLOCK.get().defaultBlockState(), 3);
            }
        }

        level.setBlock(center.offset(0, 2, 0), Blocks.BEACON.defaultBlockState(), 3);

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

    private static void spawnNexusGuardians(ServerLevel level, NexusType type) {
        BlockPos center = type.getPlatformCenter();
        double y = center.getY() + 2.0;
        double[][] guardianPositions = {{3.5, 0.5}, {-3.5, 0.5}, {0.5, 3.5}, {0.5, -3.5}};

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

    public static void onNexusDestroyed(ServerLevel level, BlockPos pos,
                                         ServerPlayer player, NexusType type) {
        MinecraftServer server = level.getServer();
        NexusSavedData data = NexusSavedData.get(
                server.getLevel(TranscendDimensions.NEXUS_LEVEL));
        if (data == null) return;

        data.markDestroyed(type);

        com.huige233.transcend.TranscendGameRules.setNexusRule(server, type, true);

        net.minecraft.world.entity.LightningBolt lightning =
                EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(Vec3.atBottomCenterOf(pos));
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }

        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS,
                2.0F, 0.5F);

        Component message = Component.literal("")
                .append(player.getDisplayName())
                .append(Component.translatable(type.brokenKey).withStyle(type.color));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(message);
        }

        if (data.allDestroyed()) {
            Component allBroken = Component.translatable("msg.transcend.all_nexus_destroyed")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(allBroken);
            }
        }

        syncToAllPlayers(server);
    }

    public static void syncToAllPlayers(MinecraftServer server) {
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return;
        NexusSavedData data = NexusSavedData.get(nexusLevel);
        S2CNexusRuleSync packet = new S2CNexusRuleSync(data.getDestroyedIds());
        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void syncToPlayer(ServerPlayer player) {
        ServerLevel nexusLevel = player.server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return;
        NexusSavedData data = NexusSavedData.get(nexusLevel);
        S2CNexusRuleSync packet = new S2CNexusRuleSync(data.getDestroyedIds());
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static boolean isNexusDestroyed(MinecraftServer server, NexusType type) {
        if (server == null) return false;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return false;
        return NexusSavedData.get(nexusLevel).isDestroyed(type);
    }

    public static float getSpellCDRMultiplier(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.BINDING) ? 0.7F : 1.0F;
    }

    public static float getManaCostMultiplier(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.SCARCITY) ? 0.5F : 1.0F;
    }

    public static boolean isBossDamageCapRemoved(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.ENTROPY);
    }

    public static float getReactionDamageMultiplier(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.FRAILTY) ? 1.5F : 1.0F;
    }

    public static float getSpellPowerBonus(MinecraftServer server) {
        return isNexusDestroyed(server, NexusType.SILENCE) ? 0.25F : 0.0F;
    }

    public static int getDestroyedCount(MinecraftServer server) {
        if (server == null) return 0;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return 0;
        return NexusSavedData.get(nexusLevel).getDestroyedCount();
    }

    public static boolean areAllNexusesDestroyed(MinecraftServer server) {
        if (server == null) return false;
        ServerLevel nexusLevel = server.getLevel(TranscendDimensions.NEXUS_LEVEL);
        if (nexusLevel == null) return false;
        return NexusSavedData.get(nexusLevel).allDestroyed();
    }
}
