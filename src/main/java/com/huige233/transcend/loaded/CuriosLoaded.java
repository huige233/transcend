package com.huige233.transcend.loaded;

import net.minecraftforge.fml.ModList;

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
