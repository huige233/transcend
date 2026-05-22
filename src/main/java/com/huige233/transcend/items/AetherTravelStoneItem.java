package com.huige233.transcend.items;

import com.huige233.transcend.Transcend;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Round 24: 维度旅人之石 — 在主世界 ↔ Aether Realm 之间穿梭。
 *
 * <p>右键使用：
 * <ul>
 *   <li>当前在主世界 / 下界 / 末地 → 传送到 transcend:aether_realm（同坐标 + Y=128）</li>
 *   <li>当前在 transcend:aether_realm → 传送回主世界（同坐标 + 找地表）</li>
 * </ul>
 *
 * <p>耐久 16 次（每次 use 减 1）。Stage 1+ 才能使用（防滥用）。
 */
public class AetherTravelStoneItem extends Item {

    public static final ResourceKey<Level> AETHER_REALM_KEY = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            Transcend.rl("aether_realm"));

    public AetherTravelStoneItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).durability(16).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.fail(stack);
        }

        // Stage 1+ 门槛
        com.huige233.transcend.ascension.PlayerAscensionData data =
                com.huige233.transcend.ascension.AscensionCapability.get(player);
        if (data.getStage() < 1 && !player.getAbilities().instabuild) {
            player.displayClientMessage(
                    Component.translatable("travel_stone.transcend.stage_gate")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        ResourceKey<Level> currentDim = level.dimension();
        ResourceKey<Level> targetDim = currentDim.equals(AETHER_REALM_KEY)
                ? Level.OVERWORLD
                : AETHER_REALM_KEY;

        ServerLevel targetLevel = sp.server.getLevel(targetDim);
        if (targetLevel == null) {
            player.displayClientMessage(
                    Component.translatable("travel_stone.transcend.dim_missing")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // 计算落点：保持 XZ + 在目标维度找安全 Y
        double tx = sp.getX();
        double tz = sp.getZ();
        double ty = targetDim.equals(AETHER_REALM_KEY) ? 200.0 : 128.0; // 高空安全
        // 目标维度找地表
        net.minecraft.core.BlockPos topPos = targetLevel.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                new net.minecraft.core.BlockPos((int) tx, 0, (int) tz));
        ty = topPos.getY() + 2.0;

        // 入场音 + 离场音
        ((ServerLevel) level).playSound(null, sp.blockPosition(),
                SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.8F);

        // 传送
        sp.teleportTo(targetLevel, tx, ty, tz, sp.getYRot(), sp.getXRot());

        targetLevel.playSound(null, sp.blockPosition(),
                SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.2F, 1.2F);

        // 反馈消息
        String key = targetDim.equals(AETHER_REALM_KEY)
                ? "travel_stone.transcend.entered_realm"
                : "travel_stone.transcend.returned_home";
        sp.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.AQUA), false);

        // 耐久 -1
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("travel_stone.transcend.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("travel_stone.transcend.gate")
                .withStyle(ChatFormatting.GRAY));
    }
}
