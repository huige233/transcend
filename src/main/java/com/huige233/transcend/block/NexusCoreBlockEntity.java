package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.world.nexus.NexusType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 法则枢纽核心方块实体 — 存储所属枢纽类型，客户端生成粒子特效。
 */
public class NexusCoreBlockEntity extends BlockEntity {

    private String nexusTypeId = "";
    private int lockTicks = 0;
    private static final Random RAND = new Random();

    public NexusCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEXUS_CORE_BE.get(), pos, state);
    }

    public String getNexusTypeId() {
        return nexusTypeId;
    }

    public void setNexusType(String id) {
        this.nexusTypeId = id;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getLockTicks() {
        return lockTicks;
    }

    public void setLockTicks(int ticks) {
        this.lockTicks = ticks;
        setChanged();
    }

    public boolean isLocked() {
        return lockTicks > 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("NexusType", nexusTypeId);
        tag.putInt("LockTicks", lockTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nexusTypeId = tag.getString("NexusType");
        lockTicks = tag.getInt("LockTicks");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putString("NexusType", nexusTypeId);
        tag.putInt("LockTicks", lockTicks);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 服务端Tick — 锁定倒计时。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, NexusCoreBlockEntity be) {
        if (be.lockTicks > 0) {
            be.lockTicks--;
            if (be.lockTicks == 0) {
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    /**
     * 客户端粒子特效 — 符文粒子螺旋上升 + 环境粒子。
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, NexusCoreBlockEntity be) {
        if (!level.isClientSide) return;

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // Determine color from nexus type
        NexusType type = NexusType.getById(be.nexusTypeId);
        float hue = type != null ? type.ordinal() * 0.2F : 0.5F;

        // Spiral particles rising upward
        long tick = level.getGameTime();
        double angle = (tick % 40) * Math.PI * 2.0 / 40.0;
        double radius = 0.6;
        double px = cx + Math.cos(angle) * radius;
        double pz = cz + Math.sin(angle) * radius;
        level.addParticle(ParticleTypes.END_ROD,
                px, cy + (tick % 20) * 0.1, pz,
                0, 0.05, 0);

        // Ambient enchant particles
        if (RAND.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT,
                    cx + (RAND.nextDouble() - 0.5) * 2.0,
                    cy + RAND.nextDouble() * 2.0,
                    cz + (RAND.nextDouble() - 0.5) * 2.0,
                    0, -0.1, 0);
        }

        // Soul particles
        if (RAND.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SOUL,
                    cx + (RAND.nextDouble() - 0.5) * 1.5,
                    cy + 0.5,
                    cz + (RAND.nextDouble() - 0.5) * 1.5,
                    0, 0.02, 0);
        }
    }
}
