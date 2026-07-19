package com.huige233.transcend.network;

import com.huige233.transcend.block.circle.CircleCoreMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CCircleStatus {

    private final BlockPos corePos;
    private final int tier;
    private final int storedMana;
    private final int maxMana;
    private final boolean active;
    private final boolean structureValid;
    private final String functionId;
    private final float upkeepPerMin;
    private final int settingsCount;

    private final boolean sigilLocked;
    private final int missingBlockCount;
    private final int catalystCount;
    private final int catalystSatisfiedCount;

    public S2CCircleStatus(BlockPos corePos, int tier, int storedMana, int maxMana,
                           boolean active, boolean structureValid,
                           String functionId, float upkeepPerMin, int settingsCount,
                           boolean sigilLocked, int missingBlockCount,
                           int catalystCount, int catalystSatisfiedCount) {
        this.corePos = corePos;
        this.tier = tier;
        this.storedMana = storedMana;
        this.maxMana = maxMana;
        this.active = active;
        this.structureValid = structureValid;
        this.functionId = functionId == null ? "" : functionId;
        this.upkeepPerMin = upkeepPerMin;
        this.settingsCount = Math.max(0, settingsCount);
        this.sigilLocked = sigilLocked;
        this.missingBlockCount = Math.max(0, missingBlockCount);
        this.catalystCount = Math.max(0, catalystCount);
        this.catalystSatisfiedCount = Math.max(0, Math.min(catalystSatisfiedCount, this.catalystCount));
    }

    public S2CCircleStatus(BlockPos corePos, int tier, int storedMana, int maxMana,
                           boolean active, boolean structureValid,
                           String functionId, float upkeepPerMin, int settingsCount) {
        this(corePos, tier, storedMana, maxMana, active, structureValid,
                functionId, upkeepPerMin, settingsCount,
                false, 0, 0, 0);
    }

    public S2CCircleStatus(BlockPos corePos, int tier, int storedMana, int maxMana,
                           boolean active, boolean structureValid,
                           String functionId, float upkeepPerMin) {
        this(corePos, tier, storedMana, maxMana, active, structureValid,
                functionId, upkeepPerMin, 0,
                false, 0, 0, 0);
    }

    public S2CCircleStatus(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        this.tier = buf.readVarInt();
        this.storedMana = buf.readVarInt();
        this.maxMana = buf.readVarInt();
        this.active = buf.readBoolean();
        this.structureValid = buf.readBoolean();
        this.functionId = buf.readUtf();
        this.upkeepPerMin = buf.readFloat();
        this.settingsCount = buf.readVarInt();
        this.sigilLocked = buf.readBoolean();
        this.missingBlockCount = buf.readVarInt();
        this.catalystCount = buf.readVarInt();
        this.catalystSatisfiedCount = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(corePos);
        buf.writeVarInt(tier);
        buf.writeVarInt(storedMana);
        buf.writeVarInt(maxMana);
        buf.writeBoolean(active);
        buf.writeBoolean(structureValid);
        buf.writeUtf(functionId);
        buf.writeFloat(upkeepPerMin);
        buf.writeVarInt(settingsCount);
        buf.writeBoolean(sigilLocked);
        buf.writeVarInt(missingBlockCount);
        buf.writeVarInt(catalystCount);
        buf.writeVarInt(catalystSatisfiedCount);
    }

    public void run(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient));
        ctx.setPacketHandled(true);
    }

    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
        if (!(cs.getMenu() instanceof CircleCoreMenu menu)) return;

        if (!menu.getCorePos().equals(corePos)) return;

        menu.updateData(new CircleCoreMenu.CircleCoreData(
                tier, storedMana, maxMana, active, structureValid, functionId, upkeepPerMin,
                settingsCount, sigilLocked, missingBlockCount, catalystCount, catalystSatisfiedCount));
    }
}
