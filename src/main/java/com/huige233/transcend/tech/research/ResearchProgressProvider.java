package com.huige233.transcend.tech.research;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;


/** 向玩家暴露单个研究进度能力实例，并提供其状态的显式 NBT 保存与加载入口。 */
public final class ResearchProgressProvider implements ICapabilityProvider {
    private final ResearchProgress progress = new ResearchProgress();
    private final LazyOptional<ResearchProgress> optional = LazyOptional.of(() -> progress);
    @Override public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        return cap == ResearchCapabilities.PROGRESS ? optional.cast() : LazyOptional.empty();
    }
    public void save(CompoundTag tag) { progress.save(tag); }
    public void load(CompoundTag tag) { progress.load(tag); }
}
