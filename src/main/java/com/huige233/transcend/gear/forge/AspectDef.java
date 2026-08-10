package com.huige233.transcend.gear.forge;

/** 专属特性定义记录。 */
public record AspectDef(
        String id,
        String langSubKey,
        AspectKind dominant,
        AspectKind accent,
        float offset,
        int color
) {

    public boolean isPure() {
        return dominant == accent;
    }

    public String nameKey() {
        return "aspect.transcend." + langSubKey + ".name";
    }
}
