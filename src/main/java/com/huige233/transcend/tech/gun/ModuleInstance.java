package com.huige233.transcend.tech.gun;

import com.huige233.transcend.tech.attribute.TechAttrModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/** 持久化已安装模块的标识与临时属性修饰器，并通过注册表解析模块定义。 */
public final class ModuleInstance {
    private static final String ID = "Id";
    private static final String MODIFIERS = "Modifiers";

    private final String moduleId;
    private final List<TechAttrModifier> temporaryModifiers;

    public ModuleInstance(String moduleId) {
        this(moduleId, List.of());
    }

    public ModuleInstance(String moduleId, List<TechAttrModifier> temporaryModifiers) {
        this.moduleId = moduleId == null ? "" : moduleId;
        this.temporaryModifiers = new ArrayList<>(temporaryModifiers == null ? List.of() : temporaryModifiers);
    }

    public String moduleId() { return moduleId; }
    public List<TechAttrModifier> temporaryModifiers() { return List.copyOf(temporaryModifiers); }

    public void save(CompoundTag tag) {
        tag.putString(ID, moduleId);
        
        CompoundTag attrs = new CompoundTag();
        ListTag values = new ListTag();
        for (TechAttrModifier modifier : temporaryModifiers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Uuid", modifier.uuid());
            entry.putString("Name", modifier.name());
            entry.putDouble("Amount", modifier.amount());
            entry.putString("Op", modifier.op().name());
            entry.putLong("Expire", modifier.expiresAtTick());
            values.add(entry);
        }
        attrs.put("Values", values);
        tag.put(MODIFIERS, attrs);
    }

    public static ModuleInstance load(CompoundTag tag) {
        String id = tag.getString(ID);
        List<TechAttrModifier> modifiers = new ArrayList<>();
        CompoundTag data = tag.getCompound(MODIFIERS);
        ListTag values = data.getList("Values", Tag.TAG_COMPOUND);
        for (int i = 0; i < values.size(); i++) {
            CompoundTag entry = values.getCompound(i);
            try {
                modifiers.add(new TechAttrModifier(entry.getUUID("Uuid"), entry.getString("Name"),
                        entry.getDouble("Amount"), com.huige233.transcend.tech.attribute.ModifierOp.valueOf(entry.getString("Op")),
                        entry.getLong("Expire")));
            } catch (RuntimeException ignored) {
                
            }
        }
        return new ModuleInstance(id, modifiers);
    }

    @Nullable
    public GunModule resolve() {
        return ModuleRegistry.byId(moduleId);
    }
}
