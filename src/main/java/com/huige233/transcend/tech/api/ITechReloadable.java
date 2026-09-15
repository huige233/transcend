package com.huige233.transcend.tech.api;

import com.huige233.transcend.tech.ammo.AmmoType;
import com.huige233.transcend.tech.ammo.Magazine;
import net.minecraft.world.entity.player.Player;


/** 约定可装填科技设备的弹匣访问和指定弹种装填入口。 */
public interface ITechReloadable {
    Magazine magazine();
    boolean reload(Player player, AmmoType ammo);
}
