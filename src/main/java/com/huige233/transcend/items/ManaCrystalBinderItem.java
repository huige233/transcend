package com.huige233.transcend.items;

import com.huige233.transcend.block.mana.ManaTransmitCrystalBlock;
import com.huige233.transcend.block.mana.ManaTransmitCrystalBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Round 42: 魔力水晶绑定器 — 灵感来自龙之研究 Crystal Binder。
 *
 * <p><b>R63 升级 — 多链折射</b>：原 1↔1 绑定升级为追加模式，单晶最多 4 伙伴。
 *
 * <p>用法：
 * <ol>
 *   <li>右键传输水晶 A → 选中起点（NBT 记录坐标）</li>
 *   <li>右键传输水晶 B → 双向追加 A↔B 到各自的伙伴列表（不替换已有）</li>
 *   <li>潜行 + 右键水晶 → 解绑该水晶的 <b>所有</b> 伙伴</li>
 *   <li>右键空气 → 取消选择（清除 NBT）</li>
 * </ol>
 *
 * <p>距离限制 {@link ManaTransmitCrystalBlockEntity#MAX_RANGE} 块；
 * 每个水晶最多 {@link ManaTransmitCrystalBlockEntity#MAX_PARTNERS} 个伙伴。
 */
public class ManaCrystalBinderItem extends Item {

    private static final String TAG_PENDING_X = "pending_x";
    private static final String TAG_PENDING_Y = "pending_y";
    private static final String TAG_PENDING_Z = "pending_z";

    public ManaCrystalBinderItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON).durability(0));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        BlockPos clickedPos = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();
        if (player == null) return InteractionResult.PASS;

        BlockState clickedState = level.getBlockState(clickedPos);
        if (!(clickedState.getBlock() instanceof ManaTransmitCrystalBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 潜行 + 右键 → 解绑所有伙伴
        if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(clickedPos) instanceof ManaTransmitCrystalBlockEntity be && be.isBound()) {
                int count = be.getPartnerCount();
                ManaTransmitCrystalBlockEntity.unbindAll(level, be);
                player.displayClientMessage(
                        Component.translatable("msg.transcend.crystal_binder.unbound_all", count)
                                .withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, clickedPos, SoundEvents.AMETHYST_BLOCK_BREAK,
                        SoundSource.BLOCKS, 0.6F, 1.5F);
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.crystal_binder.not_bound")
                                .withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
        }

        BlockPos pending = getPendingPos(stack);
        if (pending == null) {
            // 第一次选择 — 记录起点
            if (level.getBlockEntity(clickedPos) instanceof ManaTransmitCrystalBlockEntity startBe && startBe.isFull()) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.crystal_binder.full",
                                ManaTransmitCrystalBlockEntity.MAX_PARTNERS)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            setPendingPos(stack, clickedPos);
            int currentCount = level.getBlockEntity(clickedPos) instanceof ManaTransmitCrystalBlockEntity sbe
                    ? sbe.getPartnerCount() : 0;
            player.displayClientMessage(
                    Component.translatable("msg.transcend.crystal_binder.start_selected_n",
                            clickedPos.getX(), clickedPos.getY(), clickedPos.getZ(),
                            currentCount, ManaTransmitCrystalBlockEntity.MAX_PARTNERS)
                            .withStyle(ChatFormatting.AQUA), true);
            level.playSound(null, clickedPos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.6F, 1.0F);
            return InteractionResult.CONSUME;
        }

        // 第二次选择 — 绑定
        if (pending.equals(clickedPos)) {
            // 同一个水晶 — 清除选择
            clearPendingPos(stack);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.crystal_binder.cleared")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        long distSq = (long) pending.distSqr(clickedPos);
        if (distSq > (long) ManaTransmitCrystalBlockEntity.MAX_RANGE * ManaTransmitCrystalBlockEntity.MAX_RANGE) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.crystal_binder.out_of_range",
                            (int) Math.sqrt(distSq), ManaTransmitCrystalBlockEntity.MAX_RANGE)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        int code = ManaTransmitCrystalBlockEntity.bindMutual(level, pending, clickedPos);
        clearPendingPos(stack);
        if (code == 0) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.crystal_binder.bound",
                            (int) Math.sqrt(distSq))
                            .withStyle(ChatFormatting.GREEN), true);
            level.playSound(null, clickedPos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.2F, 1.4F);
            level.playSound(null, pending, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.2F, 1.4F);
            return InteractionResult.CONSUME;
        }
        // R63: 失败码 → 玩家可见原因
        String reasonKey = switch (code) {
            case -4 -> "msg.transcend.crystal_binder.already_bound";
            case -5 -> "msg.transcend.crystal_binder.bind_full";
            default -> "msg.transcend.crystal_binder.bind_failed";
        };
        player.displayClientMessage(
                Component.translatable(reasonKey).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 右键空气 — 清除 pending
        if (!level.isClientSide && getPendingPos(stack) != null) {
            clearPendingPos(stack);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.crystal_binder.cleared")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                 @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        BlockPos pending = getPendingPos(stack);
        if (pending != null) {
            tooltip.add(Component.translatable("tooltip.transcend.crystal_binder.pending",
                    pending.getX(), pending.getY(), pending.getZ())
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.transcend.crystal_binder.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.crystal_binder.unbind")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        // R63: 多链提示
        tooltip.add(Component.translatable("tooltip.transcend.crystal_binder.multilink",
                ManaTransmitCrystalBlockEntity.MAX_PARTNERS)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    // ── NBT helpers ──

    @Nullable
    public static BlockPos getPendingPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_PENDING_X)) return null;
        return new BlockPos(tag.getInt(TAG_PENDING_X), tag.getInt(TAG_PENDING_Y), tag.getInt(TAG_PENDING_Z));
    }

    public static void setPendingPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_PENDING_X, pos.getX());
        tag.putInt(TAG_PENDING_Y, pos.getY());
        tag.putInt(TAG_PENDING_Z, pos.getZ());
    }

    public static void clearPendingPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tag.remove(TAG_PENDING_X);
        tag.remove(TAG_PENDING_Y);
        tag.remove(TAG_PENDING_Z);
    }
}
