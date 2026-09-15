package com.huige233.transcend.util;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

   
                                          
  
                             
                                    
                                                                   
                                                                     
                                                                    
                                                                
                                                                       
  
                                                                 
                                           
   
/** 定期按属性类别为全套超越护甲玩家施加跨模组属性加成，并在卸下套装后移除这些加成。 */
public final class TranscendLinkBoost {

    private TranscendLinkBoost() {
    }

    
    
    private static final double CAPACITY_MUL = 4.0;
    
    private static final double POTENCY_ADD = 5.0;
    
    private static final double POTENCY_MUL = 2.0;
    
    private static final double THROUGHPUT_MUL = 1.5;
    
    private static final double DEFENSE_MUL = 3.0;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("TranscendLinkBoost");
    private static int appliedCount = 0;
    private static int nullCount = 0;
    private static int lastReportTick = -100;
    private static boolean armReported = false;

    
    private static boolean loaded(String id) {
        try {
            return ModList.get() != null && ModList.get().isLoaded(id);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean arsLoaded() { return loaded("ars_nouveau"); }
    private static boolean goetyLoaded() { return loaded("goety"); }
    private static boolean issLoaded() { return loaded("irons_spellbooks"); }
    private static boolean malumLoaded() { return loaded("malum"); }
    private static boolean apothLoaded() { return loaded("attributeslib"); }

    
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        LOGGER.debug("[LinkBoost] serverStarting; ars={} goety={} iss={} malum={} apoth={}",
                arsLoaded(), goetyLoaded(), issLoaded(), malumLoaded(), apothLoaded());
    }

    
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.player == null) return;
        tick(event.player);
    }

    public static void tick(Player player) {
        if (player == null || player.level().isClientSide) return;
        if (!armReported) {
            armReported = true;
            LOGGER.debug("[LinkBoost] class loaded; ars={} goety={} iss={} malum={} apoth={}",
                    arsLoaded(), goetyLoaded(), issLoaded(), malumLoaded(), apothLoaded());
        }
        
        if (player.tickCount % 20 != 0) return;

        try {
            if (!ArmorUtils.fullEquipped(player)) {
                removeAll(player);
                return;
            }
            applyAll(player);
        } catch (Throwable t) {
            LOGGER.error("[LinkBoost] apply error", t);
        }
    }

    private static void applyAll(Player player) {
        if (arsLoaded()) applyArs(player);
        if (goetyLoaded()) applyGoety(player);
        if (issLoaded()) applyIss(player);
        if (malumLoaded()) applyMalum(player);
        if (apothLoaded()) applyApoth(player);
        
        applyAdd(player, ForgeMod.ENTITY_REACH.get(), 10.0);
    }

    
    private static void applyArs(Player player) {
        var P = com.hollingsworth.arsnouveau.api.perk.PerkAttributes.class;
        
        capacity(player, arsAttr(P, "MAX_MANA"));
        capacity(player, arsAttr(P, "MAX_MANA_BONUS"));
        capacity(player, arsAttr(P, "WARDING"));
        
        throughput(player, arsAttr(P, "MANA_REGEN_BONUS"));
        
        potency(player, arsAttr(P, "SPELL_DAMAGE_BONUS"));
    }

    @SuppressWarnings("unchecked")
    private static Attribute arsAttr(Class<?> clazz, String field) {
        try {
            java.lang.reflect.Field f = clazz.getField(field);
            Object v = f.get(null);
            if (v instanceof net.minecraftforge.registries.RegistryObject<?> ro) return (Attribute) ro.get();
        } catch (Throwable ignored) {}
        return null;
    }

    
    private static void applyGoety(Player player) {
        Class<?> A = com.Polarice3.Goety.init.ModAttributes.class;
        
        potency(player, goetyAttr(A, "SPELL_POTENCY"));
        potency(player, goetyAttr(A, "ABYSS_POTENCY"));
        potency(player, goetyAttr(A, "FROST_POTENCY"));
        potency(player, goetyAttr(A, "GEOMANCY_POTENCY"));
        potency(player, goetyAttr(A, "NECROMANCY_POTENCY"));
        potency(player, goetyAttr(A, "NETHER_POTENCY"));
        potency(player, goetyAttr(A, "STORM_POTENCY"));
        potency(player, goetyAttr(A, "VOID_POTENCY"));
        potency(player, goetyAttr(A, "WILD_POTENCY"));
        potency(player, goetyAttr(A, "WIND_POTENCY"));
        
        applyAdd(player, goetyAttr(A, "SPELL_RADIUS"), 10.0);
        
        throughput(player, goetyAttr(A, "SPELL_VELOCITY"));
        throughput(player, goetyAttr(A, "CASTING_SPEED"));
        throughput(player, goetyAttr(A, "COOLDOWN_DISCOUNT"));
        
        Attribute soul = goetyAttr(A, "SOUL_DISCOUNT");
        applyAdd(player, soul, 0.2);
        applyMul(player, soul, 4.0);
    }

    @SuppressWarnings("unchecked")
    private static Attribute goetyAttr(Class<?> clazz, String field) {
        try {
            java.lang.reflect.Field f = clazz.getField(field);
            Object v = f.get(null);
            if (v instanceof net.minecraftforge.registries.RegistryObject<?> ro) return (Attribute) ro.get();
            if (v instanceof Attribute a) return a;
        } catch (Throwable ignored) {}
        return null;
    }

    
    private static void applyIss(Player player) {
        Class<?> R = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.class;
        
        capacity(player, attr(R, "MAX_MANA"));
        
        throughput(player, attr(R, "MANA_REGEN"));
        throughput(player, attr(R, "COOLDOWN_REDUCTION"));
        throughput(player, attr(R, "CAST_TIME_REDUCTION"));
        
        potency(player, attr(R, "SPELL_POWER"));
        
        defense(player, attr(R, "SPELL_RESIST"));
        String[] schools = {"FIRE", "ICE", "LIGHTNING", "HOLY", "ENDER", "BLOOD", "EVOCATION", "NATURE", "ELDRITCH"};
        for (String sc : schools) {
            defense(player, attr(R, sc + "_MAGIC_RESIST"));
        }
    }

    
    @SuppressWarnings("unchecked")
    private static Attribute attr(Class<?> clazz, String field) {
        try {
            java.lang.reflect.Field f = clazz.getField(field);
            Object v = f.get(null);
            if (v instanceof net.minecraftforge.registries.RegistryObject<?> ro) {
                return (Attribute) ro.get();
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    
    private static void applyMalum(Player player) {
        var M = com.sammy.malum.registry.common.AttributeRegistry.class;
        
        potency(player, malumAttr(M, "ARCANE_RESONANCE"));
        
        capacity(player, malumAttr(M, "RESERVE_STAFF_CHARGES"));
        capacity(player, malumAttr(M, "SOUL_WARD_CAP"));
        
        defense(player, malumAttr(M, "SOUL_WARD_RECOVERY_RATE"));
        defense(player, malumAttr(M, "SOUL_WARD_INTEGRITY"));
        
        Class<?> L = team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry.class;
        throughput(player, lodestoneAttr(L, "MAGIC_PROFICIENCY"));
        potency(player, lodestoneAttr(L, "MAGIC_DAMAGE"));
    }

    @SuppressWarnings("unchecked")
    private static Attribute malumAttr(Class<?> clazz, String field) {
        try {
            java.lang.reflect.Field f = clazz.getField(field);
            Object v = f.get(null);
            if (v instanceof net.minecraftforge.registries.RegistryObject<?> ro) return (Attribute) ro.get();
        } catch (Throwable ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Attribute lodestoneAttr(Class<?> clazz, String field) {
        try {
            java.lang.reflect.Field f = clazz.getField(field);
            Object v = f.get(null);
            if (v instanceof net.minecraftforge.registries.RegistryObject<?> ro) return (Attribute) ro.get();
        } catch (Throwable ignored) {}
        return null;
    }

    
    private static void applyApoth(Player player) {
        
        applyAdd(player, dev.shadowsoffire.attributeslib.api.ALObjects.Attributes.PROT_SHRED.get(), 100.0);
        applyAdd(player, dev.shadowsoffire.attributeslib.api.ALObjects.Attributes.PROT_PIERCE.get(), 34.0);
        
        defense(player, dev.shadowsoffire.attributeslib.api.ALObjects.Attributes.HEALING_RECEIVED.get());
    }

    
    private static void capacity(Player player, Attribute attr) { applyMul(player, attr, CAPACITY_MUL); }
    private static void throughput(Player player, Attribute attr) { applyMul(player, attr, THROUGHPUT_MUL); }
    private static void defense(Player player, Attribute attr) { applyMul(player, attr, DEFENSE_MUL); }
    private static void potency(Player player, Attribute attr) { applyPotency(player, attr); }

    
    private static void applyMul(Player player, Attribute attr, double multiplier) {
        if (attr == null) return;
        try {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) {
                nullCount++;
                report(player);
                return;
            }
            applyMod(inst, uuidFor(attr, AttributeModifier.Operation.MULTIPLY_TOTAL),
                    multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
            appliedCount++;
            report(player);
        } catch (Throwable ignored) {
        }
    }

    
    private static void applyAdd(Player player, Attribute attr, double amount) {
        if (attr == null) return;
        try {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) {
                nullCount++;
                report(player);
                return;
            }
            applyMod(inst, uuidFor(attr, AttributeModifier.Operation.ADDITION), amount, AttributeModifier.Operation.ADDITION);
            appliedCount++;
            report(player);
        } catch (Throwable ignored) {
        }
    }

    
    private static void applyPotency(Player player, Attribute attr) {
        if (attr == null) return;
        try {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) {
                nullCount++;
                report(player);
                return;
            }
            UUID addId = uuidFor(attr, AttributeModifier.Operation.ADDITION);
            UUID mulId = uuidFor(attr, AttributeModifier.Operation.MULTIPLY_TOTAL);
            applyMod(inst, addId, POTENCY_ADD, AttributeModifier.Operation.ADDITION);
            applyMod(inst, mulId, POTENCY_MUL, AttributeModifier.Operation.MULTIPLY_TOTAL);
            appliedCount += 2;
            report(player);
        } catch (Throwable ignored) {
        }
    }

    
    private static void applyMod(AttributeInstance inst, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) < 0.0001 && existing.getOperation() == op) return;
            inst.removeModifier(id);
        }
        inst.addTransientModifier(new AttributeModifier(id, "tr_" + inst.getAttribute().getDescriptionId(), amount, op));
    }

    private static UUID uuidFor(Attribute attr, AttributeModifier.Operation op) {
        String seed = "tr_" + attr.getDescriptionId() + "_" + op.ordinal();
        return UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    
    private static void report(Player player) {
        if (player.tickCount - lastReportTick < 60) return;
        lastReportTick = player.tickCount;
        LOGGER.debug("[LinkBoost] applied={} null={} player={}",
                appliedCount, nullCount, player.getName().getString());
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("  ISS_MAX_MANA=").append(valueOf(player, attr(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.class, "MAX_MANA")));
            sb.append(" SPELL_POWER=").append(valueOf(player, attr(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.class, "SPELL_POWER")));
            if (goetyLoaded()) {
                sb.append(" GOETY_POTENCY=").append(valueOf(player, goetyAttr(com.Polarice3.Goety.init.ModAttributes.class, "SPELL_POTENCY")));
            }
            if (malumLoaded()) {
                sb.append(" MALUM_ARCANE=").append(valueOf(player, malumAttr(com.sammy.malum.registry.common.AttributeRegistry.class, "ARCANE_RESONANCE")));
            }
            if (arsLoaded()) {
                sb.append(" ARS_MAX_MANA=").append(valueOf(player, arsAttr(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.class, "MAX_MANA")));
                sb.append(" ARS_SPELL_DMG=").append(valueOf(player, arsAttr(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.class, "SPELL_DAMAGE_BONUS")));
            }
            LOGGER.debug("[LinkBoost] values:{}", sb);
        } catch (Throwable t) {
            LOGGER.debug("[LinkBoost] value read failed: {}", t.toString());
        }
    }

    private static double valueOf(Player player, Attribute attr) {
        if (attr == null) return -999.0;
        AttributeInstance inst = player.getAttribute(attr);
        return inst == null ? -888.0 : inst.getValue();
    }

    private static void removeAll(Player player) {
        try {
            for (AttributeInstance inst : player.getAttributes().getSyncableAttributes()) {
                for (AttributeModifier m : new java.util.ArrayList<>(inst.getModifiers())) {
                    if (m.getName().startsWith("tr_")) inst.removeModifier(m.getId());
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
