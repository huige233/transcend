package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 锻造盾牌效果处理。 */
public class ForgeShieldHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        var forge = BalanceConfig.get().forge;
        if (!forge.ward_shield_enabled) return;
        if (forge.ward_shield_regen_interval <= 0) return;
        if (player.tickCount % forge.ward_shield_regen_interval != 0) return;

        int wardSockets = countWardSockets(player);
        if (wardSockets <= 0) return;

        float cap = wardSockets * forge.ward_shield_per_socket;
        float current = player.getAbsorptionAmount();
        if (current >= cap) return;

        float regen = Math.max(0.0f, forge.ward_shield_regen_amount);
        float newAmount = Math.min(cap, current + regen);
        if (newAmount <= current) return;

        player.setAbsorptionAmount(newAmount);

        if (player.level() instanceof ServerLevel serverLevel) {
            ItemStack themedArmor = pickAnyForgedArmor(player);
            if (!themedArmor.isEmpty()) {
                ForgeVisualEffects.spawnDefenseAura(serverLevel, player, themedArmor);
            }
        }
    }

    private static int countWardSockets(Player player) {
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            if (!GearForgeData.isInPipeline(armor)) continue;
            if (GearCategory.classify(armor) != GearCategory.ARMOR) continue;
            for (GearForgeData.ResonanceSocket socket : GearForgeData.getSockets(armor)) {
                if (ResonanceKind.WARD.id.equals(socket.crystalId())) total++;
            }
        }
        return total;
    }

    private static ItemStack pickAnyForgedArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty()
                    && GearForgeData.isInPipeline(armor)
                    && GearCategory.classify(armor) == GearCategory.ARMOR) {
                return armor;
            }
        }
        return ItemStack.EMPTY;
    }
}
