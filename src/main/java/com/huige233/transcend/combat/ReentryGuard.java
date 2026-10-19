package com.huige233.transcend.combat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** 限制同一线程的嵌套操作，并禁止同一对象在回调中重复进入。 */
public final class ReentryGuard<T> {
    private static final ThreadLocal<Set<Object>> GLOBAL_ACTIVE = new ThreadLocal<>();
    private final ThreadLocal<Set<T>> active = new ThreadLocal<>();
    private final int limit;

    public ReentryGuard(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        this.limit = limit;
    }

    /** 用于无法方便包裹整段生命周期清理代码的低层入口。 */
    public static boolean tryEnter(Object target) {
        Set<Object> targets = GLOBAL_ACTIVE.get();
        if (targets == null) {
            targets = Collections.newSetFromMap(new IdentityHashMap<>());
            GLOBAL_ACTIVE.set(targets);
        }
        if (!targets.add(target)) return false;
        return true;
    }

    public static void exit(Object target) {
        Set<Object> targets = GLOBAL_ACTIVE.get();
        if (targets == null) return;
        targets.remove(target);
        if (targets.isEmpty()) GLOBAL_ACTIVE.remove();
    }

    public boolean isActive(T target) {
        Set<T> targets = active.get();
        return targets != null && targets.contains(target);
    }

    public boolean run(T target, Runnable action) {
        Set<T> targets = active.get();
        if (targets == null) {
            targets = Collections.newSetFromMap(new IdentityHashMap<>());
            active.set(targets);
        }
        if (targets.size() >= limit || !targets.add(target)) return false;
        try {
            action.run();
            return true;
        } finally {
            targets.remove(target);
            if (targets.isEmpty()) active.remove();
        }
    }
}
