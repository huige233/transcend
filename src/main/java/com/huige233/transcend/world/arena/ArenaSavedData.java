package com.huige233.transcend.world.arena;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 持久化存储：追踪竞技场结构是否已生成。
 * 存储在竞技场维度的 data 文件夹中。
 */
public class ArenaSavedData extends SavedData {

    private static final String DATA_NAME = "transcend_arena";
    private boolean built;

    public ArenaSavedData() {
    }

    public static ArenaSavedData load(CompoundTag tag) {
        ArenaSavedData data = new ArenaSavedData();
        data.built = tag.getBoolean("Built");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Built", built);
        return tag;
    }

    public boolean isBuilt() {
        return built;
    }

    public void markBuilt() {
        this.built = true;
        setDirty();
    }

    public static ArenaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ArenaSavedData::load,
                ArenaSavedData::new,
                DATA_NAME
        );
    }
}
