package com.huige233.transcend.tech.attribute;

import com.huige233.transcend.tech.TechConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

   
                                                         
  
                                                   
                                       
                                                   
                                                           
  
                                                
   
/** 管理按属性分组的修饰器，缓存先加后乘的计算结果并处理过期清理和 NBT 存取。 */
public final class AttributeContainer {

    
    private final Map<TechAttribute, List<TechAttrModifier>> modifiers = new HashMap<>();
    
    private final Map<TechAttribute, Double> cache = new HashMap<>();

    

    
    public double getValue(TechAttribute attr) {
        Double cached = cache.get(attr);
        if (cached != null) return cached;

        double value = TechConfig.attributeBase(attr);
        List<TechAttrModifier> mods = modifiers.get(attr);
        if (mods != null && !mods.isEmpty()) {
            double add = 0.0;
            double mult = 1.0;
            for (TechAttrModifier mod : mods) {
                if (mod.op() == ModifierOp.ADD) add += mod.amount();
                else mult *= mod.amount();
            }
            value = (value + add) * mult;
        }
        cache.put(attr, value);
        return value;
    }

    
    public double getCached(TechAttribute attr) {
        return getValue(attr);
    }

    

    public void addModifier(TechAttribute attr, TechAttrModifier mod) {
        removeModifier(attr, mod.uuid());             
        modifiers.computeIfAbsent(attr, k -> new ArrayList<>()).add(mod);
        invalidate();
    }

    
    public boolean removeModifier(TechAttribute attr, UUID uuid) {
        List<TechAttrModifier> mods = modifiers.get(attr);
        if (mods == null) return false;
        boolean removed = mods.removeIf(m -> m.uuid().equals(uuid));
        if (removed) invalidate();
        return removed;
    }

    
    public boolean removeModifier(UUID uuid) {
        boolean any = false;
        for (List<TechAttrModifier> mods : modifiers.values()) {
            any |= mods.removeIf(m -> m.uuid().equals(uuid));
        }
        if (any) invalidate();
        return any;
    }

       
                 
      
                                                        
       
    public boolean tickCleanup(long currentTick) {
        boolean any = false;
        for (List<TechAttrModifier> mods : modifiers.values()) {
            any |= mods.removeIf(m -> m.isExpired(currentTick));
        }
        if (any) invalidate();
        return any;
    }

    public void invalidate() {
        cache.clear();
    }

    
    public boolean hasModifiers(TechAttribute attr) {
        List<TechAttrModifier> mods = modifiers.get(attr);
        return mods != null && !mods.isEmpty();
    }

    
    public List<TechAttrModifier> getModifiers(TechAttribute attr) {
        List<TechAttrModifier> mods = modifiers.get(attr);
        return mods == null ? List.of() : List.copyOf(mods);
    }

    

    private static final String TAG_ATTR = "Attr";
    private static final String TAG_LIST = "Mods";

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        ListTag attrList = new ListTag();
        for (Map.Entry<TechAttribute, List<TechAttrModifier>> e : modifiers.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString(TAG_ATTR, e.getKey().name());
            ListTag modList = new ListTag();
            for (TechAttrModifier m : e.getValue()) {
                CompoundTag mt = new CompoundTag();
                mt.putUUID("Id", m.uuid());
                mt.putString("Name", m.name());
                mt.putDouble("Amount", m.amount());
                mt.putString("Op", m.op().name());
                mt.putLong("Expire", m.expiresAtTick());
                modList.add(mt);
            }
            entry.put(TAG_LIST, modList);
            attrList.add(entry);
        }
        root.put("Attributes", attrList);
        return root;
    }

    public void load(CompoundTag root) {
        modifiers.clear();
        invalidate();
        ListTag attrList = root.getList("Attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attrList.size(); i++) {
            CompoundTag entry = attrList.getCompound(i);
            TechAttribute attr = byName(entry.getString(TAG_ATTR));
            if (attr == null) continue;
            ListTag modList = entry.getList(TAG_LIST, Tag.TAG_COMPOUND);
            List<TechAttrModifier> mods = new ArrayList<>(modList.size());
            for (int j = 0; j < modList.size(); j++) {
                CompoundTag mt = modList.getCompound(j);
                try {
                    mods.add(new TechAttrModifier(
                            mt.getUUID("Id"),
                            mt.getString("Name"),
                            mt.getDouble("Amount"),
                            ModifierOp.valueOf(mt.getString("Op")),
                            mt.getLong("Expire")));
                } catch (Exception ignored) {
                    
                }
            }
            if (!mods.isEmpty()) modifiers.put(attr, mods);
        }
    }

    private static TechAttribute byName(String name) {
        try {
            return TechAttribute.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
