package com.huige233.transcend.network;

import com.huige233.transcend.ascension.*;
import com.huige233.transcend.ascension.resource.ClassResourceHandler;
import com.huige233.transcend.ascension.tree.NodeDefinition;
import com.huige233.transcend.ascension.tree.TreeRegistry;
import com.huige233.transcend.spell.MagicCrystalHelper;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端→服务端飞升操作包。 */
public class C2SAscensionAction {
    private static final String AURA_GUARD_COOLDOWN_TAG = "transcend_aura_guard_cooldown_until";
    private static final int AURA_GUARD_COOLDOWN_TICKS = 40;
    private static final int AURA_GUARD_ACTIVATION_MANA = 2;
    private static final String CLASS_SKILL_COOLDOWN_TAG = "transcend_class_skill_cooldown_until";
    private static final String CLASS_SKILL_FAILURE_MESSAGE_TAG = "transcend_class_skill_failure_message_after";
    private static final int CLASS_SKILL_COOLDOWN_TICKS = 20;
    private static final int CLASS_SKILL_FAILURE_MESSAGE_INTERVAL = 20;

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
                    if (data.getRitualTier() < 1) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.class_requires_ritual_tier").withStyle(ChatFormatting.RED));
                        break;
                    }
                    if (!data.selectClass(mc)) break;
                    AscensionHandler.applyPersistentStats(player, data);
                    player.sendSystemMessage(Component.translatable(
                            "msg.transcend.class_selected", mc.getDisplayName())
                            .withStyle(ChatFormatting.GREEN));
                }

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
                    if (data.getRitualTier() < stage) {
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

                case 7 -> {
                    if (!TribulationManager.start(player)) {
                        player.sendSystemMessage(Component.translatable(
                                "command.transcend.tribulation.start_failed").withStyle(ChatFormatting.RED));
                    }
                }
                case 8 -> {
                    long now = player.level().getGameTime();
                    long cooldownUntil = player.getPersistentData().getLong(AURA_GUARD_COOLDOWN_TAG);
                    if (now < cooldownUntil) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.aura_guard.cooldown").withStyle(ChatFormatting.RED));
                        break;
                    }
                    if (data.hasCultivationDeviation() || data.isTribulationActive()) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.aura_guard.blocked").withStyle(ChatFormatting.RED));
                        break;
                    }
                    boolean enabling = !data.isAuraGuardEnabled();
                    if (enabling && !MagicCrystalHelper.hasEnoughMana(player, AURA_GUARD_ACTIVATION_MANA)) {
                        player.sendSystemMessage(Component.translatable(
                                "msg.transcend.aura_guard.no_mana", AURA_GUARD_ACTIVATION_MANA)
                                .withStyle(ChatFormatting.RED));
                        break;
                    }
                    if (enabling) MagicCrystalHelper.consumeMana(player, AURA_GUARD_ACTIVATION_MANA);
                    data.setAuraGuardEnabled(enabling);
                    player.getPersistentData().putLong(AURA_GUARD_COOLDOWN_TAG,
                            now + AURA_GUARD_COOLDOWN_TICKS);
                    player.sendSystemMessage(Component.translatable(data.isAuraGuardEnabled()
                            ? "msg.transcend.aura_guard.enabled"
                            : "msg.transcend.aura_guard.disabled"));
                }
                case 9 -> {
                    long now = player.level().getGameTime();
                    long cooldownUntil = player.getPersistentData().getLong(CLASS_SKILL_COOLDOWN_TAG);
                    if (now < cooldownUntil) {
                        sendClassSkillFailure(player, "msg.transcend.class_skill.cooldown", now);
                    } else if (ClassResourceHandler.activateClassSkill(player)) {
                        player.getPersistentData().putLong(
                                CLASS_SKILL_COOLDOWN_TAG, now + CLASS_SKILL_COOLDOWN_TICKS);
                    } else {
                        sendClassSkillFailure(player, "msg.transcend.class_skill.failed", now);
                    }
                }
            }

            AscensionHandler.syncToClient(player, data);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void sendClassSkillFailure(ServerPlayer player, String translationKey, long now) {
        long messageAfter = player.getPersistentData().getLong(CLASS_SKILL_FAILURE_MESSAGE_TAG);
        if (now < messageAfter) return;
        player.sendSystemMessage(Component.translatable(translationKey).withStyle(ChatFormatting.RED));
        player.getPersistentData().putLong(
                CLASS_SKILL_FAILURE_MESSAGE_TAG, now + CLASS_SKILL_FAILURE_MESSAGE_INTERVAL);
    }
}
