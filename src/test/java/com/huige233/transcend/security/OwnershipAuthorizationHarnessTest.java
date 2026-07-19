package com.huige233.transcend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.entity.TestDummy;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class OwnershipAuthorizationHarnessTest {
    private static final UUID OWNER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID INTRUDER = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    void productionTestDummyCodecLoadsLegacyClaimsSavesAndReloads() {
        UUID legacyOwner = ProductionOwnershipTestAccess.loadDummyOwner(new CompoundTag());
        assertNull(legacyOwner, "old TestDummy NBT must remain ownerless");
        UUID claimedOwner = ProductionOwnershipTestAccess.claimDummyOwner(legacyOwner, OWNER, false);
        assertEquals(OWNER, claimedOwner, "first legitimate server sender must claim legacy state");

        CompoundTag saved = new CompoundTag();
        ProductionOwnershipTestAccess.saveDummyOwner(saved, claimedOwner);
        assertEquals(OWNER, saved.getUUID("Owner"));
        assertEquals(OWNER, ProductionOwnershipTestAccess.loadDummyOwner(saved),
                "production TestDummy codec must survive save/reload");
    }

    @Test
    void actualCircleOwnerAllowedIntruderDeniedAndAdminBypassesWithoutClaim() {
        MagicCircleCoreBlockEntity circle = ownerlessCircle();
        assertTrue(ProductionOwnershipTestAccess.authorize(circle, ADMIN, true));
        assertNull(circle.getOwner(), "admin bypass must not silently claim legacy state");
        assertTrue(ProductionOwnershipTestAccess.authorize(circle, OWNER, false));
        assertEquals(OWNER, circle.getOwner());
        assertTrue(ProductionOwnershipTestAccess.authorize(circle, OWNER, false));
        assertFalse(ProductionOwnershipTestAccess.authorize(circle, INTRUDER, false));
    }

    @Test
    void concurrentActualCircleFirstClaimHasExactlyOneWinner() throws Exception {
        MagicCircleCoreBlockEntity circle = ownerlessCircle();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Set<UUID> winners = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> ownerAttempt = executor.submit(() -> claim(circle, OWNER, ready, start, winners));
            Future<?> intruderAttempt = executor.submit(() -> claim(circle, INTRUDER, ready, start, winners));
            ready.await();
            start.countDown();
            ownerAttempt.get();
            intruderAttempt.get();
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, winners.size(), "atomic first claim must return success to exactly one contender");
        assertTrue(winners.contains(circle.getOwner()), "stored owner must be the sole successful claimant");
    }

    private static void claim(MagicCircleCoreBlockEntity circle, UUID sender, CountDownLatch ready,
                              CountDownLatch start, Set<UUID> winners) {
        try {
            ready.countDown();
            start.await();
            if (ProductionOwnershipTestAccess.authorize(circle, sender, false)) {
                winners.add(sender);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("claim contender interrupted", exception);
        }
    }

    private static MagicCircleCoreBlockEntity ownerlessCircle() {
        return ProductionOwnershipTestAccess.allocate(MagicCircleCoreBlockEntity.class);
    }

}
