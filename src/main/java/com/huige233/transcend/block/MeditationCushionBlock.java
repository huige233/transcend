package com.huige233.transcend.block;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.network.S2COpenProgramScreen;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 打坐垫方块：打坐/冥想增益。 */
public class MeditationCushionBlock extends BaseEntityBlock {

    public static final String NBT_MEDITATION_POS = "meditation_pos";

    public MeditationCushionBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL)
                .strength(0.5F)
                .sound(SoundType.WOOL)
                .noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                  Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (pLevel.isClientSide()) return InteractionResult.SUCCESS;
        if (!(pPlayer instanceof ServerPlayer sp)) return InteractionResult.CONSUME;

        CompoundTag pd = sp.getPersistentData();
        if (pd.contains(NBT_MEDITATION_POS)) {
            BlockPos sitPos = NbtUtils.readBlockPos(pd.getCompound(NBT_MEDITATION_POS));
            if (sitPos.equals(pPos)) {
                exitMeditation(sp, pPos, pLevel);
                return InteractionResult.sidedSuccess(false);
            }

            exitMeditationSilent(sp, sitPos, pLevel);
        }

        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (!(be instanceof MeditationCushionBlockEntity cbe)) return InteractionResult.CONSUME;
        if (cbe.hasOccupant()) {
            sp.displayClientMessage(Component.translatable("block.transcend.meditation_cushion.occupied"), true);
            return InteractionResult.CONSUME;
        }

        enterMeditation(sp, pPos, cbe, pLevel);
        return InteractionResult.sidedSuccess(false);
    }

    public static void enterMeditation(ServerPlayer sp, BlockPos pos,
                                        MeditationCushionBlockEntity cbe, Level level) {
        cbe.setOccupant(sp.getUUID());
        sp.getPersistentData().put(NBT_MEDITATION_POS, NbtUtils.writeBlockPos(pos));
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
        sp.displayClientMessage(Component.translatable("block.transcend.meditation_cushion.enter"), true);

        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new S2COpenProgramScreen());
    }

    public static void exitMeditation(ServerPlayer sp, BlockPos pos, Level level) {
        sp.getPersistentData().remove(NBT_MEDITATION_POS);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MeditationCushionBlockEntity cbe) cbe.clearOccupant();
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8F, 1.1F);
        sp.displayClientMessage(Component.translatable("block.transcend.meditation_cushion.exit"), true);
    }

    public static void evictPlayer(ServerPlayer sp, BlockPos pos) {
        sp.getPersistentData().remove(NBT_MEDITATION_POS);
        sp.displayClientMessage(Component.translatable("block.transcend.meditation_cushion.evicted"), true);
    }

    private static void exitMeditationSilent(ServerPlayer sp, BlockPos oldPos, Level level) {
        sp.getPersistentData().remove(NBT_MEDITATION_POS);
        BlockEntity be = level.getBlockEntity(oldPos);
        if (be instanceof MeditationCushionBlockEntity cbe) cbe.clearOccupant();
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos,
                          BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof MeditationCushionBlockEntity cbe && cbe.hasOccupant()) {
                if (pLevel instanceof ServerLevel sl) {
                    ServerPlayer occupant = (ServerPlayer) sl.getPlayerByUUID(cbe.getOccupantUUID());
                    if (occupant != null) evictPlayer(occupant, pPos);
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MeditationCushionBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return !pLevel.isClientSide() ?
                createTickerHelper(pBlockEntityType, ModBlockEntities.MEDITATION_CUSHION_BE.get(),
                        MeditationCushionBlockEntity::serverTick) : null;
    }
}
