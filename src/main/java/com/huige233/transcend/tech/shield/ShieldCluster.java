package com.huige233.transcend.tech.shield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** 按安装顺序管理多层护盾，依据防御类别与穿透等级结算伤害和能耗，并支持共享电容预算。 */
public final class ShieldCluster {
    private static final String LAYERS = "Layers";
    private final List<ShieldInstance> layers = new ArrayList<>();

    public List<ShieldInstance> layers() { return Collections.unmodifiableList(layers); }

    
    public boolean add(ShieldInstance shield) {
        if (shield == null || layers.size() >= com.huige233.transcend.tech.TechConfig.shieldClusterMaxLayers()) return false;
        layers.add(shield);
        return true;
    }

    
    private void addLoaded(ShieldInstance shield) {
        if (shield != null) layers.add(shield);
    }

    public boolean canInstall() {
        return layers.size() < com.huige233.transcend.tech.TechConfig.shieldClusterMaxLayers();
    }
    public boolean remove(ShieldInstance shield) { return layers.remove(shield); }
    public void clear() { layers.clear(); }
    public boolean isEmpty() { return layers.isEmpty(); }

    public float maxEnergy() {
        return (float) layers.stream().mapToDouble(ShieldInstance::maxEnergy).sum();
    }

    public float energy() {
        return (float) layers.stream().mapToDouble(ShieldInstance::energy).sum();
    }

    public float regen() {
        return (float) layers.stream().mapToDouble(ShieldInstance::regen).sum();
    }

       
                                                                                                  
                                                                                        
       
    public ShieldResolution resolve(ShieldDamageContext context) {
        return resolve(context, Float.POSITIVE_INFINITY, false);
    }

    
    public ShieldResolution resolvePowered(ShieldDamageContext context, float energyBudget) {
        return resolve(context, Float.isFinite(energyBudget) ? Math.max(0.0F, energyBudget) : 0.0F, true);
    }

    private ShieldResolution resolve(ShieldDamageContext context, float energyBudget, boolean powered) {
        if (context == null || context.bypassesShield() || context.amount() <= 0.0F) {
            float requested = context == null ? 0.0F : context.amount();
            return new ShieldResolution(requested, 0.0F, requested, 0.0F, 0, 0, 0);
        }

        float remaining = context.amount();
        float spent = 0.0F;
        int participating = 0;
        int depleted = 0;
        int penetrated = 0;
        int activeLimit = com.huige233.transcend.tech.TechConfig.shieldClusterMaxLayers();
        for (int layerIndex = 0; layerIndex < layers.size() && layerIndex < activeLimit; layerIndex++) {
            ShieldInstance layer = layers.get(layerIndex);
            float available = Math.min(powered ? layer.maxEnergy() : layer.energy(), Math.max(0.0F, energyBudget - spent));
            if (remaining <= 0.0F || available <= 0.0F || !layer.defenseMode().protects(context.categoryIndex())) {
                continue;
            }
            if (com.huige233.transcend.tech.combat.PenetrationPolicy.bypasses(
                    context.penetrationStrength(), layer.defenseTier())) {
                penetrated++;
                continue;
            }
            float ratio = Math.max(1.0F, layer.focusedRatio());
            float damageFactor = layer.typeReduction() > 0.0F
                    && layer.typeCategory() == context.categoryIndex()
                    ? 1.0F - layer.typeReduction() : 1.0F;
            float possibleDamage = available * ratio / damageFactor;
            float absorbed = Math.min(remaining, possibleDamage);
            float energyCost = absorbed * damageFactor / ratio;
            if (energyCost <= 0.0F) continue;

            float before = layer.energy();
            if (!powered) layer.drain(energyCost);
            spent += Math.min(available, energyCost);
            remaining -= absorbed;
            participating++;
            if (before > 0.0F && layer.energy() <= 0.0001F) depleted++;
        }
        float absorbed = context.amount() - remaining;
        return new ShieldResolution(context.amount(), absorbed, Math.max(0.0F, remaining), spent, participating, depleted, penetrated);
    }

    
    public float drain(float amount) {
        float remaining = Math.max(0.0F, amount);
        float drained = 0.0F;
        for (ShieldInstance layer : layers) {
            if (remaining <= 0.0F) break;
            float current = layer.drain(remaining);
            drained += current;
            remaining -= current;
        }
        return drained;
    }

    
    public boolean tickRegen() {
        boolean changed = false;
        for (ShieldInstance layer : layers) {
            float before = layer.energy();
            layer.recharge(layer.regen());
            changed |= before != layer.energy();
        }
        return changed;
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ShieldInstance layer : layers) {
            CompoundTag entry = new CompoundTag();
            layer.save(entry);
            list.add(entry);
        }
        tag.put(LAYERS, list);
    }

    public static ShieldCluster load(CompoundTag tag) {
        ShieldCluster cluster = new ShieldCluster();
        ListTag list = tag.getList(LAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) cluster.addLoaded(ShieldInstance.load(list.getCompound(i)));
        return cluster;
    }
}
