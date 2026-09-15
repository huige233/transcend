package com.huige233.transcend.entity;

import com.huige233.transcend.entity.shield.ShieldSystem;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2COpenTestDummyScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

   
                              
                              
                                                         
                                                            
                       
                                              
                               
                 
   
/** 提供可配置抗性、护盾、回血和生物类别的免死测试假人，并统计伤害及管理装备与效果。 */
public class TestDummy extends Mob {

    private static final EntityDataAccessor<Float> DATA_LAST_DAMAGE =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);
    
    private static final EntityDataAccessor<Float> DATA_RAW_DAMAGE =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TOTAL_DAMAGE =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_HIT =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HIT_COUNT =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);
    
    private static final EntityDataAccessor<Boolean> DATA_NO_KNOCKBACK =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.BOOLEAN);
    
    private static final EntityDataAccessor<String> DATA_CATEGORY_RESIST =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.STRING);
    
    private static final EntityDataAccessor<String> DATA_ACTIVE_BUFFS =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.STRING);

    
    private static final int DPS_WINDOW = 20;
    
    private static final int BURST_WINDOW = 100;

    private float dpsAccum = 0;
    private int dpsTicks = 0;
    private float dpsValue = 0;
    private float burstAccum = 0;
    private int burstTimer = 0;
    private boolean announceHits = true;
    private UUID owner;

    public TestDummy(EntityType<? extends TestDummy> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                
                .add(Attributes.MAX_HEALTH, 1000000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0);
    }

    
    private static final java.util.Map<net.minecraft.world.entity.ai.attributes.Attribute, Double> LOCKED_BASE =
            java.util.Map.of(
                    Attributes.MAX_HEALTH, 1000000.0,
                    Attributes.MOVEMENT_SPEED, 0.0,
                    Attributes.KNOCKBACK_RESISTANCE, 1.0);

       
                               
                                                  
                                                      
       
    private void enforceLockedAttributes() {
        for (var e : LOCKED_BASE.entrySet()) {
            var inst = this.getAttribute(e.getKey());
            if (inst == null) continue;
            boolean dirty = false;
            
            for (var mod : new ArrayList<>(inst.getModifiers())) {
                inst.removeModifier(mod.getId());
                dirty = true;
            }
            if (dirty || Math.abs(inst.getBaseValue() - e.getValue()) > 0.001D) {
                inst.setBaseValue(e.getValue());
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LAST_DAMAGE, 0.0F);
        this.entityData.define(DATA_RAW_DAMAGE, 0.0F);
        this.entityData.define(DATA_TOTAL_DAMAGE, 0.0F);
        this.entityData.define(DATA_MAX_HIT, 0.0F);
        this.entityData.define(DATA_HIT_COUNT, 0);
        this.entityData.define(DATA_NO_KNOCKBACK, true);
        this.entityData.define(DATA_CATEGORY_RESIST, "0,0,0,0,0,0");
        this.entityData.define(DATA_ACTIVE_BUFFS, "");
        this.entityData.define(DATA_RESIST_LEVEL, 0);
        this.entityData.define(DATA_BURST_TICKS, 0);
        this.entityData.define(DATA_KIND, 0);
        this.entityData.define(DATA_HIT_EFFECT, true);
        this.entityData.define(DATA_LAST_CATEGORY, -1);
        this.entityData.define(DATA_SHIELD_VALUE, 0.0F);
        this.entityData.define(DATA_SHIELD_MAX, 0.0F);
        this.entityData.define(DATA_SHIELD_REGEN_STEP, 2);
        this.entityData.define(DATA_SHIELD_REGEN_ON_HIT, false);
        this.entityData.define(DATA_SHIELD_RESIST, true);
        this.entityData.define(DATA_SHIELD_REGEN_DELAY, 60);
        this.entityData.define(DATA_SHIELD_REGEN_ON_HIT_PCT, 5);
        this.entityData.define(DATA_SHIELD_TOUGHNESS, "1,1,1,1,1,1");
        this.entityData.define(DATA_HEAL_MODE, 0);
    }

    
    
    

    
    private static final EntityDataAccessor<Float> DATA_SHIELD_VALUE =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);
    
    private static final EntityDataAccessor<Float> DATA_SHIELD_MAX =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);
    
    private static final EntityDataAccessor<Integer> DATA_SHIELD_REGEN_STEP =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);
    
    private static final EntityDataAccessor<Boolean> DATA_SHIELD_REGEN_ON_HIT =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.BOOLEAN);
    
    private static final EntityDataAccessor<Boolean> DATA_SHIELD_RESIST =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.BOOLEAN);
    
    private static final EntityDataAccessor<Integer> DATA_SHIELD_REGEN_DELAY =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);
    
    private static final EntityDataAccessor<Integer> DATA_SHIELD_REGEN_ON_HIT_PCT =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);
    
    private static final EntityDataAccessor<String> DATA_SHIELD_TOUGHNESS =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.STRING);

    
    private final ShieldSystem shieldSystem = new ShieldSystem();
    
    private final int[] shieldToughness = new int[DamageCategory.values().length];

    public int getShieldRegenStep() {
        return this.entityData.get(DATA_SHIELD_REGEN_STEP);
    }

    public void setShieldRegenStep(int step) {
        this.entityData.set(DATA_SHIELD_REGEN_STEP, Math.max(ShieldSystem.MIN_REGEN_STEP,
                Math.min(ShieldSystem.MAX_REGEN_STEP, step)));
    }

    
    public boolean isShieldRegenOnHit() {
        return this.entityData.get(DATA_SHIELD_REGEN_ON_HIT);
    }

    public void setShieldRegenOnHit(boolean value) {
        this.entityData.set(DATA_SHIELD_REGEN_ON_HIT, value);
    }

    
    public int getShieldRegenDelay() {
        return Math.max(ShieldSystem.MIN_REGEN_DELAY,
                Math.min(ShieldSystem.MAX_REGEN_DELAY, this.entityData.get(DATA_SHIELD_REGEN_DELAY)));
    }

    public void setShieldRegenDelay(int delay) {
        this.entityData.set(DATA_SHIELD_REGEN_DELAY, Math.max(ShieldSystem.MIN_REGEN_DELAY,
                Math.min(ShieldSystem.MAX_REGEN_DELAY, delay)));
    }

    
    public int getShieldRegenOnHitPercent() {
        return Math.max(ShieldSystem.MIN_REGEN_ON_HIT_PERCENT,
                Math.min(ShieldSystem.MAX_REGEN_ON_HIT_PERCENT, this.entityData.get(DATA_SHIELD_REGEN_ON_HIT_PCT)));
    }

    public void setShieldRegenOnHitPercent(int pct) {
        this.entityData.set(DATA_SHIELD_REGEN_ON_HIT_PCT, Math.max(ShieldSystem.MIN_REGEN_ON_HIT_PERCENT,
                Math.min(ShieldSystem.MAX_REGEN_ON_HIT_PERCENT, pct)));
    }

    public boolean isShieldAffectedByResist() {
        return this.entityData.get(DATA_SHIELD_RESIST);
    }

    public void setShieldAffectedByResist(boolean value) {
        this.entityData.set(DATA_SHIELD_RESIST, value);
    }

    public float getShieldValue() {
        return this.entityData.get(DATA_SHIELD_VALUE);
    }

    public float getShieldMax() {
        return this.entityData.get(DATA_SHIELD_MAX);
    }

    public boolean isShieldEnabled() {
        return getShieldMax() > 0;
    }

    
    public void setShieldMax(float max) {
        max = Math.max(0, max);
        this.entityData.set(DATA_SHIELD_MAX, max);
        if (getShieldValue() > max) {
            this.entityData.set(DATA_SHIELD_VALUE, max);
        }
        if (max > 0 && getShieldValue() <= 0) {
            
            this.entityData.set(DATA_SHIELD_VALUE, max);
        }
        shieldSystem.resetRegenDelay();
    }

    public void setShieldValue(float value) {
        this.entityData.set(DATA_SHIELD_VALUE, Math.max(0, Math.min(getShieldMax(), value)));
    }

    
    public int getShieldToughness(int categoryIndex) {
        if (this.level().isClientSide) {
            return parseToughnessSync(categoryIndex);
        }
        return (categoryIndex >= 0 && categoryIndex < shieldToughness.length)
                ? shieldToughness[categoryIndex] : 1;
    }

    
    public void setShieldToughness(int categoryIndex, int toughness) {
        if (categoryIndex >= 0 && categoryIndex < shieldToughness.length) {
            shieldToughness[categoryIndex] = Math.max(1,
                    Math.min(ShieldSystem.MAX_TOUGHNESS, toughness));
            syncToughnessToClient();
        }
    }

    private int parseToughnessSync(int categoryIndex) {
        String sync = this.entityData.get(DATA_SHIELD_TOUGHNESS);
        String[] parts = sync.split(",");
        if (categoryIndex < 0 || categoryIndex >= parts.length) return 1;
        try {
            return Math.max(1, Integer.parseInt(parts[categoryIndex].trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    
    private void syncToughnessToClient() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shieldToughness.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(shieldToughness[i]);
        }
        this.entityData.set(DATA_SHIELD_TOUGHNESS, sb.toString());
    }

       
                                            
                                          
       
    private final ShieldSystem.Host shieldHost = new ShieldSystem.Host() {
        @Override public float getShieldValue() { return TestDummy.this.getShieldValue(); }
        @Override public void setShieldValueRaw(float value) { TestDummy.this.setShieldValue(value); }
        @Override public float getShieldMax() { return TestDummy.this.getShieldMax(); }
        @Override public void setShieldMaxRaw(float max) { TestDummy.this.setShieldMax(max); }
        @Override public int getRegenStep() { return TestDummy.this.getShieldRegenStep(); }
        @Override public void setRegenStepRaw(int step) { TestDummy.this.setShieldRegenStep(step); }
        @Override public boolean isRegenOnHit() { return TestDummy.this.isShieldRegenOnHit(); }
        @Override public void setRegenOnHitRaw(boolean value) { TestDummy.this.setShieldRegenOnHit(value); }
        @Override public boolean isAffectedByResist() { return TestDummy.this.isShieldAffectedByResist(); }
        @Override public void setAffectedByResistRaw(boolean value) { TestDummy.this.setShieldAffectedByResist(value); }
        @Override public int getToughness(int categoryIndex) { return TestDummy.this.getShieldToughness(categoryIndex); }
        @Override public void setToughnessRaw(int categoryIndex, int toughness) { TestDummy.this.setShieldToughness(categoryIndex, toughness); }
        @Override public int getRegenDelay() { return TestDummy.this.getShieldRegenDelay(); }
        @Override public int getRegenOnHitPercent() { return TestDummy.this.getShieldRegenOnHitPercent(); }
        @Override public int getCategoryResistance(int categoryIndex) { return TestDummy.this.getCategoryResistance(categoryIndex); }
        @Override public int getResistanceLevel() { return TestDummy.this.getResistanceLevel(); }
        @Override public boolean isAnnounceReduce() { return TestDummy.this.isAnnounceReduce(); }
        @Override public void announceShieldAbsorb(float absorbed, float overflow, float shieldValue, float shieldMax, float regenOnHit) {
            Player blocked = getShieldAttacker();
            if (blocked == null) return;
            if (overflow <= 0.01F) {
                
                blocked.displayClientMessage(Component.literal(String.format(
                        "§b[护盾] §7吸收 %.2f §8(剩余 %.0f/%.0f)%s", absorbed, shieldValue, shieldMax,
                        regenOnHit > 0 ? String.format(" §a+%s", fmtShield(regenOnHit)) : "")), true);
            } else {
                
                blocked.displayClientMessage(Component.literal(String.format(
                        "§b[护盾] §7吸收 %.2f §8(破盾 §f%.2f§8)%s", absorbed, overflow,
                        regenOnHit > 0 ? String.format(" §a+%s", fmtShield(regenOnHit)) : "")), true);
            }
        }
    };

    
    private static String fmtShield(float v) {
        if (v >= 1_000_000F) return String.format("%.2fM", v / 1_000_000F);
        if (v >= 10_000F) return String.format("%.1fk", v / 1_000F);
        return String.format("%.1f", v);
    }

    
    private Player shieldLastAttacker;

    private Player getShieldAttacker() {
        return shieldLastAttacker;
    }


    
    private static final EntityDataAccessor<Integer> DATA_LAST_CATEGORY =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);

    
    public DamageCategory getLastCategory() {
        return DamageCategory.byIndex(this.entityData.get(DATA_LAST_CATEGORY));
    }

    
    private static final EntityDataAccessor<Integer> DATA_BURST_TICKS =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);

    
    public int getBurstTicks() {
        return this.entityData.get(DATA_BURST_TICKS);
    }

    @Override
    protected void registerGoals() {
    }

    

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.announceHits = !tag.contains("AnnounceHits") || tag.getBoolean("AnnounceHits");
        this.announceReduce = !tag.contains("AnnounceReduce") || tag.getBoolean("AnnounceReduce");
        this.dpsValue = tag.getFloat("DpsValue");
        this.burstAccum = tag.getFloat("BurstAccum");
        this.burstTimer = tag.getInt("BurstTimer");
        boolean savedNoKb = !tag.contains("NoKnockback") || tag.getBoolean("NoKnockback");
        this.entityData.set(DATA_NO_KNOCKBACK, savedNoKb);
        if (tag.contains("CategoryResistance")) {
            int[] arr = tag.getIntArray("CategoryResistance");
            for (int i = 0; i < Math.min(arr.length, categoryResistance.length); i++) {
                categoryResistance[i] = Math.max(0, Math.min(100, arr[i]));
            }
        }
        
        syncResistToClient();
        
        setResistanceLevel(tag.getInt("ResistanceLevel"));
        
        setHitEffectEnabled(!tag.contains("HitEffect") || tag.getBoolean("HitEffect"));
        
        this.entityData.set(DATA_KIND, Math.max(0, Math.min(DummyKind.values().length - 1,
                tag.getInt("DummyKind"))));
        
        setShieldMax(tag.getFloat("ShieldMax"));
        setShieldValue(tag.contains("ShieldValue") ? tag.getFloat("ShieldValue") : getShieldMax());
        setShieldRegenStep(tag.contains("ShieldRegenStep") ? tag.getInt("ShieldRegenStep") : 2);
        setShieldRegenOnHit(tag.contains("ShieldRegenOnHit") && tag.getBoolean("ShieldRegenOnHit"));
        setShieldAffectedByResist(!tag.contains("ShieldAffectedByResist") || tag.getBoolean("ShieldAffectedByResist"));
        setShieldRegenDelay(tag.contains("ShieldRegenDelay") ? tag.getInt("ShieldRegenDelay") : 60);
        setShieldRegenOnHitPercent(tag.contains("ShieldRegenOnHitPct") ? tag.getInt("ShieldRegenOnHitPct") : 5);
        int[] savedToughness = ShieldSystem.readToughness(tag);
        for (int i = 0; i < Math.min(savedToughness.length, shieldToughness.length); i++) {
            shieldToughness[i] = savedToughness[i];
        }
        syncToughnessToClient();
        
        setHealMode(tag.contains("HealMode") ? tag.getInt("HealMode") : 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putBoolean("AnnounceHits", announceHits);
        tag.putBoolean("AnnounceReduce", announceReduce);
        tag.putFloat("DpsValue", dpsValue);
        tag.putFloat("BurstAccum", burstAccum);
        tag.putInt("BurstTimer", burstTimer);
        tag.putBoolean("NoKnockback", isNoKnockback());
        tag.putIntArray("CategoryResistance", categoryResistance);
        tag.putInt("ResistanceLevel", getResistanceLevel());
        tag.putBoolean("HitEffect", isHitEffectEnabled());
        tag.putInt("DummyKind", this.entityData.get(DATA_KIND));
        tag.putFloat("ShieldMax", getShieldMax());
        tag.putFloat("ShieldValue", getShieldValue());
        tag.putInt("ShieldRegenStep", getShieldRegenStep());
        tag.putBoolean("ShieldRegenOnHit", isShieldRegenOnHit());
        tag.putBoolean("ShieldAffectedByResist", isShieldAffectedByResist());
        tag.putInt("ShieldRegenDelay", getShieldRegenDelay());
        tag.putInt("ShieldRegenOnHitPct", getShieldRegenOnHitPercent());
        ShieldSystem.writeToughness(tag, shieldToughness);
        tag.putInt("HealMode", getHealMode().index);
    }

    

    public synchronized boolean canConfigure(ServerPlayer player) {
        if (player == null || !player.serverLevel().getServer().isSameThread()) {
            return false;
        }
        if (player.hasPermissions(2)) return true;
        if (owner == null) {
            owner = player.getUUID();
            return true;
        }
        return owner.equals(player.getUUID());
    }

    public synchronized boolean bindToCreator(ServerPlayer creator) {
        if (creator == null || !creator.serverLevel().getServer().isSameThread() || owner != null) {
            return false;
        }
        owner = creator.getUUID();
        return true;
    }

    public UUID getOwner() {
        return owner;
    }

    

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);
        this.setNoGravity(true);

        
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            enforceLockedAttributes();
        }

        

        dpsTicks++;
        if (dpsTicks >= DPS_WINDOW) {
            dpsValue = dpsAccum * (20.0F / DPS_WINDOW);
            dpsAccum = 0;
            dpsTicks = 0;
        }

        if (!this.level().isClientSide && this.entityData.get(DATA_BURST_TICKS) != burstTimer) {
            
            this.entityData.set(DATA_BURST_TICKS, Math.max(0, burstTimer));
        }

        
        
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            syncBuffsToClient();
        }

        
        if (!this.level().isClientSide && isShieldEnabled()) {
            shieldSystem.tickRegen(shieldHost);
        }

        if (burstTimer > 0) {
            burstTimer--;
            if (burstTimer <= 0) burstAccum = 0;
        }

        
        
        if (!this.level().isClientSide && getHealMode() != HealMode.NONE) {
            tickHeal();
        }
    }

    
    private int lastHurtAt = -1000;

    
    private static final EntityDataAccessor<Boolean> DATA_HIT_EFFECT =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.BOOLEAN);

    public boolean isHitEffectEnabled() {
        return this.entityData.get(DATA_HIT_EFFECT);
    }

    public void setHitEffectEnabled(boolean value) {
        this.entityData.set(DATA_HIT_EFFECT, value);
    }
    
    @SuppressWarnings("unused")
    private UUID lastAttacker;

    private void updateCustomName() {
        String burstStr = burstTimer > 0
                ? String.format(" | §d%.0f §7/ 5s", burstAccum)
                : "";
        this.setCustomName(Component.literal(String.format(
                "§c%.1f §7last | §e%.1f §7DPS%s",
                this.entityData.get(DATA_LAST_DAMAGE), dpsValue, burstStr)));
        this.setCustomNameVisible(true);
    }

    

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        
        if (source.getEntity() instanceof Player p) {
            shieldLastAttacker = p;
        }
        
        DamageCategory category = classifySource(source);

        
        
        
        if (isShieldEnabled()) {
            
            float shieldInput = shieldSystem.applyResistChain(shieldHost, amount, category);
            float overflow = shieldSystem.absorb(shieldHost, amount, category, shieldInput);

            
            this.entityData.set(DATA_RAW_DAMAGE, amount);
            this.entityData.set(DATA_LAST_CATEGORY, category != null ? category.index : -1);

            if (overflow <= 0.01F) {
                
                return true;
            }
            
            amount = overflow;
        }

        
        
        float original = amount;
        
        

        
        if (category != null) {
            int pct = categoryResistance[category.index];
            if (pct >= 100) {
                if (announceReduce && source.getEntity() instanceof Player p) {
                    p.displayClientMessage(Component.literal(
                            String.format("§7 目标造成伤害：%.2f, 已免疫", original)), true);
                }
                
                this.entityData.set(DATA_RAW_DAMAGE, original);
                this.entityData.set(DATA_LAST_CATEGORY, category.index);
                return true;
            }
            amount *= 1.0F - pct / 100.0F;
        }

        
        int resistPct = getResistanceLevel();
        if (resistPct >= 100) {
            if (announceReduce && source.getEntity() instanceof Player p) {
                p.displayClientMessage(Component.literal(
                        String.format("§7 目标造成伤害：%.2f, 已免疫", original)), true);
            }
            
            this.entityData.set(DATA_RAW_DAMAGE, original);
            this.entityData.set(DATA_LAST_CATEGORY, category != null ? category.index : -1);
            return true;
        }
        if (resistPct > 0) {
            amount *= 1.0F - resistPct / 100.0F;
        }
        float effectiveAmount = amount;

        
        if (announceReduce && effectiveAmount < original && source.getEntity() instanceof Player p1) {
            int totalPct = original > 0 ? Math.round((1.0F - effectiveAmount / original) * 100) : 0;
            String kind = category != null ? category.name().toLowerCase() : "generic";
            p1.displayClientMessage(Component.literal(String.format(
                    "§7 目标造成伤害：%.2f, §f%s§7 已减免%d%%", original, kind, totalPct)), true);
        }

        boolean isDot = source.getMsgId().contains("inFire") || source.getMsgId().contains("onFire")
                || source.getMsgId().contains("lava") || source.getMsgId().contains("wither")
                || source.getMsgId().contains("poison") || source.getMsgId().contains("freeze");

        
        this.entityData.set(DATA_RAW_DAMAGE, original);
        this.entityData.set(DATA_LAST_CATEGORY, category != null ? category.index : -1);
        amount = effectiveAmount;

        if (!isDot) {
            this.entityData.set(DATA_LAST_DAMAGE, amount);
            float maxHit = this.entityData.get(DATA_MAX_HIT);
            if (amount > maxHit) {
                this.entityData.set(DATA_MAX_HIT, amount);
            }
            this.entityData.set(DATA_HIT_COUNT, this.entityData.get(DATA_HIT_COUNT) + 1);
        }
        this.entityData.set(DATA_TOTAL_DAMAGE, this.entityData.get(DATA_TOTAL_DAMAGE) + amount);
        dpsAccum += amount;
        burstAccum += amount;
        burstTimer = BURST_WINDOW;

        
        

        if (this.level() instanceof ServerLevel sl) {
            
            lastHurtAt = this.tickCount;
            if (isHitEffectEnabled()) {
                float intensity = Math.min(1.0F, amount / 50.0F);
                DustParticleOptionsHolder.spawnHitParticles(sl, this, intensity);
            }
        }

        
        
        
        
        this.invulnerableTime = 0;
        this.lastHurt = 0;
        boolean result = super.hurt(source, amount);
        
        
        
        
        HealMode mode = getHealMode();
        if (mode == HealMode.IMMEDIATE) {
            this.setHealth(this.getMaxHealth());
        } else if (mode == HealMode.INTERVAL || mode == HealMode.LOW_HP) {
            healTimer = 0;
        }
        return true;
    }

    
    
    
    
    

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean isServer = !player.level().isClientSide;

        
        if (hand == InteractionHand.MAIN_HAND && !held.isEmpty()) {
            net.minecraft.world.entity.EquipmentSlot slot = pickEquipSlot(held);
            if (slot != null) {
                if (!isServer) return InteractionResult.SUCCESS;
                ItemStack current = this.getItemBySlot(slot);
                this.setItemSlot(slot, held.copy());
                
                
                player.setItemInHand(hand, current);
                this.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, 0.8F, 1.0F);
                return InteractionResult.CONSUME;
            }
        }

        
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        
        if (isServer && player instanceof ServerPlayer sp && canConfigure(sp)) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new S2COpenTestDummyScreen(this.getId()));
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

       
                                         
                                          
       
    private net.minecraft.world.entity.EquipmentSlot pickEquipSlot(ItemStack held) {
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() != net.minecraft.world.entity.EquipmentSlot.Type.ARMOR
                    && slot != net.minecraft.world.entity.EquipmentSlot.OFFHAND
                    && slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) continue;
            if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND && !this.getMainHandItem().isEmpty()) {
                continue;               
            }
            if (held.canEquip(slot, this)) return slot;
        }
        return null;
    }

    

    public void resetData() {
        this.entityData.set(DATA_LAST_DAMAGE, 0.0F);
        this.entityData.set(DATA_TOTAL_DAMAGE, 0.0F);
        this.entityData.set(DATA_MAX_HIT, 0.0F);
        this.entityData.set(DATA_HIT_COUNT, 0);
        this.dpsAccum = 0;
        this.dpsTicks = 0;
        this.dpsValue = 0;
        this.burstAccum = 0;
        this.burstTimer = 0;
        this.setCustomName(Component.translatable("entity.transcend.test_dummy.data_cleared"));
    }

    public void toggleAnnounce() {
        this.announceHits = !this.announceHits;
    }

    public boolean isAnnounceHits() {
        return this.announceHits;
    }

    
    private boolean announceReduce = true;

    public boolean isAnnounceReduce() {
        return this.announceReduce;
    }

    public void setAnnounceReduce(boolean value) {
        this.announceReduce = value;
    }

    

    @Override
    public boolean isPushable() {
        return !isNoKnockback();
    }

       
                                                       
                                      
       
    @Override
    public void setDeltaMovement(net.minecraft.world.phys.Vec3 deltaMovement) {
        if (isNoKnockback() && !this.level().isClientSide) {
            super.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            return;
        }
        super.setDeltaMovement(deltaMovement);
    }

    
    @Override
    public void knockback(double strength, double x, double z) {
        if (isNoKnockback()) return;
        super.knockback(strength, x, z);
    }

    
    private static final EntityDataAccessor<Integer> DATA_RESIST_LEVEL =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);

    
    public int getResistanceLevel() {
        return this.entityData.get(DATA_RESIST_LEVEL);
    }

       
                              
                                                            
                    
       
    public void setResistanceLevel(int level) {
        this.entityData.set(DATA_RESIST_LEVEL, Math.max(0, Math.min(100, level)));
    }

    

       
                         
                                                   
                                   
                        
       
    public boolean applyBuff(String effectId, int amplifier) {
        var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS
                .getValue(new net.minecraft.resources.ResourceLocation(effectId));
        if (effect == null) return false;
        boolean ok = this.addEffect(new MobEffectInstance(effect, -1, Math.max(0, amplifier), false, false));
        syncBuffsToClient();
        return ok;
    }

    
    public boolean removeBuff(String effectId) {
        var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS
                .getValue(new net.minecraft.resources.ResourceLocation(effectId));
        if (effect == null) return false;
        boolean ok = this.removeEffect(effect);
        syncBuffsToClient();
        return ok;
    }

    
    public void clearBuffs() {
        this.removeAllEffects();
        syncBuffsToClient();
    }

    
    private void syncBuffsToClient() {
        StringBuilder sb = new StringBuilder();
        for (MobEffectInstance inst : this.getActiveEffects()) {
            if (sb.length() > 0) sb.append(';');
            String id = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect()).toString();
            sb.append(id).append(' ').append(romanLevel(inst.getAmplifier()));
        }
        this.entityData.set(DATA_ACTIVE_BUFFS, sb.toString());
    }

       
                 
                                                        
       
    public List<String> getActiveBuffDescriptions() {
        if (!this.level().isClientSide) {
            syncBuffsToClient();
            List<String> list = new ArrayList<>();
            for (MobEffectInstance inst : this.getActiveEffects()) {
                String id = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect()).toString();
                list.add(id + " " + romanLevel(inst.getAmplifier()));
            }
            return list;
        }
        String sync = this.entityData.get(DATA_ACTIVE_BUFFS);
        if (sync == null || sync.isEmpty()) return List.of();
        return new ArrayList<>(List.of(sync.split(";")));
    }

    private static String romanLevel(int amplifier) {
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return amplifier >= 0 && amplifier < roman.length ? roman[amplifier] : String.valueOf(amplifier + 1);
    }

    

    
    /** 为假人的近战、投射物、爆炸、火焰、魔法和环境伤害定义稳定分类索引。 */
    public enum DamageCategory {
        MELEE(0),           
        PROJECTILE(1),       
        EXPLOSION(2),       
        FIRE(3),               
        MAGIC(4),           
        ENVIRONMENT(5);                    

        public final int index;
        DamageCategory(int index) { this.index = index; }

        private static final DamageCategory[] VALUES = values();
        public static DamageCategory byIndex(int i) {
            return (i >= 0 && i < VALUES.length) ? VALUES[i] : null;
        }
    }

    
    private final int[] categoryResistance = new int[DamageCategory.values().length];

    
    public int getCategoryResistance(int categoryIndex) {
        if (this.level().isClientSide) {
            return parseResistSync(categoryIndex);
        }
        return (categoryIndex >= 0 && categoryIndex < categoryResistance.length)
                ? categoryResistance[categoryIndex] : 0;
    }

    private int parseResistSync(int categoryIndex) {
        String sync = this.entityData.get(DATA_CATEGORY_RESIST);
        String[] parts = sync.split(",");
        if (categoryIndex < 0 || categoryIndex >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[categoryIndex].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    
    public void setCategoryResistance(int categoryIndex, int percent) {
        if (categoryIndex >= 0 && categoryIndex < categoryResistance.length) {
            categoryResistance[categoryIndex] = Math.max(0, Math.min(100, percent));
            syncResistToClient();
        }
    }

    
    private void syncResistToClient() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categoryResistance.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(categoryResistance[i]);
        }
        this.entityData.set(DATA_CATEGORY_RESIST, sb.toString());
    }

    
    
    
    

    
    /** 定义假人立即回满、间隔回满、低血量回满和不回血四种恢复策略。 */
    public enum HealMode {
        IMMEDIATE(0), INTERVAL(1), LOW_HP(2), NONE(3);

        public final int index;
        HealMode(int index) { this.index = index; }

        private static final HealMode[] VALUES = values();
        public static HealMode byIndex(int i) {
            return (i >= 0 && i < VALUES.length) ? VALUES[i] : IMMEDIATE;
        }
    }

    
    private static final int HEAL_INTERVAL_TICKS = 60;
    
    private static final float LOW_HP_THRESHOLD = 0.1F;

    private static final EntityDataAccessor<Integer> DATA_HEAL_MODE =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);

    
    private int healTimer = 0;

    
    public HealMode getHealMode() {
        return HealMode.byIndex(this.entityData.get(DATA_HEAL_MODE));
    }

    
    public void setHealMode(int mode) {
        healTimer = 0;
        this.entityData.set(DATA_HEAL_MODE, Math.max(0, Math.min(HealMode.values().length - 1, mode)));
    }

       
                                                          
                                                               
                         
       
    private void tickHeal() {
        switch (getHealMode()) {
            case IMMEDIATE -> {
                if (this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
            }
            case INTERVAL -> {
                if (++healTimer >= HEAL_INTERVAL_TICKS) {
                    healTimer = 0;
                    if (this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
                }
            }
            case LOW_HP -> {
                if (this.getHealth() < this.getMaxHealth() * LOW_HP_THRESHOLD) {
                    this.setHealth(this.getMaxHealth());
                }
            }
            case NONE -> { }
        }
    }

    

    
    /** 定义假人可模拟的普通、亡灵、节肢、灾厄村民和水生生物类别。 */
    public enum DummyKind {
        NORMAL("normal"),              
        UNDEAD("undead"),                         
        ARTHROPOD("arthropod"),                
        ILLAGER("illager"),                      
        WATER("water");                       

        public final String id;
        DummyKind(String id) { this.id = id; }

        private static final DummyKind[] VALUES = values();
        public static DummyKind byIndex(int i) {
            return (i >= 0 && i < VALUES.length) ? VALUES[i] : NORMAL;
        }
    }

    
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.INT);

    public DummyKind getKind() {
        return DummyKind.byIndex(this.entityData.get(DATA_KIND));
    }

    
    public void setKind(int kindIndex) {
        this.entityData.set(DATA_KIND, Math.max(0, Math.min(DummyKind.values().length - 1, kindIndex)));
    }

    

    
    @Override
    public net.minecraft.world.entity.MobType getMobType() {
        return switch (getKind()) {
            case UNDEAD -> net.minecraft.world.entity.MobType.UNDEAD;
            case ARTHROPOD -> net.minecraft.world.entity.MobType.ARTHROPOD;
            case ILLAGER -> net.minecraft.world.entity.MobType.ILLAGER;
            case WATER -> net.minecraft.world.entity.MobType.WATER;
            default -> super.getMobType();
        };
    }

    
    public boolean isNoKnockback() {
        return this.entityData.get(DATA_NO_KNOCKBACK);
    }

    
    public void setNoKnockback(boolean value) {
        this.entityData.set(DATA_NO_KNOCKBACK, value);
    }

    
    public static DamageCategory classifySource(DamageSource source) {
        String m = source.getMsgId();
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            return DamageCategory.PROJECTILE;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return DamageCategory.EXPLOSION;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return DamageCategory.FIRE;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)) return DamageCategory.ENVIRONMENT;
        if (m.contains("arrow") || m.contains("trident") || m.contains("projectile")
                || m.contains("thrown") || m.contains("firework")) return DamageCategory.PROJECTILE;
        if (m.contains("player") || m.contains("mob") || m.equals("attack")) return DamageCategory.MELEE;
        if (m.contains("magic") || m.contains("indirectMagic") || m.equals("wither")
                || m.contains("dragon_breath")) return DamageCategory.MAGIC;
        if (m.equals("fall") || m.equals("drown") || m.equals("starve") || m.equals("inWall")
                || m.equals("cactus") || m.equals("sweetBerryBush") || m.equals("lightningBolt")) return DamageCategory.ENVIRONMENT;
        return null;
    }

    public float getLastDamage() {
        return this.entityData.get(DATA_LAST_DAMAGE);
    }

    
    public float getRawDamage() {
        return this.entityData.get(DATA_RAW_DAMAGE);
    }

    
    public boolean isBurstWindowActive() {
        return getBurstTicks() > 0;
    }

    public float getTotalDamage() {
        return this.entityData.get(DATA_TOTAL_DAMAGE);
    }

    public float getMaxHit() {
        return this.entityData.get(DATA_MAX_HIT);
    }

    public int getHitCount() {
        return this.entityData.get(DATA_HIT_COUNT);
    }

    public float getDps() {
        return dpsValue;
    }

    public float getBurstDamage() {
        return burstTimer > 0 ? burstAccum : 0;
    }

    

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public double getMyRidingOffset() { return 0; }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions size) {
        return 2.5F;
    }

    @Override
    public void teleportTo(double x, double y, double z) {
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingMultiplier, boolean recentlyHit) {
        
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        
        return source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                || super.isInvulnerableTo(source);
    }

    @Override
    public void die(DamageSource source) {
        
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        
        return true;
    }

    
    /** 按受击强度在假人周围生成数量和尺寸变化的红色尘粒反馈。 */
    private static final class DustParticleOptionsHolder {
        static void spawnHitParticles(ServerLevel sl, TestDummy dummy, float intensity) {
            net.minecraft.core.particles.DustParticleOptions dust = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(1.0F, 0.3F, 0.3F), 2.0F + intensity * 2.0F);
            sl.sendParticles(dust, dummy.getX(), dummy.getY() + 0.8, dummy.getZ(),
                    5 + (int) (intensity * 10), 0.3, 0.5, 0.3, 0.1);
        }
    }

    

    
    private static String trimNumber(float v) {
        if (v >= 10000000) {
            return String.format("%.2fe%d", v / 1000000.0, 6);
        }
        if (v == Math.floor(v) && !Float.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.format("%.2f", v);
    }
}