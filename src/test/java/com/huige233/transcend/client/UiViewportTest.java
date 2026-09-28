package com.huige233.transcend.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiViewportTest {
    @Test void leavesNormalWindowsAtNativeScale() {
        UiViewport view = UiViewport.fit(960, 540, 436, 288);
        assertEquals(1.0, view.scale());
        assertEquals(960, view.width());
        assertEquals(540, view.height());
    }

    @Test void researchAndDummyLayoutsFitSmallWindows() {
        for (int[] layout : new int[][] {{488, 340}, {300, 294}, {292, 400}, {400, 320}}) {
            UiViewport view = UiViewport.fit(320, 240, layout[0], layout[1]);
            assertTrue(view.width() >= layout[0]);
            assertTrue(view.height() >= layout[1]);
            assertTrue(layout[0] * view.scale() <= 320.000001);
            assertTrue(layout[1] * view.scale() <= 240.000001);
        }
    }

    @Test void scaledSlotAndDragCoordinatesRoundTrip() {
        UiViewport view = UiViewport.fit(320, 240, 436, 288);
        // Center of a research inventory slot, including the centered panel offset.
        double slotCenter = (view.width() - 420) / 2 + 16 + 8;
        assertEquals(slotCenter, view.logical(slotCenter * view.scale()), 0.000001);
        assertEquals(18, view.logical(18 * view.scale()), 0.000001);
        assertEquals(-18, view.logical(-18 * view.scale()), 0.000001);
    }

    @Test void pointerEdgesUseFloorRatherThanTruncation() {
        UiViewport view = UiViewport.fit(320, 240, 292, 400);
        assertEquals(-2, view.pixel(-1));
        assertEquals(0, view.pixel(0));
        assertTrue(Double.isFinite(UiViewport.fit(0, 0, 292, 400).scale()));
    }
}
