package com.huige233.transcend.client.compat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OculusCompatTest {
    @Test
    void absentApiFallsBackWithoutRepeatedLookupOrWarning() {
        AtomicInteger lookups = new AtomicInteger();
        List<Throwable> warnings = new ArrayList<>();
        var query = new OculusCompat.ShadowPassQuery(name -> {
            assertEquals("net.irisshaders.iris.api.v0.IrisApi", name);
            lookups.incrementAndGet();
            throw new ClassNotFoundException(name);
        }, warnings::add);

        assertFalse(query.isRenderingShadowPass());
        assertFalse(query.isRenderingShadowPass());
        assertEquals(1, lookups.get());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void cachedApiReadsCurrentPassAfterShaderToggle() {
        AtomicInteger lookups = new AtomicInteger();
        List<Throwable> warnings = new ArrayList<>();
        DynamicApi.INSTANCE = new DynamicApi();
        var query = new OculusCompat.ShadowPassQuery(name -> {
            lookups.incrementAndGet();
            return DynamicApi.class;
        }, warnings::add);

        assertFalse(query.isRenderingShadowPass());
        DynamicApi.INSTANCE.shadow = true;
        assertTrue(query.isRenderingShadowPass());
        DynamicApi.INSTANCE.shadow = false;
        assertFalse(query.isRenderingShadowPass());
        assertEquals(1, lookups.get());
        assertEquals(3, DynamicApi.INSTANCE.calls);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void incompatibleApiFallsBackAndReportsOnce() {
        List<Throwable> warnings = new ArrayList<>();
        var query = new OculusCompat.ShadowPassQuery(name -> WrongSignatureApi.class, warnings::add);

        assertFalse(query.isRenderingShadowPass());
        assertFalse(query.isRenderingShadowPass());
        assertEquals(1, warnings.size());
        assertInstanceOf(NoSuchMethodException.class, warnings.get(0));
    }

    @Test
    void failingApiIsDisabledWithoutPerFrameExceptions() {
        List<Throwable> warnings = new ArrayList<>();
        FailingApi.INSTANCE.calls = 0;
        var query = new OculusCompat.ShadowPassQuery(name -> FailingApi.class, warnings::add);

        assertFalse(query.isRenderingShadowPass());
        assertFalse(query.isRenderingShadowPass());
        assertEquals(1, FailingApi.INSTANCE.calls);
        assertEquals(1, warnings.size());
    }

    @Test
    void brokenOptionalDependencyDoesNotBreakRenderer() {
        List<Throwable> warnings = new ArrayList<>();
        var query = new OculusCompat.ShadowPassQuery(name -> {
            throw new NoClassDefFoundError("incompatible Oculus dependency");
        }, warnings::add);

        assertFalse(query.isRenderingShadowPass());
        assertFalse(query.isRenderingShadowPass());
        assertEquals(1, warnings.size());
    }

    public static final class DynamicApi {
        static DynamicApi INSTANCE = new DynamicApi();
        boolean shadow;
        int calls;

        public static DynamicApi getInstance() { return INSTANCE; }
        public boolean isRenderingShadowPass() { calls++; return shadow; }
    }

    public static final class WrongSignatureApi {
        public static WrongSignatureApi getInstance() { return new WrongSignatureApi(); }
        public String isRenderingShadowPass() { return "false"; }
    }

    public static final class FailingApi {
        static final FailingApi INSTANCE = new FailingApi();
        int calls;

        public static FailingApi getInstance() { return INSTANCE; }
        public boolean isRenderingShadowPass() {
            calls++;
            throw new IllegalStateException("shader renderer is unavailable");
        }
    }
}
