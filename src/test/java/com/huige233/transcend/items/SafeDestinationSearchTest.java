package com.huige233.transcend.items;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeDestinationSearchTest {
    @Test
    void selectsIntendedDestinationBeforeFallbacks() {
        var result = SafeDestinationSearch.find(10, 64, 20, 2,
                candidate -> candidate.equals(new SafeDestinationSearch.GridPos(10, 64, 20)));
        assertEquals(new SafeDestinationSearch.GridPos(10, 64, 20), result.orElseThrow());
    }

    @Test
    void findsOnlyBoundedSafeFallbackAndCanRejectAll() {
        var safe = new SafeDestinationSearch.GridPos(11, 64, 20);
        assertEquals(safe, SafeDestinationSearch.find(10, 64, 20, 2, safe::equals).orElseThrow());
        assertTrue(SafeDestinationSearch.find(10, 64, 20, 2, Set.of()::contains).isEmpty());
        assertTrue(SafeDestinationSearch.find(10, 64, 20, 99,
                candidate -> candidate.x() == 15).isEmpty());
    }
}
