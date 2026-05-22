package com.huige233.transcend.items;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Round 45: 祭祀之刃 — 灵感来自血魔法（Blood Magic）的 Sacrificial Knife。
 *
 * <p>给法环一些"血魔法"风味：
 * <ul>
 *   <li>右键空气：自伤 §c4 HP §r→ 玩家身上"涌出"血液 → 寻找 8 格内最近的法环核心 → +200 mana</li>
 *   <li>右键法环核心方块：直接对该核心献血 → -4 HP / +200 mana</li>
 *   <li>60 tick (3s) 冷却</li>
 *   <li>耐久 50 — 用完即坏</li>
 * </ul>
 *
 * <p>"Blood Magic 风" 不是完整搬运 — 只是给 Transcend 现有 mana 注入路径加一条
 * "用 HP 换 mana" 的快速通道，符合用户"法环制作的更像血魔法一些"的诉求。
 */
public class SacrificialKnifeItem extends Item {

    private static final int HP_COST = 4;             // 2 颗心
    private static final int MANA_GAIN = 200;
    private static final int SEARCH_RADIUS = 8;
    private static final int COOLDOWN_TICKS = 60;

    public SacrificialKnifeItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON).durability(50));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        BlockPos clickedPos = ctx.getClickedPos();
        if (player == null || level.isClientSide) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(clickedPos);
        if (!(be instanceof MagicCircleCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        return doSacrifice(level, player, core, ctx.getItemInHand()) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // 找 8 格球内最近的法环核心
        MagicCircleCoreBlockEntity nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos pPos = player.blockPosition();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    if (dx*dx + dy*dy + dz*dz > SEARCH_RADIUS * SEARCH_RADIUS) continue;
                    cursor.set(pPos.getX() + dx, pPos.getY() + dy, pPos.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof MagicCircleCoreBlockEntity core) {
                        double d = cursor.distSqr(pPos);
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            nearest = core;
                        }
                    }
                }
            }
        }
        if (nearest == null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sacrifice.no_core")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        return doSacrifice(level, player, nearest, stack) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    /** 实际献祭逻辑：扣血 → 注入 mana → 视觉/音效 → 耐久 → 冷却 */
    private boolean doSacrifice(Level level, Player player, MagicCircleCoreBlockEntity core, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) return false;

        if (player.getHealth() <= HP_COST + 1) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sacrifice.too_weak")
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!core.isStructureValid()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sacrifice.invalid_structure")
                            .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        int inserted = core.insertMana(MANA_GAIN);
        if (inserted <= 0) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sacrifice.full")
                            .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        // 扣血 — 用魔法伤害绕过普通 invuln 帧
        player.hurt(level.damageSources().magic(), HP_COST);

        // 视觉：红粒子从玩家洒到 core
        if (level instanceof ServerLevel sl) {
            BlockPos cp = core.getBlockPos();
            for (int i = 0; i < 20; i++) {
                double t = i / 20.0;
                double x = player.getX() + (cp.getX() + 0.5 - player.getX()) * t;
                double y = player.getY() + 1.0 + (cp.getY() + 0.5 - player.getY() - 1.0) * t;
                double z = player.getZ() + (cp.getZ() + 0.5 - player.getZ()) * t;
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.10F), 1.4F),
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.7F, 1.4F);
        level.playSound(null, core.getBlockPos(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);

        player.displayClientMessage(
                Component.translatable("msg.transcend.sacrifice.success", inserted)
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);

        // 耐久 + 冷却
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                 @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.sacrificial_knife.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.sacrificial_knife.cost",
                        HP_COST / 2.0F, MANA_GAIN)
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.transcend.sacrificial_knife.usage")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
