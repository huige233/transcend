package com.huige233.transcend.block;

import com.huige233.transcend.spell.MagicCrystalHelper;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MeditationCushionBlockEntity extends BlockEntity {

    private static final String TAG_OCCUPANT = "OccupantUUID";

    private static final float BASE_REGEN_PER_SEC = 8f;
    private static final int TICK_INTERVAL = 20;

    private static final float MAX_DIST_SQ = 9f;

    @Nullable private UUID occupantUUID;
    private int tickCounter = 0;

    public MeditationCushionBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MEDITATION_CUSHION_BE.get(), pPos, pBlockState);
    }

    public boolean hasOccupant() { return occupantUUID != null; }

    @Nullable
    public UUID getOccupantUUID() { return occupantUUID; }

    public void setOccupant(UUID uuid) {
        this.occupantUUID = uuid;
        setChanged();
    }

    public void clearOccupant() {
        this.occupantUUID = null;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   MeditationCushionBlockEntity be) {
        if (++be.tickCounter < TICK_INTERVAL) return;
        be.tickCounter = 0;

        if (be.occupantUUID == null) return;
        if (!(level instanceof ServerLevel sl)) return;

        ServerPlayer player = (ServerPlayer) sl.getPlayerByUUID(be.occupantUUID);
        if (player == null || !player.isAlive()) {
            be.clearOccupant();
            return;
        }

        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        if (dx * dx + dz * dz > MAX_DIST_SQ) {
            MeditationCushionBlock.evictPlayer(player, pos);
            be.clearOccupant();
            return;
        }

        ChunkManaSavedData aura = ChunkManaSavedData.get(sl);
        float chunkMana = aura.getMana(player.chunkPosition());
        float baseline  = aura.getManaBaseline(player.chunkPosition());
        float flux      = aura.getFlux(player.chunkPosition());

        float auraFactor = (baseline > 0) ? Math.min(2f, chunkMana / baseline) : 0f;

        float fluxFactor = Math.max(0.1f, 1f - (flux / ChunkManaSavedData.MAX_FLUX) * 0.9f);

        float regenThisSec = BASE_REGEN_PER_SEC * auraFactor * fluxFactor;
        if (regenThisSec <= 0) return;

        int current = MagicCrystalHelper.getInnateMana(player);
        int max     = MagicCrystalHelper.getInnateMaxMana(player);
        if (current < max) {
            MagicCrystalHelper.setInnateMana(player, Math.min(max, current + Math.round(regenThisSec)));
        }
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        occupantUUID = pTag.hasUUID(TAG_OCCUPANT) ? pTag.getUUID(TAG_OCCUPANT) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (occupantUUID != null) pTag.putUUID(TAG_OCCUPANT, occupantUUID);
    }
}
