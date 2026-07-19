package com.huige233.transcend.network;

import com.huige233.transcend.ascension.tree.TreeDefinition;
import com.huige233.transcend.ascension.tree.TreeRegistry;
import com.huige233.transcend.client.ClientTreeCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class S2CTreeSync {

    private final List<TreeDefinition> trees;

    public S2CTreeSync(Collection<TreeDefinition> trees) {
        this.trees = new ArrayList<>(trees);
    }

    public S2CTreeSync(FriendlyByteBuf buf) {
        int count = buf.readInt();
        trees = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            trees.add(TreeDefinition.read(buf));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(trees.size());
        for (TreeDefinition tree : trees) {
            tree.write(buf);
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientTreeCache.load(trees);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
