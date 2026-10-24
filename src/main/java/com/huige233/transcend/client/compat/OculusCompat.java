package com.huige233.transcend.client.compat;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

/** Optional, client-only access to the public Iris API shared by Oculus releases. */
public final class OculusCompat {
    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";

    private OculusCompat() { }

    public static boolean isRenderingShadowPass() {
        return Holder.QUERY.isRenderingShadowPass();
    }

    private static final class Holder {
        private static final ShadowPassQuery QUERY = new ShadowPassQuery(
                name -> Class.forName(name, false, OculusCompat.class.getClassLoader()),
                failure -> LoggerFactory.getLogger(OculusCompat.class).warn(
                        "Oculus shadow-pass detection is unavailable; rendering without the optional shadow guard.", failure));
    }

    @FunctionalInterface
    interface ApiResolver {
        Class<?> resolve(String name) throws ClassNotFoundException;
    }

    /** Resolves once, but queries the current pass on every draw, including shader-pack toggles. */
    static final class ShadowPassQuery {
        private final Consumer<Throwable> reportFailure;
        private Object api;
        private Method shadowPass;
        private boolean failed;

        ShadowPassQuery(ApiResolver resolver, Consumer<Throwable> reportFailure) {
            this.reportFailure = reportFailure;
            try {
                Class<?> apiClass = resolver.resolve(IRIS_API);
                Method getInstance = apiClass.getMethod("getInstance");
                Method query = apiClass.getMethod("isRenderingShadowPass");
                if (!Modifier.isStatic(getInstance.getModifiers()) || Modifier.isStatic(query.getModifiers())
                        || query.getReturnType() != boolean.class) {
                    throw new NoSuchMethodException("Unsupported Iris shadow-pass API signature");
                }
                Object instance = getInstance.invoke(null);
                if (!apiClass.isInstance(instance)) {
                    throw new IllegalStateException("Iris API returned no compatible instance");
                }
                api = instance;
                shadowPass = query;
            } catch (ClassNotFoundException absent) {
                // Oculus is optional. Its absence is normal and needs no log or repeated lookup.
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                disable(failure);
            }
        }

        boolean isRenderingShadowPass() {
            if (shadowPass == null) return false;
            try {
                return (boolean) shadowPass.invoke(api);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                disable(failure);
                return false;
            }
        }

        private void disable(Throwable failure) {
            api = null;
            shadowPass = null;
            if (!failed) {
                failed = true;
                reportFailure.accept(failure);
            }
        }
    }
}
