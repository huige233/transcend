package com.huige233.transcend.network;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.items.TranscendWand;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.config.SpellConfigurationRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class C2SWandCardEdit {
    private final int inventorySlot;
    private final int spellSlot;
    private final SpellCarrier carrier;
    private final SpellElement element;
    private final List<SpellEffect> effects;
    private final boolean validPayload;

    public C2SWandCardEdit(int inventorySlot, int spellSlot, SpellCarrier carrier,
                           SpellElement element, List<SpellEffect> effects) {
        this.inventorySlot = inventorySlot;
        this.spellSlot = spellSlot;
        this.carrier = carrier;
        this.element = element;
        this.effects = List.copyOf(effects);
        this.validPayload = carrier != null && element != null && effects.size() <= 7;
    }

    public C2SWandCardEdit(FriendlyByteBuf buf) {
        inventorySlot = buf.readVarInt();
        spellSlot = buf.readVarInt();
        String carrierId = buf.readUtf(32);
        carrier = SpellCarrier.getById(carrierId);
        String elementId = buf.readUtf(32);
        SpellElement decodedElement = SpellElement.getById(elementId);
        element = decodedElement == null ? null : decodedElement.canonical();
        int encodedCount = buf.readVarInt();
        int count = Math.min(7, Math.max(0, encodedCount));
        boolean decodedValid = carrier != null && element != null
                && carrier.id.equals(carrierId) && element.id.equals(elementId)
                && encodedCount >= 0 && encodedCount <= 7;
        List<SpellEffect> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String effectId = buf.readUtf(32);
            SpellEffect effect = SpellEffect.getById(effectId);
            if (effect != null && effect.id.equals(effectId)) decoded.add(effect);
            else decodedValid = false;
        }
        effects = List.copyOf(decoded);
        validPayload = decodedValid;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(inventorySlot);
        buf.writeVarInt(spellSlot);
        buf.writeUtf(carrier.id, 32);
        buf.writeUtf(element.id, 32);
        buf.writeVarInt(Math.min(7, effects.size()));
        for (int i = 0; i < Math.min(7, effects.size()); i++) buf.writeUtf(effects.get(i).id, 32);
    }

    public void run(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> apply(context.getSender()));
        context.setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        if (!validPayload || player == null || inventorySlot < 0
                || inventorySlot >= player.getInventory().getContainerSize()) return;
        ItemStack wand = player.getInventory().getItem(inventorySlot);
        if (!(wand.getItem() instanceof TranscendWand)) return;

        CompoundTag wandTag = wand.getOrCreateTag();
        int maxWandSlots = Math.max(1, wandTag.getInt("max_slots"));
        if (spellSlot < 0 || spellSlot >= maxWandSlots) return;

        PlayerAscensionData data = AscensionCapability.get(player);
        if (effects.size() > 7 || effects.size() > data.getMaxSpellEffectSlots()) return;

        EnumMap<SpellEffect, Integer> counts = new EnumMap<>(SpellEffect.class);
        for (SpellEffect effect : effects) {
            if (!SpellConfigurationRules.isEffectCompatible(carrier, effect)) return;
            int count = counts.merge(effect, 1, Integer::sum);
            if (count > 1 && !effect.isRepeatable()) return;
        }
        SpellConfigurationRules.Computation computation = SpellConfigurationRules.compute(carrier, element, effects);
        if (computation.manaCost() == Integer.MAX_VALUE || computation.tier() > data.getSpellTier()) return;

        ListTag slots = wandTag.getList("wand_slots", Tag.TAG_COMPOUND);
        if (slots.size() != maxWandSlots) return;
        CompoundTag old = slots.getCompound(spellSlot);
        CompoundTag configured = new CompoundTag();
        configured.putString("carrier", carrier.id);
        configured.putString("element", element.canonical().id);
        configured.putString("effect", effects.isEmpty() ? "" : effects.get(0).id);
        ListTag effectTags = new ListTag();
        for (SpellEffect effect : effects) effectTags.add(StringTag.valueOf(effect.id));
        configured.put("effects", effectTags);
        configured.putInt("spell_tier", computation.tier());
        configured.putFloat("base_power", 1.0F);
        configured.putFloat("base_cooldown", 1.0F);
        configured.putInt("spell_level", Math.max(1, old.getInt("spell_level")));
        configured.putInt("spell_xp", Math.max(0, old.getInt("spell_xp")));
        slots.set(spellSlot, configured);
        wandTag.put("wand_slots", slots);
    }
}
