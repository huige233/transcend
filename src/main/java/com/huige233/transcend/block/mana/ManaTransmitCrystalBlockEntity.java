package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ManaTransmitCrystalBlockEntity extends BlockEntity {

    public static final int MAX_RANGE = 64;
    public static final int TRANSFER_RATE = 32;
    public static final int TRANSFER_INTERVAL = 10;
    public static final double LOSS_FIXED = 0.05;

    public static final int MAX_PARTNERS = 4;

    private static final int LOCAL_SCAN_RADIUS = 4;

    private static final int BALANCE_DEAD_ZONE_ABS = 1;

    private final List<BlockPos> partners = new ArrayList<>(MAX_PARTNERS);
    private int tickCounter = 0;

    private int losCheckCounter = 0;
    private static final int LOS_CHECK_INTERVAL = 200;

    private long lastTransferTick = -1000L;

    public ManaTransmitCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_TRANSMIT_CRYSTAL_BE.get(), pos, state);
    }

    public List<BlockPos> getPartners() {
        return Collections.unmodifiableList(partners);
    }

    @Nullable
    public BlockPos getPartnerPos() {
        return partners.isEmpty() ? null : partners.get(0);
    }

    public boolean isBound() {
        return !partners.isEmpty();
    }

    public boolean isFull() {
        return partners.size() >= MAX_PARTNERS;
    }

    public int getPartnerCount() {
        return partners.size();
    }

    public boolean addPartnerSelf(BlockPos partner) {
        if (partner == null || partners.contains(partner) || partners.size() >= MAX_PARTNERS) {
            return false;
        }
        partners.add(partner);
        markDirtyAndSync();
        return true;
    }

    public boolean removePartnerSelf(BlockPos partner) {
        if (partners.remove(partner)) {
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    public void clearPartnersSelf() {
        if (!partners.isEmpty()) {
            partners.clear();
            markDirtyAndSync();
        }
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public static boolean hasLineOfSight(Level level, BlockPos posA, BlockPos posB) {
        Vec3 from = Vec3.atCenterOf(posA);
        Vec3 to   = Vec3.atCenterOf(posB);
        var hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null));

        return hit.getType() == HitResult.Type.MISS
                || hit.getBlockPos().equals(posB)
                || hit.getBlockPos().equals(posA);
    }

    public static int bindMutual(Level level, BlockPos posA, BlockPos posB) {
        if (level == null || level.isClientSide) return -3;
        if (posA.equals(posB)) return -1;
        if (posA.distSqr(posB) > (long) MAX_RANGE * MAX_RANGE) return -2;
        BlockEntity beA = level.getBlockEntity(posA);
        BlockEntity beB = level.getBlockEntity(posB);
        if (!(beA instanceof ManaTransmitCrystalBlockEntity ca) ||
                !(beB instanceof ManaTransmitCrystalBlockEntity cb)) return -3;
        if (ca.partners.contains(posB) || cb.partners.contains(posA)) return -4;
        if (ca.isFull() || cb.isFull()) return -5;
        if (!hasLineOfSight(level, posA, posB)) return -6;
        ca.addPartnerSelf(posB);
        cb.addPartnerSelf(posA);
        return 0;
    }

    public static void unbindAll(Level level, ManaTransmitCrystalBlockEntity self) {

        List<BlockPos> copy = new ArrayList<>(self.partners);
        for (BlockPos partner : copy) {
            if (level.getBlockEntity(partner) instanceof ManaTransmitCrystalBlockEntity p) {
                p.removePartnerSelf(self.getBlockPos());
            }
        }
        self.clearPartnersSelf();
    }

    public static boolean unbindOnePartner(Level level, ManaTransmitCrystalBlockEntity self, BlockPos partner) {
        boolean changed = self.removePartnerSelf(partner);
        if (level.getBlockEntity(partner) instanceof ManaTransmitCrystalBlockEntity p) {
            changed |= p.removePartnerSelf(self.getBlockPos());
        }
        return changed;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaTransmitCrystalBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < TRANSFER_INTERVAL) return;
        be.tickCounter = 0;

        if (be.partners.isEmpty()) return;

        IManaHandler selfRes = resolveSource(level, pos);
        if (selfRes == null || selfRes.getMaxManaStored() == 0) return;
        if (!selfRes.canExtract()) return;

        boolean anyTransferred = false;

        boolean doLosCheck = (++be.losCheckCounter >= LOS_CHECK_INTERVAL);
        if (doLosCheck) be.losCheckCounter = 0;

        Iterator<BlockPos> it = be.partners.iterator();
        while (it.hasNext()) {
            BlockPos partner = it.next();

            if (pos.distSqr(partner) > (long) MAX_RANGE * MAX_RANGE) {

                it.remove();
                if (level.getBlockEntity(partner) instanceof ManaTransmitCrystalBlockEntity p) {
                    p.removePartnerSelf(pos);
                }
                continue;
            }

            if (doLosCheck && !hasLineOfSight(level, pos, partner)) {
                it.remove();
                if (level.getBlockEntity(partner) instanceof ManaTransmitCrystalBlockEntity p) {
                    p.removePartnerSelf(pos);
                }
                continue;
            }
            BlockEntity partnerBe = level.getBlockEntity(partner);
            if (!(partnerBe instanceof ManaTransmitCrystalBlockEntity)) {
                it.remove();
                continue;
            }

            IManaHandler partnerRes = resolveSource(level, partner);
            if (partnerRes == null || partnerRes.getMaxManaStored() == 0) continue;
            if (!partnerRes.canReceive()) continue;
            if (selfRes.getManaStored() == 0) break;
            if (partnerRes.getManaStored() >= partnerRes.getMaxManaStored()) continue;

            int equilibriumAmount = resolveEquilibriumTransfer(selfRes, partnerRes);
            if (equilibriumAmount < BALANCE_DEAD_ZONE_ABS) continue;

            int batchTarget = Math.min(TRANSFER_RATE, equilibriumAmount);

            int extractTry = selfRes.extractMana(batchTarget, true);
            if (extractTry <= 0) continue;
            int wouldReceive = Math.max(1, (int) Math.round(extractTry * (1.0 - LOSS_FIXED)));
            int accepted = partnerRes.receiveMana(wouldReceive, true);
            if (accepted <= 0) continue;

            int actualExtract = (int) Math.ceil(accepted / (1.0 - LOSS_FIXED));
            actualExtract = Math.max(1, Math.min(actualExtract, extractTry));

            selfRes.extractMana(actualExtract, false);
            partnerRes.receiveMana(accepted, false);

            anyTransferred = true;
            emitTransferParticles(level, pos, partner);
        }

        if (anyTransferred) {
            be.lastTransferTick = level.getGameTime();
            be.setChanged();
            if (level instanceof ServerLevel) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    private static int resolveEquilibriumTransfer(IManaHandler self, IManaHandler partner) {
        long selfMana = self.getManaStored();
        long partnerMana = partner.getManaStored();
        long partnerMax = partner.getMaxManaStored();

        if (selfMana <= partnerMana) return 0;

        long partnerSpace = partnerMax - partnerMana;
        if (partnerSpace <= 0) return 0;

        double rawX = (selfMana - partnerMana) / (2.0 - LOSS_FIXED);

        double xCapByPartner = partnerSpace / (1.0 - LOSS_FIXED);

        double x = Math.min(rawX, xCapByPartner);
        x = Math.min(x, (double) selfMana);

        return (int) Math.floor(x);
    }

    @Nullable
    private static IManaHandler resolveSource(Level level, BlockPos crystalPos) {
        BlockPos below = crystalPos.below();
        BlockEntity belowBe = level.getBlockEntity(below);
        if (belowBe != null) {
            IManaHandler cap = belowBe.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
            if (cap != null) return cap;
        }
        IManaHandler best = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -LOCAL_SCAN_RADIUS; dx <= LOCAL_SCAN_RADIUS; dx++) {
            for (int dy = -LOCAL_SCAN_RADIUS; dy <= LOCAL_SCAN_RADIUS; dy++) {
                for (int dz = -LOCAL_SCAN_RADIUS; dz <= LOCAL_SCAN_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    m.set(below.getX() + dx, below.getY() + dy, below.getZ() + dz);
                    BlockEntity nb = level.getBlockEntity(m);
                    if (nb == null) continue;
                    IManaHandler cap = nb.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
                    if (cap == null) continue;
                    double d = below.distSqr(m);
                    if (d < bestDistSq) {
                        bestDistSq = d;
                        best = cap;
                    }
                }
            }
        }
        return best;
    }

    private static void emitTransferParticles(Level level, BlockPos src, BlockPos dst) {
        if (!(level instanceof ServerLevel sl)) return;
        DustParticleOptions options = new DustParticleOptions(new Vector3f(0.4F, 0.85F, 1.0F), 0.8F);
        sl.sendParticles(options,
                src.getX() + 0.5, src.getY() + 0.4, src.getZ() + 0.5,
                2, 0.1, 0.1, 0.1, 0.0);
        sl.sendParticles(options,
                dst.getX() + 0.5, dst.getY() + 0.4, dst.getZ() + 0.5,
                2, 0.1, 0.1, 0.1, 0.0);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ManaTransmitCrystalBlockEntity be) {
        if (be.partners.isEmpty()) return;
        if (level.getGameTime() % 20 != 0) return;

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        for (int i = 0; i < 3; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double r = 0.3 + level.random.nextDouble() * 0.2;
            level.addParticle(ParticleTypes.END_ROD,
                    cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r,
                    0, 0.02, 0);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        if (!partners.isEmpty()) {
            ListTag list = new ListTag();
            for (BlockPos p : partners) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("x", p.getX());
                entry.putInt("y", p.getY());
                entry.putInt("z", p.getZ());
                list.add(entry);
            }
            tag.put("partners", list);
        }
        tag.putLong("last_transfer", lastTransferTick);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        partners.clear();

        if (tag.contains("partners", Tag.TAG_LIST)) {
            ListTag list = tag.getList("partners", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && partners.size() < MAX_PARTNERS; i++) {
                CompoundTag entry = list.getCompound(i);
                partners.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
            }
        }

        if (partners.isEmpty() && tag.contains("partner_x")) {
            partners.add(new BlockPos(
                    tag.getInt("partner_x"),
                    tag.getInt("partner_y"),
                    tag.getInt("partner_z")));
        }
        if (tag.contains("last_transfer")) {
            lastTransferTick = tag.getLong("last_transfer");
        }
    }

    public boolean isTransferActive() {
        if (level == null) return false;
        return level.getGameTime() - lastTransferTick < 30L;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
