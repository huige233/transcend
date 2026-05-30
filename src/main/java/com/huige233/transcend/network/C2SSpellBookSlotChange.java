package com.huige233.transcend.network;

import com.huige233.transcend.items.SpellBookItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * R78: 法术书 wheel slot 切换 — 客户端 → 服务端。
 *
 * <p>触发：玩家手持 SpellBookItem + 按住 Shift + 滚动鼠标滚轮 →
 * 由 {@link com.huige233.transcend.client.SpellBookScrollHandler} 捕获 →
 * 发此 C2S 包让服务端权威地修改主/副手 SpellBookItem 的 active_slot NBT。
 *
 * <p>delta：+1 = 向后一个，-1 = 向前一个。其它值会按符号收敛到 ±1。
 *
 * <p>反馈：服务端切换成功 → 玩家收到 actionbar 提示（spellbook.transcend.switched）+ 翻页音效。
 */
public class C2SSpellBookSlotChange {

    private final int delta;

    public C2SSpellBookSlotChange(int delta) {
        this.delta = delta;
    }

    public C2SSpellBookSlotChange(FriendlyByteBuf buf) {
        this.delta = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(delta);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;

            // 优先主手；主手不是法术书则尝试副手
            ItemStack stack = sp.getMainHandItem();
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (!(stack.getItem() instanceof SpellBookItem)) {
                stack = sp.getOffhandItem();
                hand = InteractionHand.OFF_HAND;
                if (!(stack.getItem() instanceof SpellBookItem)) return;
            }
            SpellBookItem book = (SpellBookItem) stack.getItem();

            int next = book.cycleActiveSlot(stack, delta);
            if (next < 0) {
                // 空书 — 静默返回（客户端不应触发到这里，但容错）
                return;
            }

            int used = book.getUsedSlots(stack);
            CompoundTag slotData = book.getSlotData(stack, next);
            String summary = slotData != null
                    ? String.format("%s/%s%s",
                        slotData.getString("carrier"),
                        slotData.getString("element"),
                        slotData.getString("effect").isEmpty()
                                ? ""
                                : "+" + slotData.getString("effect"))
                    : "?";
            sp.displayClientMessage(
                    Component.translatable("spellbook.transcend.switched",
                            next + 1, used, summary)
                            .withStyle(ChatFormatting.AQUA), true);
            sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.5F, 1.2F);
        });
        ctx.get().setPacketHandled(true);
    }
}
