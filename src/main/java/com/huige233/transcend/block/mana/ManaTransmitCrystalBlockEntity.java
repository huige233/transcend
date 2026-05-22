package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 魔力传输水晶的方块实体：与另一个同类型水晶建立一对一无线绑定，
 * 周期性在两端之间执行平衡式 mana 传输。
 * 流向由当前两端的 mana 百分比决定，固定 {@value #LOSS_FIXED} 的损耗。
 */
public class ManaTransmitCrystalBlockEntity extends BlockEntity {

    public static final int MAX_RANGE = 64;
    public static final int TRANSFER_RATE = 32;
    public static final int TRANSFER_INTERVAL = 10;
    public static final double LOSS_FIXED = 0.05;

    private static final int LOCAL_SCAN_RADIUS = 4;
    private static final double BALANCE_DEAD_ZONE = 0.005;

    @Nullable
    private BlockPos partnerPos = null;
    private int tickCounter = 0;

    public ManaTransmitCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_TRANSMIT_CRYSTAL_BE.get(), pos, state);
    }

    @Nullable
    public BlockPos getPartnerPos() {
        return partnerPos;
    }

    public boolean isBound() {
        return partnerPos != null;
    }

    /**
     * 设置本水晶单侧的绑定目标并向客户端广播状态。
     * 不会主动维护对端的反向引用，调用方应自行保证一致性，
     * 或使用 {@link #bindMutual(Level, BlockPos, BlockPos)} 双向绑定。
     */
    public void bindTo(@Nullable BlockPos partner) {
        this.partnerPos = partner;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 在两个水晶之间建立双向绑定，并清理双方原有 partner 的反向引用以防出现悬挂指针。
     * 失败条件：同坐标、超出 {@link #MAX_RANGE}、任一端不是合法水晶 BE。
     *
     * @return 成功绑定返回 true
     */
    public static boolean bindMutual(Level level, BlockPos posA, BlockPos posB) {
        if (level == null || level.isClientSide) return false;
        if (posA.equals(posB)) return false;
        if (posA.distSqr(posB) > (long) MAX_RANGE * MAX_RANGE) return false;
        BlockEntity beA = level.getBlockEntity(posA);
        BlockEntity beB = level.getBlockEntity(posB);
        if (!(beA instanceof ManaTransmitCrystalBlockEntity ca) ||
                !(beB instanceof ManaTransmitCrystalBlockEntity cb)) return false;
        unbindOne(level, ca);
        unbindOne(level, cb);
        ca.bindTo(posB);
        cb.bindTo(posA);
        return true;
    }

    /** 解除本水晶的绑定，并把对端 BE 上指回自己的反向链接一并清空。 */
    public static void unbindOne(Level level, ManaTransmitCrystalBlockEntity self) {
        BlockPos partner = self.partnerPos;
        self.bindTo(null);
        if (partner != null && level.getBlockEntity(partner) instanceof ManaTransmitCrystalBlockEntity p) {
            if (self.getBlockPos().equals(p.partnerPos)) {
                p.bindTo(null);
            }
        }
    }

    /**
     * 服务端 tick：执行一次平衡式 P2P 传输。
     * 仅当本端 mana 百分比高于对端且差值超过 {@link #BALANCE_DEAD_ZONE} 时发起，
     * 单次传输量被限制为差量的一半以避免来回反转。
     * 对端 BE 在自己 tick 时执行对称逻辑，由此形成自然平衡。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaTransmitCrystalBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < TRANSFER_INTERVAL) return;
        be.tickCounter = 0;

        if (be.partnerPos == null) return;
        if (pos.distSqr(be.partnerPos) > (long) MAX_RANGE * MAX_RANGE) {
            unbindOne(level, be);
            return;
        }
        BlockEntity partnerBe = level.getBlockEntity(be.partnerPos);
        if (!(partnerBe instanceof ManaTransmitCrystalBlockEntity)) {
            unbindOne(level, be);
            return;
        }

        IManaHandler selfRes = resolveSource(level, pos);
        IManaHandler partnerRes = resolveSource(level, be.partnerPos);
        if (selfRes == null || partnerRes == null) return;
        if (selfRes.getMaxManaStored() == 0 || partnerRes.getMaxManaStored() == 0) return;

        double selfPct = selfRes.getManaStored() / (double) selfRes.getMaxManaStored();
        double partnerPct = partnerRes.getManaStored() / (double) partnerRes.getMaxManaStored();
        if (selfPct - partnerPct < BALANCE_DEAD_ZONE) return;

        if (!selfRes.canExtract() || !partnerRes.canReceive()) return;
        if (selfRes.getManaStored() == 0) return;
        if (partnerRes.getManaStored() >= partnerRes.getMaxManaStored()) return;

        int diffMana = (int) Math.round((selfPct - partnerPct) *
                Math.min(selfRes.getMaxManaStored(), partnerRes.getMaxManaStored()));
        int batchTarget = Math.min(TRANSFER_RATE, Math.max(1, diffMana / 2));

        int extractTry = selfRes.extractMana(batchTarget, true);
        if (extractTry <= 0) return;
        int wouldReceive = Math.max(1, (int) Math.round(extractTry * (1.0 - LOSS_FIXED)));
        int accepted = partnerRes.receiveMana(wouldReceive, true);
        if (accepted <= 0) return;

        int actualExtract = (int) Math.ceil(accepted / (1.0 - LOSS_FIXED));
        actualExtract = Math.max(1, Math.min(actualExtract, extractTry));

        selfRes.extractMana(actualExtract, false);
        partnerRes.receiveMana(accepted, false);

        emitTransferParticles(level, pos, be.partnerPos);
    }

    /**
     * 解析水晶可访问的 mana 源：优先取下方方块的 capability，
     * 若下方不携带 capability（典型场景：装饰用的符文石），则在 {@link #LOCAL_SCAN_RADIUS}
     * 球范围内查找距离最近的 capability 提供者作为后备来源。
     */
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

    /** 在传输事件发生时，于两端各释放少量蓝色尘埃粒子作为玩家可见反馈。 */
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

    /** 客户端 tick：在已绑定状态下每秒发射数粒末影粒子作为脉动呼吸效果。 */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ManaTransmitCrystalBlockEntity be) {
        if (be.partnerPos == null) return;
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
        if (partnerPos != null) {
            tag.putInt("partner_x", partnerPos.getX());
            tag.putInt("partner_y", partnerPos.getY());
            tag.putInt("partner_z", partnerPos.getZ());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("partner_x")) {
            partnerPos = new BlockPos(tag.getInt("partner_x"), tag.getInt("partner_y"), tag.getInt("partner_z"));
        } else {
            partnerPos = null;
        }
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
