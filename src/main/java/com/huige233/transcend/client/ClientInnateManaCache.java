package com.huige233.transcend.client;

/** 客户端先天魔力数据缓存。 */
public final class ClientInnateManaCache {

    private static int currentMana = 0;

    private static float absorbPerSec = 0f;

    private static long lastUpdateTick = -1L;

    private ClientInnateManaCache() {}

    public static int getCurrentMana() {
        return currentMana;
    }

    public static float getAbsorbPerSec() {
        return absorbPerSec;
    }

    public static long getLastUpdateTick() {
        return lastUpdateTick;
    }

    public static void update(int value, float absorb) {
        currentMana = Math.max(0, value);
        absorbPerSec = Math.max(0f, absorb);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) {
            lastUpdateTick = mc.level.getGameTime();
        }
    }

    public static void update(int value) {
        update(value, absorbPerSec);
    }

    public static void clear() {
        currentMana = 0;
        absorbPerSec = 0f;
        lastUpdateTick = -1L;
    }
}
