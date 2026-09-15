package com.huige233.transcend.loaded;

import net.minecraftforge.fml.ModList;


/** 缓存 Curios 模组的加载状态，供饰品功能调用前检查依赖是否可用。 */
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
