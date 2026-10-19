package com.huige233.transcend.combat;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ReentryGuardTest {
    @Test
    void suppressesSameTargetButAllowsOtherTargets() {
        var guard = new ReentryGuard<Object>(2);
        Object first = new Object(), second = new Object(), third = new Object();
        assertTrue(guard.run(first, () -> {
            assertFalse(guard.run(first, () -> fail("same target reentered")));
            assertTrue(guard.run(second, () ->
                    assertFalse(guard.run(third, () -> fail("depth limit exceeded")))));
        }));
        assertTrue(guard.run(first, () -> {}));
    }

    @Test
    void exceptionReleasesTargetAndBudget() {
        var guard = new ReentryGuard<Object>(1);
        Object target = new Object();
        assertThrows(IllegalStateException.class,
                () -> guard.run(target, () -> { throw new IllegalStateException("callback failed"); }));
        assertTrue(guard.run(target, () -> {}));
    }

    @Test
    void distinguishesEqualObjectsByIdentity() {
        var guard = new ReentryGuard<String>(2);
        String first = new String("equal"), second = new String("equal");
        assertTrue(guard.run(first, () -> assertTrue(guard.run(second, () -> {}))));
    }

    @Test
    void doesNotBlockAnotherThread() {
        var guard = new ReentryGuard<Object>(1);
        Object target = new Object();
        assertTrue(guard.run(target, () ->
                assertTrue(CompletableFuture.supplyAsync(() -> guard.run(target, () -> {})).join())));
    }
}
