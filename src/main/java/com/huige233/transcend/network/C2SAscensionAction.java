package com.huige233.transcend.network;

import com.huige233.transcend.ascension.*;
import com.huige233.transcend.ascension.tree.NodeDefinition;
import com.huige233.transcend.ascension.tree.TreeRegistry;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：玩家飞升操作请求
 *
 * action 类型：
 *  0 = 选择职业       payload = MageClass.id
 *  1 = 解锁天赋节点   payload = NodeDefinition.id
 *  2 = 选择元素专精   payload = ElementMastery.id
 *  3 = 完成仪式       payload = AscensionRitual.name()
 *  4 = 洗点重置       payload = ""（需背包有洗点水）
 *  5 = 绑定誓约       payload = "stage|vowId" (e.g. "1|oath_of_focus")
 *  6 = 解除誓约       payload = "stage"
 */
public class C2SAscensionAction {

    private final int action;
    private final String payload;

    public C2SAscensionAction(int action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    public C2SAscensionAction(FriendlyByteBuf buf) {
        this.action = buf.readByte();
        this.payload = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(action);
        buf.writeUtf(payload);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerAscensionData data = AscensionCapability.get(player);

            switch (action) {
                // ── 0: 选择职业 ────────────────────────────────────────────
                case 0 -> {
                    MageClass mc = MageClass.getById(payload);
                    if (!mc.isSelected()) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.invalid_class").withStyle(ChatFormatting.RED));
                        break;
                    }
                    if (data.hasSelectedClass()) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.class_already_selected").withStyle(ChatFormatting.RED));
                        break;
                    }
                    data.selectClass(mc);
                    player.sendSystemMessage(Component.translatable(
                            "msg.transcend.class_selected", mc.getDisplayName())
                            .withStyle(ChatFormatting.GREEN));
                }

                // ── 1: 解锁天赋节点 ────────────────────────────────────────
                case 1 -> {
                    NodeDefinition node = TreeRegistry.getInstance().getNode(payload);
                    if (node == null) break;
                    boolean ok = data.tryUnlockNode(payload);
                    if (!ok) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.node_unlock_failed").withStyle(ChatFormatting.RED));
                    } else {
                        AscensionHandler.applyPersistentStats(player, data);
                    }
                }

                // ── 2: (已移除 - 专精由职业自动决定) ──────────────────────

                // ── 3: 完成仪式 ────────────────────────────────────────────
                case 3 -> {
                    AscensionRitual ritual = null;
                    try { ritual = AscensionRitual.valueOf(payload); }
                    catch (IllegalArgumentException ignored) {}
                    if (ritual == null) break;

                    if (!ritual.hasItems(player)) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.ritual_missing_items",
                                ritual.requiredItemCount,
                                ritual.requiredItem.get().getDescription().getString())
                                .withStyle(ChatFormatting.RED));
                        break;
                    }

                    boolean ok = AscensionHandler.tryCompleteRitual(player, data, ritual);
                    if (!ok) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.ritual_not_ready").withStyle(ChatFormatting.RED));
                    } else {
                        ritual.consumeItems(player);
                    }
                }

                // ── 4: 洗点重置 ────────────────────────────────────────────
                case 4 -> {
                    net.minecraft.world.item.Item respecItem = ModItems.respec_potion.get();
                    boolean hasItem = player.isCreative();
                    int slot = -1;
                    if (!hasItem) {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            if (player.getInventory().getItem(i).getItem() == respecItem) {
                                slot = i;
                                hasItem = true;
                                break;
                            }
                        }
                    }
                    if (!hasItem) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.respec_no_potion").withStyle(ChatFormatting.RED));
                        break;
                    }
                    if (data.getUnlockedNodes().isEmpty() && !data.hasMastery()) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.respec_nothing").withStyle(ChatFormatting.GRAY));
                        break;
                    }
                    if (!player.isCreative() && slot >= 0) {
                        player.getInventory().getItem(slot).shrink(1);
                    }
                    int refund = data.respec();
                    AscensionHandler.applyPersistentStats(player, data);
                    player.sendSystemMessage(Component.translatable(
                            "msg.transcend.respec_done", refund).withStyle(ChatFormatting.GOLD));
                }

                // ── 5: 绑定誓约 ────────────────────────────────────────────
                case 5 -> {
                    String[] parts = payload.split("\\|", 2);
                    if (parts.length != 2) break;
                    int stage;
                    try { stage = Integer.parseInt(parts[0]); }
                    catch (NumberFormatException e) { break; }
                    String vowId = parts[1];

                    AscensionVow vow = VowRegistry.get(vowId);
                    if (vow == null || vow.getStage() != stage) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.vow_invalid").withStyle(ChatFormatting.RED));
                        break;
                    }
                    if (data.getStage() < stage) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.vow_locked", stage).withStyle(ChatFormatting.RED));
                        break;
                    }
                    data.setVowForStage(stage, vowId);
                    AscensionHandler.applyPersistentStats(player, data);
                    player.sendSystemMessage(Component.translatable(
                            "msg.transcend.vow_bound",
                            Component.translatable(vow.getTranslationKey()))
                            .withStyle(ChatFormatting.GOLD));
                }

                // ── 6: 解除誓约 ────────────────────────────────────────────
                case 6 -> {
                    int stage;
                    try { stage = Integer.parseInt(payload); }
                    catch (NumberFormatException e) { break; }
                    if (stage < 1 || stage > 4) break;
                    data.setVowForStage(stage, "");
                    AscensionHandler.applyPersistentStats(player, data);
                    player.sendSystemMessage(Component.translatable(
                            "msg.transcend.vow_cleared", stage).withStyle(ChatFormatting.YELLOW));
                }
            }

            AscensionHandler.syncToClient(player, data);
        });
        ctx.get().setPacketHandled(true);
    }
}
