package com.huige233.transcend.items;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class BossSummonItem extends Item {

    private final Supplier<EntityType<? extends AbstractTranscendBoss>> bossType;
    private final String descKey;
    private final int requiredStage;
    private final int requiredTier;
    private final int searchRadius;

    public BossSummonItem(Supplier<EntityType<? extends AbstractTranscendBoss>> bossType, String descKey) {
        this(bossType, descKey, 0, 0, 0);
    }

    public BossSummonItem(Supplier<EntityType<? extends AbstractTranscendBoss>> bossType, String descKey,
                          int requiredStage, int requiredTier, int searchRadius) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.bossType = bossType;
        this.descKey = descKey;
        this.requiredStage = requiredStage;
        this.requiredTier = requiredTier;
        this.searchRadius = searchRadius;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (requiredStage > 0 && !player.getAbilities().instabuild) {
            PlayerAscensionData data = AscensionCapability.get(player);
            if (data.getRitualTier() < requiredStage) {
                player.displayClientMessage(
                        Component.translatable("boss_summon.transcend.stage_gate", requiredStage)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        if (requiredTier > 0 && !player.getAbilities().instabuild) {
            if (!hasNearbyCircleAtLeast(player, level, requiredTier, searchRadius)) {
                player.displayClientMessage(
                        Component.translatable("boss_summon.transcend.circle_gate",
                                tierName(requiredTier), searchRadius)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        AbstractTranscendBoss boss = bossType.get().create(sl);
        if (boss != null) {
            boss.setPos(player.getX(), player.getY(), player.getZ() + 3);
            sl.addFreshEntity(boss);
            sl.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.8F);
            sl.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5F, 0.6F);

            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static boolean hasNearbyCircleAtLeast(Player player, Level level, int minTier, int searchRadius) {
        BlockPos playerPos = player.blockPosition();
        int pcx = playerPos.getX() >> 4;
        int pcz = playerPos.getZ() >> 4;
        int chunkRadius = (searchRadius / 16) + 1;
        long maxDistSq = (long) searchRadius * searchRadius;

        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (!(e.getValue() instanceof MagicCircleCoreBlockEntity core)) continue;
                    if (!core.isActive() || !core.isStructureValid()) continue;
                    CircleTier tier = core.getDetectedTier();
                    if (tier == null || tier.getLevel() < minTier) continue;
                    if (e.getKey().distSqr(playerPos) <= maxDistSq) return true;
                }
            }
        }
        return false;
    }

    private static String tierName(int tierLevel) {
        return switch (tierLevel) {
            case 1 -> "INITIATE";
            case 2 -> "ADEPT";
            case 3 -> "MASTER";
            case 4 -> "ARCHON";
            case 5 -> "PRIMORDIAL";
            default -> "T" + tierLevel;
        };
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.transcend.boss_summon.warning").withStyle(ChatFormatting.RED));
        if (requiredStage > 0 || requiredTier > 0) {
            tooltip.add(Component.empty());
            if (requiredStage > 0) {
                tooltip.add(Component.translatable("boss_summon.transcend.gate.stage", requiredStage)
                        .withStyle(ChatFormatting.RED));
            }
            if (requiredTier > 0) {
                tooltip.add(Component.translatable("boss_summon.transcend.gate.circle",
                                tierName(requiredTier), searchRadius)
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }
}

