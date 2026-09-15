package com.huige233.transcend.items.tech;

import net.minecraft.nbt.CompoundTag;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.gun.ModuleInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

   
                             
                                                   
   
/** 读写粒子枪三槽模块配置，兼容旧版数据并计算伤害、射速、弹速和瞄准功能。 */
public final class GunConfig {

    public static final String NBT_AMMO = "GunAmmo";
    public static final String NBT_BARREL = "GunBarrel";
    public static final String NBT_MUZZLE = "GunMuzzle";

    private GunConfig() {
    }

    

    public static GunModule getAmmo(ItemStack gun) {
        GunModule module = read(gun, NBT_AMMO, GunModule.Slot.AMMO);
        return module == null ? GunModule.AMMO_STD : module;
    }

    public static GunModule getBarrel(ItemStack gun) {
        GunModule module = read(gun, NBT_BARREL, GunModule.Slot.BARREL);
        return module == null ? GunModule.BARREL_STD : module;
    }

    
    public static GunModule getMuzzle(ItemStack gun) {
        GunModule m = read(gun, NBT_MUZZLE, GunModule.Slot.MUZZLE);
        return m == null ? GunModule.MUZZLE_NONE : m;
    }

    @Nullable
    private static GunModule read(ItemStack gun, String key, GunModule.Slot slot) {
        CompoundTag tag = gun.getTag();
        String id = tag == null ? null : tag.getString(key);
        GunModule module = GunModule.byId(id);
        if (module != null && module.slot == slot) return module;
        
        for (ModuleInstance instance : TechItemData.loadModules(gun)) {
            GunModule legacy = GunModule.byId(instance.moduleId());
            if (legacy != null && legacy.slot == slot) return legacy;
        }
        return null;
    }

    

    public static void set(ItemStack gun, GunModule.Slot slot, @Nullable GunModule module) {
        if (module != null && module.slot != slot) {
            throw new IllegalArgumentException("Module does not fit slot " + slot);
        }
        String key = switch (slot) {
            case AMMO -> NBT_AMMO;
            case BARREL -> NBT_BARREL;
            case MUZZLE -> NBT_MUZZLE;
        };
        if (module == null || module.id.isEmpty()) {
            gun.getOrCreateTag().remove(key);
        } else {
            gun.getOrCreateTag().putString(key, module.id);
        }
    }

    
    @Nullable
    public static GunModule remove(ItemStack gun, GunModule.Slot slot) {
        GunModule current = switch (slot) {
            case AMMO -> getAmmo(gun);
            case BARREL -> getBarrel(gun);
            case MUZZLE -> getMuzzle(gun);
        };
        if (current == GunModule.defaultFor(slot) || current == GunModule.MUZZLE_NONE) {
            return null;          
        }
        set(gun, slot, null);
        return current;
    }

    

    public static float damageMult(ItemStack gun) {
        return getAmmo(gun).damageMult * getBarrel(gun).damageMult * getMuzzle(gun).damageMult;
    }

    public static float cooldownMult(ItemStack gun) {
        return getAmmo(gun).cooldownMult * getBarrel(gun).cooldownMult * getMuzzle(gun).cooldownMult;
    }

    public static int cooldownTicks(ItemStack gun) {
        return Math.max(1, Math.round(5.0F * cooldownMult(gun)));
    }

    public static boolean hasSirius(ItemStack gun) {
        return getMuzzle(gun) == GunModule.MUZZLE_SIRIUS;
    }

    
    public static float siriusHealthPercent(ItemStack gun) { return hasSirius(gun) ? 10.0F : 0.0F; }

    public static boolean hasScope(ItemStack gun) {
        return gun.getItem() instanceof ParticleGun && getMuzzle(gun) == GunModule.MUZZLE_SCOPE;
    }

    public static float speedMult(ItemStack gun) {
        return getAmmo(gun).speedMult * getBarrel(gun).speedMult * getMuzzle(gun).speedMult;
    }

    

    public static void appendTooltip(ItemStack gun, List<Component> tooltip) {
        GunModule ammo = getAmmo(gun);
        GunModule barrel = getBarrel(gun);
        GunModule muzzle = getMuzzle(gun);
        tooltip.add(Component.translatable("tooltip.transcend.particle_gun.modules").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(" ▸ ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("gunmodule.transcend.slot.ammo").withStyle(ChatFormatting.GRAY))
                .append(ammo.displayName().withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.literal(" ▸ ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("gunmodule.transcend.slot.barrel").withStyle(ChatFormatting.GRAY))
                .append(barrel.displayName().withStyle(ChatFormatting.AQUA)));
        if (muzzle != GunModule.MUZZLE_NONE) {
            tooltip.add(Component.literal(" ▸ ").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("gunmodule.transcend.slot.muzzle").withStyle(ChatFormatting.GRAY))
                    .append(muzzle.displayName().withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }
}
