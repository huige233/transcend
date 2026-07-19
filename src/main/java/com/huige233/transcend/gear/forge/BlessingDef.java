package com.huige233.transcend.gear.forge;

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
