package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.block.NexusCoreBlock;
import com.huige233.transcend.block.NexusCoreBlockEntity;
import com.huige233.transcend.world.TranscendDimensions;
import com.huige233.transcend.world.nexus.NexusManager;
import com.huige233.transcend.world.nexus.NexusType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 法则之境事件处理器。
 * - 区块加载时生成枢纽结构
 * - 方块破坏时检验飞升阶段 + 触发摧毁效果
 * - 玩家Tick时处理虚空伤害和增益
 * - 玩家登录时同步枢纽状态
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NexusDimensionEvents {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.dimension() == TranscendDimensions.NEXUS_LEVEL) {
            ChunkPos chunk = event.getChunk().getPos();
            NexusManager.onChunkLoad(serverLevel, chunk);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != TranscendDimensions.NEXUS_LEVEL) return;
        if (!(event.getState().getBlock() instanceof NexusCoreBlock)) return;

        Player player = event.getPlayer();
        BlockPos pos = event.getPos();

        // Check lock state
        BlockEntity lockBe = serverLevel.getBlockEntity(pos);
        if (lockBe instanceof NexusCoreBlockEntity coreBE && coreBE.isLocked()) {
            event.setCanceled(true);
            int seconds = coreBE.getLockTicks() / 20;
            player.sendSystemMessage(Component.translatable("msg.transcend.nexus_locked", seconds)
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Check ascension stage
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getStage() < 3) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("msg.transcend.nexus_too_weak")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Determine which nexus this is
        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (be instanceof NexusCoreBlockEntity coreBE) {
            NexusType type = NexusType.getById(coreBE.getNexusTypeId());
            if (type != null) {
                // 生成法则水晶实体代替直接触发效果
                serverLevel.getServer().execute(() -> {
                    com.huige233.transcend.entity.nexus.NexusCrystalEntity crystal =
                            com.huige233.transcend.init.ModEntities.NEXUS_CRYSTAL.get().create(serverLevel);
                    if (crystal != null) {
                        crystal.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
                        crystal.setNexusData(type.id, pos);
                        serverLevel.addFreshEntity(crystal);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().dimension() != TranscendDimensions.NEXUS_LEVEL) return;

        // Void damage below Y=50
        if (player.getY() < 50.0 && !player.isCreative() && !player.isSpectator()) {
            player.hurt(player.damageSources().fellOutOfWorld(), 4.0F);
            // Teleport back to center if falling into void
            if (player.getY() < 10.0) {
                BlockPos spawn = NexusType.ISOLATION.getPlatformCenter().above(2);
                player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                player.setDeltaMovement(0, 0, 0);
                player.fallDistance = 0;
                player.sendSystemMessage(Component.translatable("msg.transcend.nexus_void_rescue")
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        // Periodic night vision refresh while in the dimension
        if (player.tickCount % 200 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NexusManager.syncToPlayer(player);
        }
    }
}
