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

/**
 * R89: Ward 护盾 — 玩家穿戴已锻 ARMOR 且其上有 Ward socket 时，被动生成少量 absorption。
 *
 * <p>玩家请求："增加可选选项 允许生成少量的护盾"
 *
 * <h2>设计</h2>
 * <ul>
 *   <li><b>来源</b>：4 件 ARMOR 槽位中"已锻 ARMOR"上的 Ward socket 总数 W</li>
 *   <li><b>上限</b>：absorption cap = W × {@code ward_shield_per_socket}（默认 1.0 HP/socket）</li>
 *   <li><b>速率</b>：每 {@code ward_shield_regen_interval} server tick（默认 100 = 5s）+1 HP 直到达到上限</li>
 *   <li><b>不挤占其它来源</b>：仅在 current absorption &lt; cap 时 +1；超过 cap 时静默</li>
 *   <li><b>可关闭</b>：{@code BalanceConfig.forge.ward_shield_enabled = false}（运行时通过 data/transcend/balance/values.json 覆盖）</li>
 * </ul>
 *
 * <h2>典型场景</h2>
 * <pre>
 * 装备：4 件已锻护甲，每件 1× Ward = 4 个 socket
 * → cap = 4 × 1.0 = 4 HP 护盾
 * → 每 5 秒 +1 HP，4 个周期（20 秒）满盾
 * → 受击时优先消耗护盾；护盾耗尽后才扣本体 HP
 *
 * 极端：4 件已锻护甲全 4× Ward = 16 socket → 16 HP 护盾（满 8 颗黄心）
 * </pre>
 *
 * <p>性能保护：仅 ServerPlayer + 每 interval 检查 1 次 + 仅遍历 4 ARMOR 槽。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
        if (current >= cap) return;     // 已满（或被其它源高于上限）— 不动

        float regen = Math.max(0.0f, forge.ward_shield_regen_amount);
        float newAmount = Math.min(cap, current + regen);
        if (newAmount <= current) return; // 无变化

        player.setAbsorptionAmount(newAmount);

        // 凝聚特效：在玩家身上喷 6 颗主题色 glitter（用其中一件护甲的主题色）
        if (player.level() instanceof ServerLevel serverLevel) {
            ItemStack themedArmor = pickAnyForgedArmor(player);
            if (!themedArmor.isEmpty()) {
                ForgeVisualEffects.spawnDefenseAura(serverLevel, player, themedArmor);
            }
        }
    }

    /** 统计 4 个 ARMOR 槽位中已锻护甲上的 Ward socket 总数。 */
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

    /** 任找一件已锻护甲（按 chest > head > legs > feet 优先级）用于主题色。 */
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
