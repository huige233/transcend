package com.huige233.transcend.tech.research;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;


/** 向玩家暴露单个研究进度能力实例，并提供其状态的显式 NBT 保存与加载入口。 */
public final class ResearchProgressProvider implements ICapabilitySerializable<CompoundTag> {
    private final ResearchProgress progress = new ResearchProgress();
    private LazyOptional<ResearchProgress> optional = LazyOptional.of(() -> progress);
    @Override public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap != ResearchCapabilities.PROGRESS) return LazyOptional.empty();
        // 玩家在 Clone 期间临时 reviveCaps；旧引用保持失效，新查询重新暴露同一份进度。
        if (!optional.isPresent()) optional = LazyOptional.of(() -> progress);
        return optional.cast();
    }
    public void save(CompoundTag tag) { progress.save(tag); }
    public void load(CompoundTag tag) { progress.load(tag); }
    @Override public CompoundTag serializeNBT() { CompoundTag tag = new CompoundTag(); progress.save(tag); return tag; }
    @Override public void deserializeNBT(CompoundTag tag) { progress.load(tag); }
    public void invalidate() { optional.invalidate(); }
}
