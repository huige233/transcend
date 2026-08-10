package com.huige233.transcend.gear.forge;

/** 祝福定义记录。 */
public record BlessingDef(
        String id,
        String langSubKey,
        CelestialKind dominant,
        CelestialKind accent,
        int color
) {
    public boolean isPure() { return dominant == accent; }
    public String nameKey() { return "blessing.transcend." + langSubKey + ".name"; }
}
