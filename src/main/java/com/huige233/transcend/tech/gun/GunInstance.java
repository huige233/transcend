package com.huige233.transcend.tech.gun;

import com.huige233.transcend.items.tech.ParticleGun;
import com.huige233.transcend.tech.TechConfig;
import com.huige233.transcend.tech.ammo.AmmoType;
import com.huige233.transcend.tech.ammo.BuiltInAmmoTypes;
import com.huige233.transcend.tech.ammo.Magazine;
import com.huige233.transcend.tech.api.ITechHeat;
import com.huige233.transcend.tech.api.ITechReloadable;
import com.huige233.transcend.tech.attribute.AttributeContainer;
import com.huige233.transcend.tech.attribute.TechAttribute;
import com.huige233.transcend.tech.core.TechEntity;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.quality.TechQuality;
import java.util.EnumMap;
import java.util.Map;
import com.huige233.transcend.tech.rank.TechRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

   
                                                                                                 
                                                                                          
   
/** 封装粒子枪物品的服务端运行状态，协调模块属性、弹匣装填、射击耗弹积热和恢复存档。 */
public final class GunInstance implements TechEntity, ITechHeat, ITechReloadable {
    private final ItemStack stack;
    private final AttributeContainer attributes = new AttributeContainer();
    private final Magazine magazine = new Magazine();
    private final Map<ModuleSlot, ModuleInstance> modules = new EnumMap<>(ModuleSlot.class);

    private GunInstance(ItemStack stack) {
        this.stack = stack;
        migrateLegacyCharge();
        TechItemData.loadAttributes(stack, attributes);
        TechItemData.loadMagazine(stack, magazine);
        for (ModuleInstance module : TechItemData.loadModules(stack)) installModule(module);
    }

    
    @Nullable public ModuleInstance module(ModuleSlot slot) { return modules.get(slot); }

    
    public boolean installModule(ModuleInstance instance) {
        if (instance == null || instance.resolve() == null) return false;
        GunModule definition = instance.resolve();
        ModuleSlot slot = definition.slot();
        if (slot == null) return false;
        uninstallModule(slot);
        modules.put(slot, instance);
        for (var modifier : definition.modifiers()) attributes.addModifier(
                modifierAttribute(modifier), modifier);
        return true;
    }

    
    public boolean uninstallModule(ModuleSlot slot) {
        ModuleInstance old = modules.remove(slot);
        if (old == null) return false;
        GunModule definition = old.resolve();
        if (definition != null) for (var modifier : definition.modifiers()) attributes.removeModifier(modifier.uuid());
        for (var modifier : old.temporaryModifiers()) attributes.removeModifier(modifier.uuid());
        return true;
    }

    
    private TechAttribute modifierAttribute(com.huige233.transcend.tech.attribute.TechAttrModifier modifier) {
        try { return TechAttribute.valueOf(modifier.name().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return TechAttribute.DAMAGE_BASE; }
    }
    
    public static GunInstance of(ItemStack stack) {
        return new GunInstance(stack);
    }

    
    private void migrateLegacyCharge() {
        CompoundTag tag = stack.getTag();
        if (TechItemData.hasGunCharge(stack) || tag == null || !tag.contains(ParticleGun.NBT_CHARGE)) return;
        float legacy = tag.getFloat(ParticleGun.NBT_CHARGE);
        TechItemData.setGunCharge(stack, Math.min(TechConfig.chargeAmmoMax(), Math.max(0.0F, legacy)));
        tag.remove(ParticleGun.NBT_CHARGE);
        TechItemData.getOrCreate(stack).putInt(TechItemData.TAG_SCHEMA, TechItemData.SCHEMA_VERSION);
    }

    @Override public AttributeContainer techAttributes() { return attributes; }
    @Override public Magazine magazine() { return magazine; }

    @Nullable
    public AmmoType loadedAmmo() { return BuiltInAmmoTypes.byId(magazine.loadedAmmoId()); }
    public float gunCharge() { return Math.min(TechConfig.chargeAmmoMax(), TechItemData.getGunCharge(stack)); }
    public float gunChargeCapacity() { return TechConfig.chargeAmmoMax(); }

    public boolean ensureRoundForUse(Player player) {
        if (!magazine.isEmpty()) return !isOverheated();
        return !isOverheated() && TechConfig.autoReload() && reload(player, BuiltInAmmoTypes.charge());
    }

    public boolean hasRoundReady() { return !magazine.isEmpty() && loadedAmmo() != null; }

    @Override
    public boolean reload(Player player, AmmoType ammo) {
        if (ammo == null) return false;
        int capacity = Math.max(1, (int) Math.round(ammo.reloadCapacity()
                + attributes.getValue(TechAttribute.AMMO_CAPACITY) - TechAttribute.AMMO_CAPACITY.defaultValue));
        long cost = Math.max(0L, (long) Math.ceil(ammo.reloadCost()
                * attributes.getValue(TechAttribute.ENERGY_COST)));
        if (!magazine.tryReload(player, stack, ammo, capacity, cost)) return false;
        TechItemData.saveMagazine(stack, magazine);
        return true;
    }

    
    public boolean commitShot() {
        if (isOverheated() || magazine.isEmpty() || loadedAmmo() == null || !magazine.tryConsumeRound()) return false;
        float nextHeat = Math.min(heatCapacity(), heat() + Math.max(0.0F,
                (float) (TechConfig.defaultHeatPerShot() * attributes.getValue(TechAttribute.HEAT_PER_SHOT))));
        TechItemData.setHeat(stack, nextHeat);
        TechItemData.saveMagazine(stack, magazine);
        return true;
    }

    
    public void tick(Level level) {
        if (level.isClientSide) return;
        boolean attributesChanged = attributes.tickCleanup(level.getGameTime());
        float heat = heat();
        float cooling = Math.max(0.0F, (float) attributes.getValue(TechAttribute.COOLING_RATE));
        if (heat > 0.0F && cooling > 0.0F) {
            float next = Math.max(0.0F, heat - cooling);
            if (next != heat) TechItemData.setHeat(stack, next);
        }
        float charge = gunCharge();
        float capacity = gunChargeCapacity();
        float regeneration = Math.max(0.0F, (float) TechConfig.chargeAmmoRegenPerTick());
        if (charge < capacity && regeneration > 0.0F) {
            float next = Math.min(capacity, charge + regeneration);
            if (next != charge) TechItemData.setGunCharge(stack, next);
        }
        if (attributesChanged) TechItemData.saveAttributes(stack, attributes);
    }

    @Override public float heat() { return TechItemData.getHeat(stack); }
    @Override public float heatCapacity() { return Math.max(0.001F, (float) attributes.getValue(TechAttribute.HEAT_CAPACITY)); }
    @Override public void addHeat(float amount) {
        float next = Math.min(heatCapacity(), Math.max(0.0F, heat() + Math.max(0.0F, amount)));
        if (next != heat()) TechItemData.setHeat(stack, next);
    }
    @Override public void tickCooling() {                                           }
    @Override public boolean isOverheated() { return heat() >= heatCapacity(); }
    @Override public float cooldownProgress() { return Math.max(0.0F, Math.min(1.0F, 1.0F - heat() / heatCapacity())); }
    public double attribute(TechAttribute attribute) { return attributes.getValue(attribute); }
    public TechQuality quality() { return TechItemData.getQuality(stack); }
    public TechRank rank() { return TechItemData.getRank(stack); }
    public int affixSlots(int baseSlots) { return quality().slots(baseSlots); }
    public void setQuality(TechQuality quality) { TechItemData.setQuality(stack, quality); }
    public void setRank(TechRank rank) { TechItemData.setRank(stack, rank); }
}




