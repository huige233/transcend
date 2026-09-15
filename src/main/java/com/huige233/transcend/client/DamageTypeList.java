package com.huige233.transcend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import java.util.ArrayList;
import java.util.List;

   
                                                 
                     
                                                                     
   
/** 从客户端世界注册表缓存按标识排序的伤害类型，并提供索引查找与缓存清理。 */
public final class DamageTypeList {

    
    /** 保存可选伤害类型的注册标识和死亡消息标识，并以注册标识作为显示文本。 */
    public record DamageEntry(ResourceLocation id, String msgId) {
        public String display() {
            return id.toString();
        }
    }

    private static List<DamageEntry> cached;

    private DamageTypeList() {
    }

    
    public static synchronized List<DamageEntry> all() {
        if (cached != null) return cached;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return List.of();
        List<DamageEntry> list = new ArrayList<>();
        var registry = mc.level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            list.add(new DamageEntry(id, entry.getValue().msgId()));
        }
        list.sort((a, b) -> a.id().toString().compareToIgnoreCase(b.id().toString()));
        cached = List.copyOf(list);
        return cached;
    }

    
    public static int indexOf(String id) {
        List<DamageEntry> entries = all();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().toString().equals(id)) return i;
        }
        return -1;
    }

    
    public static void invalidate() {
        cached = null;
    }
}
