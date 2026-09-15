package com.huige233.transcend.network;

import com.huige233.transcend.entity.TestDummy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

   
                
                                               
                                       
                                                        
                                  
                                          
                                             
                                                 
                                       
   
/** 经距离、所有权和数值校验后在服务端修改假人属性、护盾、效果与回血设置或清理目标。 */
public class C2STestDummySettingsPack {

    private final int entityId;
    private final int action;
    private final int value;
    
    private final String str;

    public C2STestDummySettingsPack(int entityId, int action, int value) {
        this(entityId, action, value, "");
    }

    public C2STestDummySettingsPack(int entityId, int action, int value, String str) {
        this.entityId = entityId;
        this.action = action;
        this.value = value;
        this.str = str == null ? "" : str;
    }

    public C2STestDummySettingsPack(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.action = buf.readByte();
        this.value = buf.readInt();
        this.str = buf.readUtf(256);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(action);
        buf.writeInt(value);
        buf.writeUtf(str, 256);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Entity entity = player.serverLevel().getEntity(entityId);
            if (!(entity instanceof TestDummy dummy)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§c[假人] 设置失败：目标不存在或已移除"), true);
                return;
            }
            double distance = player.distanceTo(dummy);
            
            if (!validateRequest(entityId, dummy.getId(), distance, action)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§c[假人] 请求被拒：距离 %.1f 格（需 ≤10）", distance)), true);
                return;
            }
            if (!isValidValue(action, value)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§c[假人] 请求被拒：数值越界 (action=" + action + ", value=" + value + ")"), true);
                return;
            }
            if (!dummy.canConfigure(player)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§c[假人] 无权限：该假人属于其他玩家"), true);
                return;
            }

            switch (action) {
                case 0 -> dummy.resetData();
                case 1 -> dummy.toggleAnnounce();
                case 2 -> {
                    var armorAttr = dummy.getAttribute(Attributes.ARMOR);
                    if (armorAttr != null) armorAttr.setBaseValue(value);
                }
                case 3 -> dummy.setResistanceLevel(value);
                case 4 -> dummy.discard();
                case 6 -> dummy.setNoKnockback(value != 0);
                
                case 7 -> dummy.setCategoryResistance(value >> 16, value & 0xFFFF);
                case 8 -> {
                    boolean ok = dummy.applyBuff(str, value);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            ok ? "§a[TestDummy] buff 已添加: " + str + " " + (value + 1)
                               : "§c[TestDummy] 未找到效果: " + str), true);
                }
                case 9 -> {
                    boolean ok = dummy.removeBuff(str);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            ok ? "§a[TestDummy] buff 已移除: " + str
                               : "§c[TestDummy] 移除失败（假人没有该效果或 ID 无效）: " + str), true);
                }
                case 10 -> {
                    dummy.clearBuffs();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§a[TestDummy] 全部 buff 已清空"), true);
                }
                
                case 11 -> dummy.setHitEffectEnabled(value != 0);
                
                case 12 -> dummy.setKind(value);
                
                case 13 -> dummy.setAnnounceReduce(value != 0);
                
                case 14 -> {
                    dummy.setShieldMax(value);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            value <= 0 ? "§b[护盾] §7已关闭"
                                    : "§b[护盾] §7上限: " + value + " §8(当前 " + (int) dummy.getShieldValue() + ")"), true);
                }
                
                case 15 -> {
                    dummy.setShieldRegenStep(value);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§b[护盾] 回复档位: " + value + " §7(" + (value * 5) + "%/s)"), true);
                }
                
                case 16 -> dummy.setShieldRegenOnHit(value != 0);
                
                case 17 -> dummy.setShieldAffectedByResist(value != 0);
                
                case 18 -> {
                    int cat = value >> 16;
                    int tough = value & 0xFFFF;
                    dummy.setShieldToughness(cat, tough);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§b[护盾] §7韧性[%d]: §f%d §8(1点盾抵%d点伤害)", cat, tough, tough)), true);
                }
                
                case 19 -> {
                    dummy.setShieldRegenDelay(value);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§b[护盾] §7回充延迟: §f%d§7t §8(%.1fs)", value, value / 20.0F)), true);
                }
                
                case 20 -> {
                    dummy.setShieldRegenOnHitPercent(value);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§b[护盾] §7受击回盾: §f%d%%", value)), true);
                }
                
                
                case 21 -> {
                    dummy.setHealMode(value);
                    String[] modes = {"立即回满", "间隔回满(3s)", "低血量回满", "不回血"};
                    int idx = Math.max(0, Math.min(modes.length - 1, value));
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§a[假人] 回血模式: " + modes[idx]), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static boolean validateRequest(int requestedEntityId, int resolvedEntityId,
                                          double distance, int action) {
        return requestedEntityId == resolvedEntityId
                && Double.isFinite(distance)
                && distance >= 0.0
                && distance <= 10.0
                && action >= 0
                && action <= 21;
    }

    private static boolean isValidValue(int action, int value) {
        return switch (action) {
            case 2 -> value >= 0 && value <= 30;
            case 3 -> value >= 0 && value <= 100;
            case 6 -> value == 0 || value == 1;
            
            case 7 -> (value >>> 16) <= 5 && (value & 0xFFFF) <= 100;
            
            case 8 -> value >= 0 && value <= 255;
            case 11 -> value == 0 || value == 1;
            case 12 -> value >= 0 && value <= 4;
            
            case 14 -> value >= 0;
            case 15 -> value >= 1 && value <= 20;
            case 16 -> value == 0 || value == 1;
            case 17 -> value == 0 || value == 1;
            
            case 18 -> (value >>> 16) <= 5 && (value & 0xFFFF) >= 1 && (value & 0xFFFF) <= 100;
            
            case 19 -> value >= 0 && value <= 600;
            
            case 20 -> value >= 0 && value <= 100;
            
            case 21 -> value >= 0 && value <= 3;
            default -> value == 0;
        };
    }
}
