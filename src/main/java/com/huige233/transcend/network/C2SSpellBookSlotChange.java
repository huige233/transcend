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
