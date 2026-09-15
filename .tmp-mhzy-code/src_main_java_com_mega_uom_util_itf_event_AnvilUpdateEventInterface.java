package com.mega.uom.util.itf.event;

import net.minecraft.world.inventory.AnvilMenu;

public interface AnvilUpdateEventInterface {
    default AnvilMenu uom$getMenu() {
        return null;
    }

    default void uom$setMenu(AnvilMenu menu) {

    }
}
