package com.huige233.transcend.items.tech;

import com.huige233.transcend.tech.shield.ShieldCluster;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;


/** 读写相位护盾已安装模块及原物品数据，并提供容量和耗能倍率配置。 */
public final class PhaseShieldModules {
    public static final String MODULE = "shield_module";
    public static final String INSTALLED = "InstalledShieldModule";

    private PhaseShieldModules() {}

    @Nullable
    public static ShieldModule module(@Nullable CompoundTag host) {
        return host == null ? null : ShieldModule.byId(host.getString(MODULE));
    }

    public static float energyMultiplier(@Nullable CompoundTag host) {
        ShieldModule module = module(host);
        return module == null ? 1.0F : module.energyMultiplier();
    }

    public static float capacity(@Nullable CompoundTag host, float baseCapacity) {
        ShieldModule module = module(host);
        return module == null ? baseCapacity : module.capacity();
    }

    public static void configure(ShieldCluster cluster, @Nullable CompoundTag host, float baseCapacity) {
        
        if (!cluster.isEmpty()) cluster.layers().get(0).setMaxEnergy(capacity(host, baseCapacity));
    }

    public static void install(CompoundTag host, ShieldModule module, CompoundTag serializedItem) {
        host.putString(MODULE, module.id());
        host.put(INSTALLED, serializedItem.copy());
    }

    public static void remove(CompoundTag host) {
        host.remove(MODULE);
        host.remove(INSTALLED);
    }
}
