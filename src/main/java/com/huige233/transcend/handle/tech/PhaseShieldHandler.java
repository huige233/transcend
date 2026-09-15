package com.huige233.transcend.handle.tech;

import com.huige233.transcend.entity.TestDummy;
import com.huige233.transcend.items.tech.PhaseShield;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.combat.TechHitSnapshot;
import com.huige233.transcend.tech.shield.ShieldCluster;
import com.huige233.transcend.tech.shield.ShieldDamageContext;
import com.huige233.transcend.tech.shield.ShieldResolution;
import com.huige233.transcend.util.CuriosFinder;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CPhaseShieldState;
import com.huige233.transcend.tech.shield.PhaseShieldStatus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;


/** 统一结算手持与饰品相位护盾的伤害吸收和电容消耗，处理负面效果拦截并同步显示状态。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PhaseShieldHandler {
    private static final Map<ServerPlayer, PhaseShieldStatus> LAST_SENT = new WeakHashMap<>();
    private static final Map<ServerPlayer, String> LAST_SENT_KEY = new WeakHashMap<>();

    public static PhaseShieldStatus displayStatus(Player player) {
        if (!player.isAlive() || player.isSpectator()) return PhaseShieldStatus.ABSENT;
        ItemStack held = findPhaseShield(player);
        if (!held.isEmpty()) {
            return PhaseShieldStatus.evaluate(true, true,
                    PhaseShield.getEnergy(held) >= PhaseShield.MIN_USE_ENERGY);
        }
        List<SlotResult> curios = CuriosFinder.findCurios(player, stack -> stack.getItem() instanceof PhaseShield);
        if (!curios.isEmpty()) {
            boolean powered = curios.stream().anyMatch(slot ->
                    PhaseShield.getEnergy(slot.stack()) >= PhaseShield.MIN_USE_ENERGY);
            return PhaseShieldStatus.evaluate(true, !isLegacyProtected(player), powered);
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof PhaseShield) return PhaseShieldStatus.INACTIVE;
        }
        return PhaseShieldStatus.ABSENT;
    }

    public static void syncState(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        PhaseShieldStatus status = displayStatus(player);
        ItemStack active = findPhaseShield(player);
        float charge = active.isEmpty() ? curioCharge(player) : chargeRatio(active);
        String key = status.name() + ':' + Math.round(charge * 100.0F);
        PhaseShieldStatus previousStatus = LAST_SENT.put(serverPlayer, status);
        String previousKey = LAST_SENT_KEY.put(serverPlayer, key);
        if (previousStatus == status && key.equals(previousKey)) return;
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                new S2CPhaseShieldState(player.getUUID(), status, charge));
    }

    private static void forceSync(Player player) {
        LAST_SENT.remove(player);
        syncState(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { forceSync(event.getEntity()); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) { forceSync(event.getEntity()); }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) { forceSync(event.getEntity()); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { LAST_SENT.remove(event.getEntity()); }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof Player target) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer),
                    new S2CPhaseShieldState(target.getUUID(), displayStatus(target), currentCharge(target)));
        }
    }

    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof Player target) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer),
                    new S2CPhaseShieldState(target.getUUID(), PhaseShieldStatus.ABSENT, 0.0F));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !event.player.level().isClientSide) {
            if (displayStatus(event.player) == PhaseShieldStatus.ACTIVE && event.player.isOnFire()) {
                event.player.clearFire();
            }
            syncState(event.player);
        }
    }

    @SubscribeEvent
    public static void onNegativeEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getEffectInstance() == null
                || event.getEffectInstance().getEffect().isBeneficial()) return;
        ItemStack shield = activeShieldForEffects(player);
        if (shield.isEmpty()) return;
        int cost = (event.getEffectInstance().getAmplifier() + 1) * 5;
        if (PhaseShield.capacitorEnergy(shield) < cost) return;
        PhaseShield.drainCapacitor(shield, cost);
        ShieldCluster cluster = PhaseShield.getOrCreateCluster(shield);
        TechItemData.saveShieldCluster(shield, cluster);
        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        forceSync(player);
    }

    private static ItemStack activeShieldForEffects(ServerPlayer player) {
        ItemStack held = findPhaseShield(player);
        if (!held.isEmpty() && !isLegacyProtected(player)) return held;
        if (!held.isEmpty()) return ItemStack.EMPTY;
        for (SlotResult slot : CuriosFinder.findCurios(player,
                stack -> stack.getItem() instanceof PhaseShield)) {
            if (PhaseShield.getEnergy(slot.stack()) >= PhaseShield.MIN_USE_ENERGY)
                return slot.stack();
        }
        return ItemStack.EMPTY;
    }
    
    @SubscribeEvent
    public static void onPhaseShieldHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.isCanceled() || isLegacyProtected(player)) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(DamageTypeTags.BYPASSES_SHIELD)) return;
        float original = event.getAmount();
        if (!Float.isFinite(original) || original <= 0.0F) return;
        java.util.ArrayList<ItemStack> shields = new java.util.ArrayList<>();
        ItemStack held = findPhaseShield(player);
        if (!held.isEmpty()) shields.add(held);
        for (SlotResult slot : CuriosFinder.findCurios(player,
                stack -> stack.getItem() instanceof PhaseShield)) {
            shields.add(slot.stack());
        }
        if (shields.isEmpty()) return;
        TechHitSnapshot snapshot = TechHitSnapshot.capture(player, source);
        TestDummy.DamageCategory category = TestDummy.DamageCategory.byIndex(
                snapshot.classification().categoryIndex());
        java.util.Set<ItemStack> settled = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        float remaining = original;
        float spent = 0.0F;
        int participating = 0;
        for (ItemStack stack : shields) {
            if (remaining <= 0.0F) break;
            if (!settled.add(stack) || PhaseShield.getEnergy(stack) < PhaseShield.MIN_USE_ENERGY) continue;
            ShieldCluster cluster = PhaseShield.getOrCreateCluster(stack);
            float multiplier = PhaseShield.energyMultiplier(stack);
            if (!Float.isFinite(multiplier) || multiplier <= 0.0F) continue;
            ShieldResolution result = cluster.resolvePowered(new ShieldDamageContext(
                    category, remaining, snapshot.classification().bypassesShield(), snapshot.shieldStrength()),
                    PhaseShield.capacitorEnergy(stack) / multiplier);
            if (!result.absorbedAny()) continue;
            PhaseShield.drainCapacitor(stack, result.shieldEnergySpent() * multiplier);
            TechItemData.saveShieldCluster(stack, cluster);
            remaining = result.remainingDamage();
            spent += result.shieldEnergySpent();
            participating++;
        }
        if (participating == 0) return;
        event.setAmount(remaining);
        playAbsorb(player, new ShieldResolution(original, original - remaining, remaining, spent, participating, 0));
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        
        
    }

    private static float currentCharge(Player player) {
        ItemStack held = findPhaseShield(player);
        return held.isEmpty() ? curioCharge(player) : chargeRatio(held);
    }

    private static float chargeRatio(ItemStack stack) {
        return PhaseShield.getEnergy(stack) / Math.max(1.0F, PhaseShield.getMaxEnergy(stack));
    }

    private static float curioCharge(Player player) {
        List<SlotResult> shields = CuriosFinder.findCurios(player, stack -> stack.getItem() instanceof PhaseShield);
        return shields.stream().map(SlotResult::stack).mapToDouble(PhaseShieldHandler::chargeRatio).max().orElse(0.0D) > 0.0D
                ? (float) shields.stream().map(SlotResult::stack).mapToDouble(PhaseShieldHandler::chargeRatio).max().orElse(0.0D) : 0.0F;
    }

    private static boolean isHoldingPhaseShield(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof PhaseShield;
    }

    private static boolean isLegacyProtected(Player player) {
        return com.huige233.transcend.items.TranscendShield.hasTranscendShield(player)
                || com.huige233.transcend.util.ArmorUtils.fullEquipped(player)
                || CuriosFinder.hasCurio(player, com.huige233.transcend.init.ModItems.transcend_curio.get());
    }
    private static ItemStack findPhaseShield(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof PhaseShield && player.isUsingItem()
                    && player.getUsedItemHand() == hand) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void playBroken(Player player) {
        if (player.level() instanceof ServerLevel level) {
            ServerVisualBroadcaster.shockwave(level, player.position(), 0.8F, 1.0F, 0.3F, 0.3F, 8);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    private static void playAbsorb(Player player, ShieldResolution result) {
        if (player.level() instanceof ServerLevel level) {
            float intensity = result.fullyAbsorbed() ? 1.0F : 1.25F;
            ServerVisualBroadcaster.shieldRipple(level, player.position(), 0.9F * intensity,
                    0.25F, 0.85F, 1.0F, 10);
            ServerVisualBroadcaster.shockwave(level, player.position(), 0.35F * intensity,
                    0.35F, 0.9F, 1.0F, 6);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.65F, result.fullyAbsorbed() ? 1.25F : 1.05F);
        }
    }
}
