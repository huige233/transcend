package com.huige233.transcend.tech.assembly;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;


/** 记录制造任务所有者、配方签名、剩余批量和逐刻进度，并校验任务存档。 */
public final class AssemblyJob {
    public static final int MAX_BATCH = 64;
    public static final int MAX_DURATION = 32_000;
    private final UUID owner;
    private final ResourceLocation recipe;
    private final String signature;
    private final int duration;
    private int remaining;
    private int progress;

    public AssemblyJob(UUID owner, ResourceLocation recipe, String signature, int count, int duration) {
        if (owner == null || recipe == null || signature == null || count < 1 || count > MAX_BATCH
                || duration < 1 || duration > MAX_DURATION) throw new IllegalArgumentException("Invalid assembly job");
        this.owner = owner;
        this.recipe = recipe;
        this.signature = signature;
        this.remaining = count;
        this.duration = duration;
    }

    public UUID owner() { return owner; }
    public ResourceLocation recipe() { return recipe; }
    public String signature() { return signature; }
    public int duration() { return duration; }
    public int remaining() { return remaining; }
    public int progress() { return progress; }

    public boolean advance() {
        if (remaining <= 0) return false;
        progress = Math.min(duration, progress + 1);
        return progress == duration;
    }

    public void completeOne() {
        if (remaining <= 0 || progress != duration) throw new IllegalStateException("Assembly job is not ready");
        remaining--;
        progress = 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", owner);
        tag.putString("Recipe", recipe.toString());
        tag.putString("Signature", signature);
        tag.putInt("Duration", duration);
        tag.putInt("Remaining", remaining);
        tag.putInt("Progress", progress);
        return tag;
    }

    public static AssemblyJob load(CompoundTag tag) {
        ResourceLocation recipe = ResourceLocation.tryParse(tag.getString("Recipe"));
        int count = tag.getInt("Remaining");
        int duration = tag.getInt("Duration");
        if (!tag.hasUUID("Owner") || recipe == null || count < 1 || count > MAX_BATCH
                || duration < 1 || duration > MAX_DURATION || !tag.contains("Signature", 8)) return null;
        AssemblyJob job = new AssemblyJob(tag.getUUID("Owner"), recipe, tag.getString("Signature"), count, duration);
        job.progress = Math.max(0, Math.min(duration, tag.getInt("Progress")));
        return job;
    }
}
