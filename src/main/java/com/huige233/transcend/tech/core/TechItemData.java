package com.huige233.transcend.tech.core;

import com.huige233.transcend.tech.ammo.Magazine;
import com.huige233.transcend.tech.attribute.AttributeContainer;
import com.huige233.transcend.tech.quality.TechQuality;
import com.huige233.transcend.tech.rank.TechRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import com.huige233.transcend.tech.gun.ModuleInstance;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

   
                 
  
                                                      
                                         
   
/** 统一读写物品科技数据子标签中的能量、热量、蓄能、品质、评级、属性、模块、弹匣和护盾簇。 */
public final class TechItemData {
    public static final String ROOT = "TechData";
    public static final int SCHEMA_VERSION = 2;
    public static final String TAG_SCHEMA = "Schema";
    public static final String TAG_UNIQUE = "Unique";
    public static final String TAG_ENERGY = "Energy";
    public static final String TAG_MAX_ENERGY = "MaxEnergy";
    public static final String TAG_HEAT = "Heat";
    
    public static final String TAG_GUN_CHARGE = "GunCharge";
    public static final String TAG_ATTRIBUTES = "Attributes";
    public static final String TAG_MAGAZINE = "Magazine";
    public static final String TAG_SHIELD_CLUSTER = "ShieldCluster";
    public static final String TAG_QUALITY = "Quality";
    public static final String TAG_RANK = "Rank";
    public static final String TAG_MODULES = "Modules";

    private TechItemData() {
    }

    
    public static CompoundTag getOrCreate(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            tag = new CompoundTag();
            stack.setTag(tag);
        }
        if (!tag.contains(ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            tag.put(ROOT, new CompoundTag());
        }
        CompoundTag root = tag.getCompound(ROOT);
        if (!root.contains(TAG_SCHEMA)) {
            root.putInt(TAG_SCHEMA, SCHEMA_VERSION);
        }
        return root;
    }

    
    public static CompoundTag get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? tag.getCompound(ROOT) : new CompoundTag();
    }

    public static int schemaVersion(ItemStack stack) {
        return get(stack).getInt(TAG_SCHEMA);
    }

    public static boolean isUnique(ItemStack stack) {
        return get(stack).contains(TAG_UNIQUE);
    }

    public static void markUnique(ItemStack stack, String sourceId) {
        getOrCreate(stack).putString(TAG_UNIQUE, sourceId == null ? "" : sourceId);
    }

    public static int getEnergy(ItemStack stack) {
        return get(stack).getInt(TAG_ENERGY);
    }

    public static int getMaxEnergy(ItemStack stack) {
        return Math.max(0, get(stack).getInt(TAG_MAX_ENERGY));
    }

    
    public static void setMaxEnergy(ItemStack stack, int maxEnergy) {
        CompoundTag root = getOrCreate(stack);
        int clampedMax = Math.max(0, maxEnergy);
        root.putInt(TAG_MAX_ENERGY, clampedMax);
        root.putInt(TAG_ENERGY, Math.min(Math.max(0, root.getInt(TAG_ENERGY)), clampedMax));
    }

    
    public static void setEnergy(ItemStack stack, int value) {
        CompoundTag root = getOrCreate(stack);
        int max = Math.max(0, root.getInt(TAG_MAX_ENERGY));
        root.putInt(TAG_ENERGY, Math.max(0, Math.min(max, value)));
    }

    public static float getHeat(ItemStack stack) {
        return Math.max(0.0F, get(stack).getFloat(TAG_HEAT));
    }

    public static void setHeat(ItemStack stack, float heat) {
        getOrCreate(stack).putFloat(TAG_HEAT, Math.max(0.0F, heat));
    }

    public static float getGunCharge(ItemStack stack) {
        return Math.max(0.0F, get(stack).getFloat(TAG_GUN_CHARGE));
    }

    public static boolean hasGunCharge(ItemStack stack) {
        return get(stack).contains(TAG_GUN_CHARGE);
    }

    public static void setGunCharge(ItemStack stack, float charge) {
        getOrCreate(stack).putFloat(TAG_GUN_CHARGE, Math.max(0.0F, charge));
    }

    public static TechQuality getQuality(ItemStack stack) {
        return TechQuality.byId(get(stack).getString(TAG_QUALITY));
    }

    public static void setQuality(ItemStack stack, TechQuality quality) {
        getOrCreate(stack).putString(TAG_QUALITY, (quality == null ? TechQuality.COMMON : quality).id());
    }

    public static TechRank getRank(ItemStack stack) {
        return TechRank.byId(get(stack).getString(TAG_RANK));
    }

    public static void setRank(ItemStack stack, TechRank rank) {
        getOrCreate(stack).putString(TAG_RANK, (rank == null ? TechRank.C : rank).id());
    }

    public static void saveAttributes(ItemStack stack, AttributeContainer attributes) {
        getOrCreate(stack).put(TAG_ATTRIBUTES, attributes.save());
    }

    public static void loadAttributes(ItemStack stack, AttributeContainer attributes) {
        attributes.load(get(stack).getCompound(TAG_ATTRIBUTES));
    }

    public static void saveModules(ItemStack stack, java.util.Collection<ModuleInstance> modules) {
        ListTag list = new ListTag();
        if (modules != null) for (ModuleInstance module : modules) { CompoundTag entry = new CompoundTag(); module.save(entry); list.add(entry); }
        getOrCreate(stack).put(TAG_MODULES, list);
    }

    public static java.util.List<ModuleInstance> loadModules(ItemStack stack) {
        java.util.List<ModuleInstance> result = new java.util.ArrayList<>();
        ListTag list = get(stack).getList(TAG_MODULES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) result.add(ModuleInstance.load(list.getCompound(i)));
        return result;
    }

    public static void saveMagazine(ItemStack stack, Magazine magazine) {
        CompoundTag root = getOrCreate(stack);
        CompoundTag data = new CompoundTag();
        magazine.save(data);
        root.put(TAG_MAGAZINE, data);
    }

    public static void loadMagazine(ItemStack stack, Magazine magazine) {
        magazine.load(get(stack).getCompound(TAG_MAGAZINE));
    }

    public static void saveShieldCluster(ItemStack stack, com.huige233.transcend.tech.shield.ShieldCluster cluster) {
        CompoundTag data = new CompoundTag();
        cluster.save(data);
        getOrCreate(stack).put(TAG_SHIELD_CLUSTER, data);
    }

    public static com.huige233.transcend.tech.shield.ShieldCluster loadShieldCluster(ItemStack stack) {
        return com.huige233.transcend.tech.shield.ShieldCluster.load(get(stack).getCompound(TAG_SHIELD_CLUSTER));
    }
}


