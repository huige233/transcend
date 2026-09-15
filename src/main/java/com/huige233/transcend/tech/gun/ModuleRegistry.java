package com.huige233.transcend.tech.gun;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/** 按稳定插入顺序登记和查询枪械模块定义，并提供各安装槽位的默认模块。 */
public final class ModuleRegistry {
    private static final Map<String, GunModule> MODULES = new LinkedHashMap<>();

    static {
        register(new BasicModule("default_ammo", ModuleSlot.AMMO, "module.transcend.default_ammo"));
        register(new BasicModule("default_barrel", ModuleSlot.BARREL, "module.transcend.default_barrel"));
        register(new BasicModule("default_muzzle", ModuleSlot.MUZZLE, "module.transcend.default_muzzle"));
        register(new BasicModule("sirius", ModuleSlot.MUZZLE, "item.transcend.sirius_module"));
    }

    private ModuleRegistry() {
    }

    public static void register(GunModule module) {
        if (module == null || module.id() == null || module.id().isBlank()) {
            throw new IllegalArgumentException("A gun module must have a non-empty id");
        }
        MODULES.put(module.id(), module);
    }

    @Nullable
    public static GunModule byId(@Nullable String id) {
        return id == null ? null : MODULES.get(id);
    }

    public static GunModule defaultFor(ModuleSlot slot) {
        return MODULES.get("default_" + slot.id());
    }

    public static Map<String, GunModule> entries() {
        return Collections.unmodifiableMap(MODULES);
    }

    /** 以标识、槽位和翻译键实现不附加属性修饰器的基础枪械模块定义。 */
    private record BasicModule(String id, ModuleSlot slot, String translationKey) implements GunModule {
        @Override public Component displayName() { return Component.translatable(translationKey); }
    }
}
