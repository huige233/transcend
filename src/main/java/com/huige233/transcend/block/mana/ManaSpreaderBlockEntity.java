package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import com.huige233.transcend.mana.SimpleManaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 魔力散发器方块实体。
 *
 * <p>每 10 ticks 主逻辑：
 * <ol>
 *   <li><b>抽取</b>：从 6 个相邻方块的 IManaHandler (canExtract) 中拉取魔力到本机缓冲。</li>
 *   <li><b>推送</b>：若有绑定目标且其 IManaHandler 可接收，
 *       从本机缓冲推送魔力，并在两点间生成粒子轨迹（Botania 风格视觉反馈）。</li>
 * </ol>
 *
 * <p>本实现采用"瞬时连接传输"模式（无独立 ManaBurst 实体），
 * 实现简单且无实体性能开销。粒子由服务端在两点间逐 tick 生成，
 * 客户端通过原版粒子系统接收（不需要专门的 S2C 包）。
 */
public class ManaSpreaderBlockEntity extends BlockEntity {

    public static final String BE_ID = "mana_spreader_be";

    /** 内部缓冲容量（CM）。 */
    public static final int CAPACITY = 256;
    /** 每次推送/拉取的最大数量。 */
    public static final int TRANSFER_PER_TICK = 16;
    /** 主 tick 间隔。 */
    private static final int TICK_INTERVAL = 10;
    /** 散发器与目标允许的最大距离（方块）。 */
    public static final int MAX_RANGE = 16;

    private final SimpleManaStorage storage;
    private BlockPos boundTarget = null;
    private int tickCounter = 0;

    /** 当前是否正在传输（用于客户端动画与渲染颜色脉冲，可后续扩展）。 */
    private boolean transmitting = false;

    private final LazyOptional<IManaHandler> manaCap;

    public ManaSpreaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_SPREADER_BE.get(), pos, state);
        this.storage = new SimpleManaStorage(CAPACITY, TRANSFER_PER_TICK, TRANSFER_PER_TICK);
        this.manaCap = LazyOptional.of(() -> this.storage);
    }

    // ============================================================
    // 状态
    // ============================================================

    public int getStoredMana() { return storage.getManaStored(); }
    public int getCapacity() { return storage.getMaxManaStored(); }
    public BlockPos getBoundTarget() { return boundTarget; }

    public void setBoundTarget(@Nullable BlockPos target) {
        this.boundTarget = target == null ? null : target.immutable();
        setChanged();
    }

    // ============================================================
    // Tick
    // ============================================================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaSpreaderBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.tickCounter++;
        if (be.tickCounter < TICK_INTERVAL) return;
        be.tickCounter = 0;

        boolean changed = false;
        be.transmitting = false;

        // === 1) 抽取相邻可输出节点 ===
        int free = be.storage.getMaxManaStored() - be.storage.getManaStored();
        if (free > 0) {
            for (Direction dir : Direction.values()) {
                if (free <= 0) break;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null || neighbor == be) continue;
                LazyOptional<IManaHandler> opt = neighbor.getCapability(
                        ManaHandlerCapability.MANA_HANDLER, dir.getOpposite());
                IManaHandler handler = opt.resolve().orElse(null);
                if (handler == null || !handler.canExtract()) continue;

                int simulated = handler.extractMana(Math.min(free, TRANSFER_PER_TICK), true);
                if (simulated <= 0) continue;
                int accepted = be.storage.receiveMana(simulated, false);
                if (accepted > 0) {
                    handler.extractMana(accepted, false);
                    free -= accepted;
                    changed = true;
                }
            }
        }

        // === 2) 推送给绑定目标 ===
        if (be.boundTarget != null && be.storage.getManaStored() > 0) {
            // 距离守卫
            int dx = be.boundTarget.getX() - pos.getX();
            int dy = be.boundTarget.getY() - pos.getY();
            int dz = be.boundTarget.getZ() - pos.getZ();
            int distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > MAX_RANGE * MAX_RANGE) {
                be.boundTarget = null;
                changed = true;
            } else {
                BlockEntity targetBe = level.getBlockEntity(be.boundTarget);
                if (targetBe == null || targetBe == be) {
                    // 目标不再有效（区块卸载或方块被破坏后短暂残留），保持目标但本轮不传
                } else {
                    LazyOptional<IManaHandler> opt = targetBe.getCapability(
                            ManaHandlerCapability.MANA_HANDLER, null);
                    IManaHandler handler = opt.resolve().orElse(null);
                    if (handler != null && handler.canReceive()) {
                        int extractable = be.storage.extractMana(TRANSFER_PER_TICK, true);
                        int accepted = handler.receiveMana(extractable, false);
                        if (accepted > 0) {
                            be.storage.extractMana(accepted, false);
                            changed = true;
                            be.transmitting = true;

                            // 视觉反馈：从散发器中心到目标中心绘制粒子轨迹
                            spawnFlowParticles(serverLevel, pos, be.boundTarget, accepted);

                            // 微弱声音反馈（每秒约 1 次）
                            if (level.random.nextFloat() < 0.25F) {
                                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                                        SoundSource.BLOCKS, 0.18F,
                                        1.5F + level.random.nextFloat() * 0.3F);
                            }
                        }
                    }
                }
            }
        }

        if (changed) {
            be.setChanged();
        }
    }

    // ============================================================
    // 视觉
    // ============================================================

    /** 在散发器与目标之间撒下若干 portal/glow 粒子，密度按传输量微调。 */
    private static void spawnFlowParticles(ServerLevel level, BlockPos from, BlockPos to, int amount) {
        double sx = from.getX() + 0.5;
        double sy = from.getY() + 0.6;
        double sz = from.getZ() + 0.5;
        double tx = to.getX() + 0.5;
        double ty = to.getY() + 0.6;
        double tz = to.getZ() + 0.5;

        double dx = tx - sx;
        double dy = ty - sy;
        double dz = tz - sz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.001) return;

        // 粒子数：基础 8 + 每点 mana 增加少许，钳到 24
        int particles = Math.min(24, 8 + amount / 4);
        for (int i = 0; i < particles; i++) {
            double t = (i + 1.0) / (particles + 1.0);
            double px = sx + dx * t;
            double py = sy + dy * t + Math.sin(t * Math.PI) * 0.18; // 微弧线
            double pz = sz + dz * t;
            level.sendParticles(ParticleTypes.PORTAL, px, py, pz, 1, 0, 0, 0, 0.0);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.WITCH, px, py + 0.05, pz, 1,
                        0, 0, 0, 0.0);
            }
        }
        // 起点闪光
        level.sendParticles(ParticleTypes.ENCHANT, sx, sy + 0.2, sz, 4,
                0.1, 0.1, 0.1, 0.05);
        // 终点闪光
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, tx, ty + 0.2, tz, 4,
                0.1, 0.1, 0.1, 0.0);
    }

    // ============================================================
    // NBT
    // ============================================================

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Storage", storage.serializeNBT());
        if (boundTarget != null) {
            tag.putLong("BoundTarget", boundTarget.asLong());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Storage")) {
            storage.deserializeNBT(tag.getCompound("Storage"));
        }
        boundTarget = tag.contains("BoundTarget")
                ? BlockPos.of(tag.getLong("BoundTarget")) : null;
    }

    // ============================================================
    // Forge Capability
    // ============================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaHandlerCapability.MANA_HANDLER) {
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCap.invalidate();
    }
}
