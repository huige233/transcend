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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 魔力传输水晶的方块实体。
 *
 * <p><b>R63 重构 — Astral Sorcery 风格折射多链</b>：原 1↔1 单一绑定升级为 1↔N 多伙伴
 * 折射网络，单个水晶最多可绑定 {@link #MAX_PARTNERS} 个伙伴。响应玩家诉求"在法环上
 * 制作一个类似星辉魔法的水晶 折射出去给供应/抽取魔力"。
 *
 * <p>每 tick 对所有伙伴依次执行 R66 的绝对平衡数学，自然形成"中枢→多消费端"的折射结构：
 * 把水晶放在 mana_reservoir 上方，绑定到 4 个法环上方的水晶 → 一池魔力均匀分配到 4 个法环。
 *
 * <p>R61 + R62 + R63 + R66 联合特性：
 * <ul>
 *   <li>R66 平衡数学：每对伙伴独立解出"使两端绝对数量相等的传输量"（R61 百分比方案被否决，
 *       玩家要求"完全平衡 不是百分比平衡"）；避免几何收敛 bug</li>
 *   <li>R62 客户端 beam 渲染：每个伙伴一束激光，多束在中枢处自然加性叠加</li>
 *   <li>R63 多链：单晶最多 4 伙伴；旧 NBT (single partner) 自动迁移到 List 第一项</li>
 *   <li>R66 放开 placement：水晶可放在任何有支撑顶面的方块上（不再限制 3 种基底）</li>
 * </ul>
 *
 * <p>固定 {@value #LOSS_FIXED} 的传输损耗（不变）；最大距离 {@link #MAX_RANGE} 格（不变）。
 */
public class ManaTransmitCrystalBlockEntity extends BlockEntity {

    public static final int MAX_RANGE = 64;
    public static final int TRANSFER_RATE = 32;
    public static final int TRANSFER_INTERVAL = 10;
    public static final double LOSS_FIXED = 0.05;

    /**
     * R63: 单个水晶可同时持有的最大伙伴数。
     * 4 对应"东南西北 4 路折射"的星辉式视觉/玩法，是星辉 Refractor 的传统数。
     * 严格上限，超出则 add 操作返回 false。
     */
    public static final int MAX_PARTNERS = 4;

    private static final int LOCAL_SCAN_RADIUS = 4;
    /**
     * R61 → R66: 平衡死区（绝对单位，CM）。当解析到的"使两端绝对数量相等所需要的传输量"
     * 小于此值时停止。R66 之前用百分比阈值（0.5%）触发停摆，玩家反馈"百分比平衡不是想要的"，
     * 现统一用绝对差阈值：差 ≥ 1 CM 就继续推。
     */
    private static final int BALANCE_DEAD_ZONE_ABS = 1;

    /**
     * R63: 已绑定的伙伴坐标列表。空列表 = 未绑定，非空 = 已绑定到 1-4 个伙伴。
     * 顺序保留以保证 NBT 序列化稳定（影响渲染颜色的可重复性等）。
     */
    private final List<BlockPos> partners = new ArrayList<>(MAX_PARTNERS);
    private int tickCounter = 0;
    /**
     * R62: 最近一次成功传输的 game time。客户端用于判断 beam 渲染应"亮起"还是"暗淡"。
     * 通过 {@link #getUpdateTag()} 同步给客户端；超过 30 ticks 未更新视为闲置。
     */
    private long lastTransferTick = -1000L;

    public ManaTransmitCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_TRANSMIT_CRYSTAL_BE.get(), pos, state);
    }

    /**
     * R63: 获取所有绑定伙伴的不可变视图。客户端 BER 用此迭代渲染所有 beam。
     */
    public List<BlockPos> getPartners() {
        return Collections.unmodifiableList(partners);
    }

    /**
     * 兼容方法 — 返回首个伙伴（如有）。R63 之前的 1↔1 时代调用方仍可用。
     */
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

    /**
     * R63: 单侧添加一个伙伴。不维护反向引用，调用方应使用
     * {@link #bindMutual(Level, BlockPos, BlockPos)} 完成双向绑定。
     *
     * @return true 添加成功，false 已存在或已满
     */
    public boolean addPartnerSelf(BlockPos partner) {
        if (partner == null || partners.contains(partner) || partners.size() >= MAX_PARTNERS) {
            return false;
        }
        partners.add(partner);
        markDirtyAndSync();
        return true;
    }

    /**
     * R63: 单侧移除一个伙伴。不维护反向引用。
     */
    public boolean removePartnerSelf(BlockPos partner) {
        if (partners.remove(partner)) {
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * R63: 单侧清空所有伙伴。不维护反向引用。
     */
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

    /**
     * 在两个水晶之间建立双向绑定（追加模式 — 不会清除已有伙伴）。
     *
     * <p>失败条件：
     * <ul>
     *   <li>同坐标</li>
     *   <li>超出 {@link #MAX_RANGE}</li>
     *   <li>任一端不是合法水晶 BE</li>
     *   <li>任一端的伙伴列表已满 ({@link #MAX_PARTNERS})</li>
     *   <li>已经绑定过（A 已在 B 的列表中或反之）</li>
     * </ul>
     *
     * @return 0 = 成功；负数 = 失败原因码：
     *   -1 同位 / -2 超距 / -3 非水晶 BE / -4 已绑定 / -5 任一侧已满
     */
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
        ca.addPartnerSelf(posB);
        cb.addPartnerSelf(posA);
        return 0;
    }

    /**
     * 解除本水晶的所有绑定，并把每个对端 BE 上指回自己的反向链接也一并清空。
     * 主要用于方块破坏时的清理。
     */
    public static void unbindAll(Level level, ManaTransmitCrystalBlockEntity self) {
        // 复制以避免 ConcurrentModificationException
        List<BlockPos> copy = new ArrayList<>(self.partners);
        for (BlockPos partner : copy) {
            if (level.getBlockEntity(partner) instanceof ManaTransmitCrystalBlockEntity p) {
                p.removePartnerSelf(self.getBlockPos());
            }
        }
        self.clearPartnersSelf();
    }

    /**
     * 解除本水晶到指定伙伴的单一绑定（双向移除）。
     *
     * @return true 移除成功
     */
    public static boolean unbindOnePartner(Level level, ManaTransmitCrystalBlockEntity self, BlockPos partner) {
        boolean changed = self.removePartnerSelf(partner);
        if (level.getBlockEntity(partner) instanceof ManaTransmitCrystalBlockEntity p) {
            changed |= p.removePartnerSelf(self.getBlockPos());
        }
        return changed;
    }

    /**
     * 服务端 tick：对所有绑定伙伴依次执行一次 R61 平衡式 P2P 传输。
     *
     * <p>多伙伴时，每个伙伴各自享有完整的 {@link #TRANSFER_RATE} 预算 — 这是有意设计：
     * 让中枢水晶（绑定多个消费端）总吞吐随伙伴数线性增长，符合"折射放大"的视觉直觉。
     * 单个 tick 内任一伙伴成功传输即标记 lastTransferTick 用于客户端"活跃" 视觉。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaTransmitCrystalBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < TRANSFER_INTERVAL) return;
        be.tickCounter = 0;

        if (be.partners.isEmpty()) return;

        // 自源（中枢）解析一次复用
        IManaHandler selfRes = resolveSource(level, pos);
        if (selfRes == null || selfRes.getMaxManaStored() == 0) return;
        if (!selfRes.canExtract()) return;

        boolean anyTransferred = false;

        // R63: 安全迭代 — 中途可能因悬挂引用而 unbind，使用 iterator + remove
        Iterator<BlockPos> it = be.partners.iterator();
        while (it.hasNext()) {
            BlockPos partner = it.next();

            if (pos.distSqr(partner) > (long) MAX_RANGE * MAX_RANGE) {
                // 超距 — 移除并请求对端也移除（best effort）
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
            if (selfRes.getManaStored() == 0) break; // 中枢已耗尽，提早退出
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

    /**
     * R61 → R66 重写：计算"使两端绝对数量相等所需要的本端→对端传输量"，已含 {@link #LOSS_FIXED} 损耗补偿。
     *
     * <p><b>R66 改动</b>：用户反馈"魔力传输之后是完全平衡的 不是百分比平衡" — R61 的公式是按
     * 百分比平衡（让 self% = partner%），不符合直觉。R66 改为绝对数量平衡（让 selfMana = partnerMana）。
     *
     * <p>推导（s=本端存量, p=对端存量, S=本端容量, P=对端容量, x=传输量, L=损耗率）：
     * <pre>
     *   (s - x) = (p + x*(1-L))            ← 目标：两端 mana 数量绝对相等
     *   ⇒ x = (s - p) / (2 - L)
     * </pre>
     *
     * <p>受到的约束：
     * <ul>
     *   <li>x ≤ s （不能抽取超过自身存量）</li>
     *   <li>x*(1-L) ≤ P - p （不能溢出对端容量）→ x ≤ (P - p) / (1 - L)</li>
     *   <li>s &gt; p （仅当本端确实多于对端时才推）</li>
     * </ul>
     *
     * <p>边界情况：
     * <ul>
     *   <li>对端容量小：x 被 partner space 约束，对端被填满，本端剩 (s - x)，无法绝对相等 — 这是物理约束</li>
     *   <li>本端容量小：常规情况，公式仍正确，x 受 (s-p)/(2-L) 约束</li>
     *   <li>容量差异大（如 10000 reservoir vs 200 dock）：dock 始终保持接近满，reservoir 缓慢供应</li>
     * </ul>
     */
    private static int resolveEquilibriumTransfer(IManaHandler self, IManaHandler partner) {
        long selfMana = self.getManaStored();
        long partnerMana = partner.getManaStored();
        long partnerMax = partner.getMaxManaStored();

        // 本端不多于对端 → 不推送
        if (selfMana <= partnerMana) return 0;

        // 对端无空间 → 不推送
        long partnerSpace = partnerMax - partnerMana;
        if (partnerSpace <= 0) return 0;

        // R66: 绝对平衡公式 — x = (selfMana - partnerMana) / (2 - LOSS_FIXED)
        double rawX = (selfMana - partnerMana) / (2.0 - LOSS_FIXED);

        // 对端容量约束 — x*(1-L) ≤ partnerSpace ⇒ x ≤ partnerSpace / (1-L)
        double xCapByPartner = partnerSpace / (1.0 - LOSS_FIXED);

        double x = Math.min(rawX, xCapByPartner);
        x = Math.min(x, (double) selfMana);

        return (int) Math.floor(x);
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
        // R63: 多伙伴 — 写入 ListTag
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
        // R63: 优先尝试新格式 partners ListTag
        if (tag.contains("partners", Tag.TAG_LIST)) {
            ListTag list = tag.getList("partners", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && partners.size() < MAX_PARTNERS; i++) {
                CompoundTag entry = list.getCompound(i);
                partners.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
            }
        }
        // R63: 兼容旧 R42 单伙伴格式
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

    /**
     * R62: 客户端 BER 用于判断"激光是否处于活跃传输态"。
     * 30 tick (~1.5s) 内有过传输事件视为活跃，触发亮色脉冲；否则渲染暗色待机束。
     */
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
