package com.huige233.transcend.network;

import com.huige233.transcend.client.ChunkManaMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CChunkManaMapPack {

    public static final int MAX_RADIUS = 8;

    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final String dimensionName;
    private final float[] mana;
    private final byte[] tier;
    private final boolean[] stabilized;

    public S2CChunkManaMapPack(int centerX, int centerZ, int radius, String dimensionName,
                               float[] mana, byte[] tier, boolean[] stabilized) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = Math.max(1, Math.min(MAX_RADIUS, radius));
        this.dimensionName = dimensionName == null ? "" : dimensionName;
        this.mana = mana;
        this.tier = tier;
        this.stabilized = stabilized;
    }

    public S2CChunkManaMapPack(FriendlyByteBuf buf) {
        this.centerX = buf.readInt();
        this.centerZ = buf.readInt();
        int r = buf.readVarInt();
        this.radius = Math.max(1, Math.min(MAX_RADIUS, r));
        this.dimensionName = buf.readUtf(128);
        int n = (2 * radius + 1) * (2 * radius + 1);
        this.mana = new float[n];
        this.tier = new byte[n];
        this.stabilized = new boolean[n];
        for (int i = 0; i < n; i++) {
            mana[i] = buf.readFloat();
            tier[i] = buf.readByte();
            stabilized[i] = buf.readBoolean();
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(centerX);
        buf.writeInt(centerZ);
        buf.writeVarInt(radius);
        buf.writeUtf(dimensionName, 128);
        int n = (2 * radius + 1) * (2 * radius + 1);
        for (int i = 0; i < n; i++) {
            buf.writeFloat(mana[i]);
            buf.writeByte(tier[i]);
            buf.writeBoolean(stabilized[i]);
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::openScreen));
        ctx.setPacketHandled(true);
    }

    private void openScreen() {
        Minecraft.getInstance().setScreen(new ChunkManaMapScreen(
                centerX, centerZ, radius, dimensionName, mana, tier, stabilized));
    }
}
