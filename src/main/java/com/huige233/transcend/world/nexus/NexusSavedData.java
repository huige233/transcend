package com.huige233.transcend.world.nexus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NexusSavedData extends SavedData {

    private static final String DATA_NAME = "transcend_nexus";
    private final Set<String> placedNexuses = new HashSet<>();
    private final Set<String> destroyedNexuses = new HashSet<>();

    public NexusSavedData() {
    }

    public static NexusSavedData load(CompoundTag tag) {
        NexusSavedData data = new NexusSavedData();
        ListTag placed = tag.getList("Placed", Tag.TAG_STRING);
        for (int i = 0; i < placed.size(); i++) {
            data.placedNexuses.add(placed.getString(i));
        }
        ListTag destroyed = tag.getList("Destroyed", Tag.TAG_STRING);
        for (int i = 0; i < destroyed.size(); i++) {
            data.destroyedNexuses.add(destroyed.getString(i));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag placed = new ListTag();
        for (String id : placedNexuses) {
            placed.add(StringTag.valueOf(id));
        }
        tag.put("Placed", placed);

        ListTag destroyed = new ListTag();
        for (String id : destroyedNexuses) {
            destroyed.add(StringTag.valueOf(id));
        }
        tag.put("Destroyed", destroyed);
        return tag;
    }

    public boolean isPlaced(NexusType type) {
        return placedNexuses.contains(type.id);
    }

    public void markPlaced(NexusType type) {
        placedNexuses.add(type.id);
        setDirty();
    }

    public boolean isDestroyed(NexusType type) {
        return destroyedNexuses.contains(type.id);
    }

    public void markDestroyed(NexusType type) {
        destroyedNexuses.add(type.id);
        setDirty();
    }

    public Set<String> getDestroyedIds() {
        return Collections.unmodifiableSet(destroyedNexuses);
    }

    public int getDestroyedCount() {
        return NexusType.countDestroyed(destroyedNexuses);
    }

    public boolean allDestroyed() {
        return getDestroyedCount() >= NexusType.values().length;
    }

    public static NexusSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NexusSavedData::load,
                NexusSavedData::new,
                DATA_NAME
        );
    }
}
