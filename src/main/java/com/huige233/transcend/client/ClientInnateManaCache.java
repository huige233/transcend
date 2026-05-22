package com.huige233.transcend.client;

/**
 * 客户端镜像缓存 — 内在 mana 当前值。
 *
 * <p>原理: {@code player.getPersistentData()} 存的 NBT 字段不会自动同步到客户端，
 * 因此 HUD 必须依赖服务端通过 {@link com.huige233.transcend.network.S2CInnateManaSync}
 * 主动推送的值。本类提供单例缓存，HUD 直接读取。
 *
 * <p>客户端 - server 是 1:1 的本地玩家关系，所以只需一个 int 槽位即可，
 * 不需要 UUID -> value 映射。
 */
public final class ClientInnateManaCache {

    private static int currentMana = 0;
    /** 服务端实测的最近 1 秒环境吸收速率（mana/s） — 用于 HUD 显示蓝色环境抽取量 */
    private static float absorbPerSec = 0f;
    /** 上一次同步包到达的 game time（client tick）— 用于检测掉线/未同步 */
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

    /** 主入口：服务端推送时调用。同时更新 mana 池和吸收速率。 */
    public static void update(int value, float absorb) {
        currentMana = Math.max(0, value);
        absorbPerSec = Math.max(0f, absorb);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) {
            lastUpdateTick = mc.level.getGameTime();
        }
    }

    /** 兼容旧调用点 */
    public static void update(int value) {
        update(value, absorbPerSec);
    }

    public static void clear() {
        currentMana = 0;
        absorbPerSec = 0f;
        lastUpdateTick = -1L;
    }
}
