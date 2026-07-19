package com.huige233.transcend.items.circle;

import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class BoundAetherPearlItem extends Item {

    private static final String TAG_BOUND_POS = "BoundPos";
    private static final String TAG_BOUND_DIM = "BoundDim";

    public BoundAetherPearlItem() {
        super(new Properties().stacksTo(1));
        ModItems.ITEMS.add(this);
    }

    public static void bindPosition(ItemStack stack, BlockPos pos, ResourceKey<Level> dimension) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TAG_BOUND_POS, pos.asLong());
        tag.putString(TAG_BOUND_DIM, dimension.location().toString());
    }

    public static Optional<BlockPos> getBoundPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_BOUND_POS)) {
            return Optional.empty();
        }
        return Optional.of(BlockPos.of(tag.getLong(TAG_BOUND_POS)));
    }

    public static Optional<ResourceKey<Level>> getBoundDimension(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_BOUND_DIM)) {
            return Optional.empty();
        }
        ResourceLocation rl = ResourceLocation.tryParse(tag.getString(TAG_BOUND_DIM));
        if (rl == null) {
            return Optional.empty();
        }
        return Optional.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, rl));
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_BOUND_POS) && tag.contains(TAG_BOUND_DIM);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            Level level = ctx.getLevel();
            BlockPos clicked = ctx.getClickedPos();
            if (!level.isClientSide) {
                ItemStack stack = ctx.getItemInHand();
                bindPosition(stack, clicked, level.dimension());
                player.displayClientMessage(
                        Component.translatable("msg.transcend.aether_pearl.bound",
                                clicked.getX(), clicked.getY(), clicked.getZ())
                                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                level.playSound(null, clicked, SoundEvents.ENDER_EYE_DEATH,
                        SoundSource.PLAYERS, 0.6F, 1.4F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isBound(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (isBound(stack)) {

            BlockPos pos = getBoundPos(stack).orElse(BlockPos.ZERO);
            String dim = getBoundDimension(stack)
                    .map(k -> k.location().toString())
                    .orElse("?");
            tooltip.add(Component.translatable("tooltip.transcend.aether_pearl.bound",
                            pos.getX(), pos.getY(), pos.getZ(), dim)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {

            tooltip.add(Component.translatable("tooltip.transcend.aether_pearl.unbound")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
