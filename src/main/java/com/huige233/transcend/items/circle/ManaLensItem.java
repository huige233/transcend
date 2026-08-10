package com.huige233.transcend.items.circle;

import com.huige233.transcend.block.circle.MagicCircleCoreBlock;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.mana.ChunkManaObservation;
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

/** 魔力透镜物品。 */
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
            player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.circle_header"), false);
            Component tier = coreBe.getDetectedTier() != null
                    ? Component.literal("T" + coreBe.getDetectedTier().getLevel())
                    : Component.translatable("gui.transcend.circle_core.state.invalid");
            player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.tier", tier), false);
            player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.mana",
                    coreBe.getStoredMana(), coreBe.getMaxMana()), false);
            player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.state",
                    Component.translatable("gui.transcend.circle_core.state."
                            + coreBe.getCircleState().name().toLowerCase()).withStyle(getStateColor(coreBe.getCircleState()))), false);
            if (coreBe.getActiveFunction() != null) {
                player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.function",
                        Component.translatable(coreBe.getActiveFunction().getTranslationKey())), false);
                player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.upkeep",
                        coreBe.getActiveFunction().getBaseUpkeepPerMinute()), false);
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
        ChunkManaObservation.Sample observation = ChunkManaObservation.observe(serverLevel, chunkPos);

        player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.chunk_header"), false);
        if (!observation.known()) {
            player.displayClientMessage(Component.literal("UNKNOWN").withStyle(ChatFormatting.GRAY), false);
            return InteractionResultHolder.consume(stack);
        }
        float mana = observation.mana().orElseThrow();
        float percent = mana / ChunkManaSavedData.DEFAULT_MANA;
        String tierKey = "tier.transcend.chunk_mana." + observation.tier().name().toLowerCase();
        player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.concentration",
                mana, ChunkManaSavedData.MAX_MANA), false);
        player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.chunk_tier",
                Component.translatable(tierKey)), false);
        player.displayClientMessage(Component.translatable("msg.transcend.mana_lens.saturation", percent * 100), false);

        return InteractionResultHolder.consume(stack);
    }

    private ChatFormatting getStateColor(MagicCircleCoreBlockEntity.CircleState state) {
        return switch (state) {
            case ACTIVE -> ChatFormatting.GREEN;
            case FLICKERING -> ChatFormatting.YELLOW;
            case DORMANT -> ChatFormatting.GRAY;
            case DISABLED -> ChatFormatting.DARK_GRAY;
            default -> ChatFormatting.RED;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.mana_lens.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
