package com.huige233.transcend.ascension.resource;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassResourceDataTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void loadSanitizesResourceAndTimers() {
        CompoundTag corrupt = new CompoundTag();
        corrupt.putFloat("value", Float.NaN);
        corrupt.putInt("overflow_ticks", -20);
        corrupt.putInt("window_ticks", -5);

        ClassResourceData data = new ClassResourceData();
        data.load(corrupt);

        assertEquals(0.0F, data.getValue(), EPSILON);
        assertEquals(0, data.getOverflowTicks());
        assertEquals(0, data.getWindowTicks());
        assertTrue(data.isWindowReady());

        corrupt.putFloat("value", 140.0F);
        data.load(corrupt);
        assertEquals(100.0F, data.getValue(), EPSILON);
        assertFalse(data.isWindowReady());
    }

    @Test
    void typeOperationsStayFiniteAndRespectTypeMaximum() {
        ClassResourceData data = new ClassResourceData();
        data.add(Float.POSITIVE_INFINITY, ClassResourceType.TEMPO);
        assertEquals(0.0F, data.getValue(), EPSILON);

        data.set(100.0F, ClassResourceType.SEDIMENT);
        assertEquals(8.0F, data.getValue(), EPSILON);
        data.subtract(Float.NaN, ClassResourceType.SEDIMENT);
        assertEquals(0.0F, data.getValue(), EPSILON);
    }

    @Test
    void windowHysteresisStatePersistsThroughSaveAndSyncBuffer() {
        ClassResourceData source = new ClassResourceData();
        source.set(80.0F, ClassResourceType.HEAT);
        source.startWindow(100);
        source.disarmWindow();

        ClassResourceData loaded = new ClassResourceData();
        loaded.load(source.save());
        assertEquals(100, loaded.getWindowTicks());
        assertFalse(loaded.isWindowReady());

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        source.writeToBuffer(buffer);
        ClassResourceData synced = new ClassResourceData();
        synced.readFromBuffer(buffer);
        assertEquals(80.0F, synced.getValue(), EPSILON);
        assertEquals(100, synced.getWindowTicks());
        assertFalse(synced.isWindowReady());
    }
}
