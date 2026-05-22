package com.huige233.transcend.items.circle;

import com.huige233.transcend.block.circle.MagicCircleCoreBlock;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 魔力透镜 — 法阵与区块魔力的诊断工具。
 * 右键法阵核心：显示法阵状态。
 * 空挥：显示当前区块魔力。
 */
public class ManaLensItem extends Item {

    public ManaLensItem() {
        super(new Properties().stacksTo(1));
        ModItems.ITEMS.add(this);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof MagicCircleCoreBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe) {
            Player player = ctx.getPlayer();
            player.displayClientMessage(Component.literal("§7═══ §d法环状态 §7═══"), false);
            player.displayClientMessage(Component.literal("§7阶级: §b" +
                    (coreBe.getDetectedTier() != null ? "T" + coreBe.getDetectedTier().getLevel() : "无效")), false);
            player.displayClientMessage(Component.literal("§7魔力: §b" +
                    coreBe.getStoredMana() + " §7/ §b" + coreBe.getMaxMana()), false);
            player.displayClientMessage(Component.literal("§7状态: " +
                    getStateColor(coreBe.getCircleState()) + coreBe.getCircleState().name()), false);
            if (coreBe.getActiveFunction() != null) {
                player.displayClientMessage(Component.literal("§7功能: §e" +
                        coreBe.getActiveFunction().getId()), false);
                player.displayClientMessage(Component.literal("§7消耗: §a" +
                        String.format("%.1f", coreBe.getActiveFunction().getBaseUpkeepPerMinute()) + " CM/min"), false);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerLevel serverLevel = (ServerLevel) level;
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        ChunkManaSavedData data = ChunkManaSavedData.get(serverLevel);
        float mana = data.getMana(chunkPos);
        float percent = data.getManaPercent(chunkPos);

        String band;
        if (mana < 1000) band = "§c枯竭";
        else if (mana < 2500) band = "§6弱脉";
        else if (mana < 7500) band = "§a稳定";
        else band = "§b富脉";

        player.displayClientMessage(Component.literal("§7═══ §b区块魔力 §7═══"), false);
        player.displayClientMessage(Component.literal("§7浓度: §b" +
                String.format("%.0f", mana) + " §7/ §b" + String.format("%.0f", ChunkManaSavedData.MAX_MANA)), false);
        player.displayClientMessage(Component.literal("§7状态: " + band), false);
        player.displayClientMessage(Component.literal("§7饱和度: §e" +
                String.format("%.1f%%", percent * 100)), false);

        return InteractionResultHolder.consume(stack);
    }

    private String getStateColor(MagicCircleCoreBlockEntity.CircleState state) {
        return switch (state) {
            case ACTIVE -> "§a";
            case FLICKERING -> "§e";
            case DORMANT -> "§7";
            case DISABLED -> "§8";
            default -> "§c";
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.mana_lens.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
