package com.huige233.transcend.ascension.resource;

import com.huige233.transcend.ascension.MageClass;
import net.minecraft.nbt.CompoundTag;

public class ClassResourceData {

    private static final String TAG_VALUE = "value";
    private static final String TAG_OVERFLOW_TICKS = "overflow_ticks";
    private static final String TAG_WINDOW_TICKS = "window_ticks";
    private static final String TAG_WINDOW_READY = "window_ready";

    private float value = 0f;

    private int overflowTicks = 0;

    private int windowTicks = 0;

    private boolean windowReady = true;

    public float getValue() { return value; }

    public float getRatio(ClassResourceType type) {
        float max = type.getMaxValue();
        return max <= 0 ? 0f : Math.min(1f, value / max);
    }

    public boolean isOverflowing() { return overflowTicks > 0; }
    public int getOverflowTicks() { return overflowTicks; }

    public boolean isWindowActive() { return windowTicks > 0; }
    public int getWindowTicks() { return windowTicks; }
    public boolean isWindowReady() { return windowReady; }

    public void add(float amount, ClassResourceType type) {
        value = ClassResourceMath.clampFinite(value + amount, type.getMaxValue());
    }

    public void subtract(float amount, ClassResourceType type) {
        value = ClassResourceMath.clampFinite(value - amount, type.getMaxValue());
    }

    public void set(float newValue, ClassResourceType type) {
        value = ClassResourceMath.clampFinite(newValue, type.getMaxValue());
    }

    public void forceSet(float newValue) {
        this.value = ClassResourceMath.clampFinite(newValue, 100.0F);
    }

    public void reset() {
        value = 0f;
        overflowTicks = 0;
        windowTicks = 0;
        windowReady = true;
    }

    public void startOverflow() {
        overflowTicks = 1;
    }

    public void tickOverflow() {
        if (overflowTicks > 0 && overflowTicks < Integer.MAX_VALUE) overflowTicks++;
    }

    public void endOverflow() {
        overflowTicks = 0;
    }

    public void startWindow(int durationTicks) {
        windowTicks = Math.max(0, durationTicks);
    }

    public void tickWindow() {
        if (windowTicks > 0) windowTicks--;
    }

    public void disarmWindow() { windowReady = false; }
    public void armWindow() { windowReady = true; }

    public boolean isAtThreshold(ClassResourceType type) {
        return value >= type.getThreshold();
    }

    public boolean isFull(ClassResourceType type) {
        return value >= type.getMaxValue();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(TAG_VALUE, value);
        tag.putInt(TAG_OVERFLOW_TICKS, overflowTicks);
        tag.putInt(TAG_WINDOW_TICKS, windowTicks);
        tag.putBoolean(TAG_WINDOW_READY, windowReady);
        return tag;
    }

    public void load(CompoundTag tag) {
        value = ClassResourceMath.clampFinite(tag.getFloat(TAG_VALUE), 100.0F);
        overflowTicks = Math.max(0, tag.getInt(TAG_OVERFLOW_TICKS));
        windowTicks = Math.max(0, tag.getInt(TAG_WINDOW_TICKS));
        windowReady = tag.contains(TAG_WINDOW_READY)
                ? tag.getBoolean(TAG_WINDOW_READY)
                : value < 60.0F;
    }

    public void copyFrom(ClassResourceData other) {
        this.value = ClassResourceMath.clampFinite(other.value, 100.0F);
        this.overflowTicks = Math.max(0, other.overflowTicks);
        this.windowTicks = Math.max(0, other.windowTicks);
        this.windowReady = other.windowReady;
    }

    public void writeToBuffer(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeFloat(value);
        buf.writeVarInt(overflowTicks);
        buf.writeVarInt(windowTicks);
        buf.writeBoolean(windowReady);
    }

    public void readFromBuffer(net.minecraft.network.FriendlyByteBuf buf) {
        value = ClassResourceMath.clampFinite(buf.readFloat(), 100.0F);
        overflowTicks = Math.max(0, buf.readVarInt());
        windowTicks = Math.max(0, buf.readVarInt());
        windowReady = buf.readBoolean();
    }
}
