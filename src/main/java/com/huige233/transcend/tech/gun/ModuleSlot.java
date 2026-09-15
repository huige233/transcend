package com.huige233.transcend.tech.gun;


/** 定义枪械弹药、枪管、枪口、电池和功能模块的安装槽位及稳定标识。 */
public enum ModuleSlot {
    AMMO,
    BARREL,
    MUZZLE,
    BATTERY,
    UTILITY;

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
