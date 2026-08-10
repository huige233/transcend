package com.huige233.transcend.block.circle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.items.circle.FunctionSigilItem;

/** 法阵核心方块。 */
public class MagicCircleCoreBlock extends Block implements EntityBlock {

    private final String coreType;

    public MagicCircleCoreBlock(String coreType) {
        super(Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 12.0F)
            .sound(SoundType.AMETHYST)

            .lightLevel(state -> 5)

            .noOcclusion());
        this.coreType = coreType;
    }

    public String getCoreType() {
        return coreType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicCircleCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof MagicCircleCoreBlockEntity coreBe) {
                MagicCircleCoreBlockEntity.serverTick(lvl, pos, st, coreBe);
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe) {
            coreBe.setOwner(serverPlayer.getUUID());
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        boolean configuresCore = held.isEmpty()
                || held.getItem() instanceof MagicCrystalItem
                || held.getItem() instanceof FunctionSigilItem;
        if (configuresCore && (!(player instanceof ServerPlayer serverPlayer)
                || !coreBe.claimOrAuthorize(serverPlayer))) {
            return InteractionResult.FAIL;
        }

        if (held.getItem() instanceof MagicCrystalItem crystal) {
            int value = crystal.isRefined() ? 3 : 1;
            int inserted = coreBe.insertMana(value);
            if (inserted > 0) {
                if (!player.isCreative()) held.shrink(1);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
                player.displayClientMessage(
                        Component.literal("§b+" + inserted + " CM §7(" + coreBe.getStoredMana() + "/" + coreBe.getMaxMana() + ")")
                                .withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(
                    Component.translatable("msg.transcend.circle.mana_full").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (held.getItem() instanceof FunctionSigilItem sigil) {
            if (coreBe.isSigilLocked() && coreBe.getActiveFunction() != null) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.circle.sigil_locked")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            coreBe.setFunction(sigil.getFunctionType(), held.copy());
            if (!player.isCreative()) held.shrink(1);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

            boolean activated = coreBe.activate();
            if (activated) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.circle.sigil_activated",
                                Component.translatable(sigil.getFunctionType().getTranslationKey()))
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.circle.sigil_set",
                                Component.translatable(sigil.getFunctionType().getTranslationKey()))
                                .withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {

                String tierStr = coreBe.getDetectedTier() != null ? "T" + coreBe.getDetectedTier().getLevel() : "??";
                String stateStr = coreBe.getCircleState().name();
                player.displayClientMessage(
                        Component.literal("§7[" + tierStr + "] §d" + stateStr +
                                " §7| §b" + coreBe.getStoredMana() + "/" + coreBe.getMaxMana() + " CM"), true);
            } else {

                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    net.minecraftforge.network.NetworkHooks.openScreen(sp, coreBe, buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeVarInt(coreBe.getDetectedTier() != null ? coreBe.getDetectedTier().getLevel() : 0);
                        buf.writeVarInt(coreBe.getStoredMana());
                        buf.writeVarInt(coreBe.getMaxMana());
                        buf.writeBoolean(coreBe.isActive());
                        buf.writeBoolean(coreBe.isStructureValid());
                        buf.writeUtf(coreBe.getActiveFunction() != null ? coreBe.getActiveFunction().getId() : "");
                        buf.writeFloat(coreBe.getActiveFunction() != null ? coreBe.getActiveFunction().getBaseUpkeepPerMinute() : 0f);

                        int settingsCount = coreBe.getActiveFunction() != null
                                ? com.huige233.transcend.circle.CircleFunctionSettings.getSettingsFor(coreBe.getActiveFunction()).size()
                                : 0;
                        buf.writeVarInt(settingsCount);

                        buf.writeBoolean(coreBe.isSigilLocked());
                        buf.writeVarInt(coreBe.getMissingBlockCount());
                        buf.writeVarInt(coreBe.getCatalystCount());
                        buf.writeVarInt(coreBe.getCatalystSatisfiedCount());
                    });
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
