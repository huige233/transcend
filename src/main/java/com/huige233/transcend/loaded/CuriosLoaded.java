package com.huige233.transcend.loaded;

import net.minecraftforge.fml.ModList;

/** Curios 模组加载探测枚举。 */
public enum CuriosLoaded {
    CURIOS("curios");
    private final boolean loaded;

    CuriosLoaded(String modid) {
        this.loaded = ModList.get() != null && ModList.get().getModContainerById(modid).isPresent();
    }

    public boolean isLoaded() {
        return this.loaded;
    }

}
