package com.huige233.transcend.tech.assembly;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 验证装配任务逐件结算、进度存档、畸形数据拒绝和事务提交前的完成状态锁定。 */
class AssemblyJobTest {
    private AssemblyJob job(int count, int duration) {
        return new AssemblyJob(UUID.randomUUID(), ResourceLocation.tryParse("transcend:test"), "recipe-v1", count, duration);
    }

    @Test
    void settlesOnlyOneUnitAtCompletion() {
        AssemblyJob job = job(2, 3);
        assertThrows(IllegalStateException.class, job::completeOne);
        assertFalse(job.advance());
        assertFalse(job.advance());
        assertTrue(job.advance());
        assertEquals(2, job.remaining());
        job.completeOne();
        assertEquals(1, job.remaining());
        assertEquals(0, job.progress());
        assertThrows(IllegalStateException.class, job::completeOne);
        job.advance();
        job.advance();
        job.advance();
        job.completeOne();
        assertFalse(job.advance());
        assertEquals(0, job.remaining());
    }

    @Test
    void reloadPreservesProgressWithoutOfflineCatchup() {
        AssemblyJob before = job(64, 280);
        for (int i = 0; i < 37; i++) before.advance();
        AssemblyJob loaded = AssemblyJob.load(before.save());
        assertNotNull(loaded);
        assertEquals(before.owner(), loaded.owner());
        assertEquals(before.recipe(), loaded.recipe());
        assertEquals("recipe-v1", loaded.signature());
        assertEquals(64, loaded.remaining());
        assertEquals(37, loaded.progress());
        assertEquals(280, loaded.duration());
        assertFalse(loaded.advance());
        assertEquals(38, loaded.progress());
    }

    @Test
    void refusesMalformedTasksAndBoundsProgress() {
        assertNull(AssemblyJob.load(new CompoundTag()));
        CompoundTag tag = job(1, 40).save();
        tag.putInt("Remaining", 65);
        assertNull(AssemblyJob.load(tag));
        tag.putInt("Remaining", 1);
        tag.putInt("Duration", Integer.MAX_VALUE);
        assertNull(AssemblyJob.load(tag));
        tag.putInt("Duration", 40);
        tag.putInt("Progress", Integer.MAX_VALUE);
        assertEquals(40, AssemblyJob.load(tag).progress());
        tag.putInt("Progress", -1);
        assertEquals(0, AssemblyJob.load(tag).progress());
        tag.putString("Recipe", "INVALID ID");
        assertNull(AssemblyJob.load(tag));
    }

    @Test
    void clampsAtReadyUntilTheTransactionCommits() {
        AssemblyJob job = job(1, 1);
        assertTrue(job.advance());
        assertTrue(job.advance());
        assertEquals(1, job.progress());
        assertEquals(1, job.remaining());
        job.completeOne();
        assertNull(AssemblyJob.load(job.save()));
    }
}
