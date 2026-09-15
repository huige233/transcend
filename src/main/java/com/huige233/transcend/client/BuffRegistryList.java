package com.huige233.transcend.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;

   
                                       
                                    
   
/** 懒加载并缓存按本地化名称排序的已注册状态效果，供假人设置界面选择。 */
public final class BuffRegistryList {

    
    /** 保存可选状态效果的注册标识与本地化名称，并生成列表显示文本。 */
    public record BuffEntry(ResourceLocation id, String localizedName) {
        public String display() {
            return localizedName + " §8(" + id + ")";
        }
    }

    private static List<BuffEntry> cached;

    private BuffRegistryList() {
    }

    
    public static synchronized List<BuffEntry> all() {
        if (cached != null) return cached;
        List<BuffEntry> list = new ArrayList<>();
        for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
            MobEffect effect = entry.getValue();
            ResourceLocation id = entry.getKey().location();
            String name;
            try {
                name = effect.getDisplayName().getString();
            } catch (Exception e) {
                name = id.getPath();
            }
            list.add(new BuffEntry(id, name));
        }
        list.sort((a, b) -> a.localizedName.compareToIgnoreCase(b.localizedName));
        cached = List.copyOf(list);
        return cached;
    }
}
