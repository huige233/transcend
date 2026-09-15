package com.huige233.transcend.network;

import com.huige233.transcend.menu.ResearchStationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;


/** 向匹配的客户端研究站菜单同步研究进度、能量、转换速率、研究点和研究者快照。 */
public final class S2CResearchStationPacket {
    private final int containerId; private final BlockPos pos; private final CompoundTag progress;
    private final long energy, rate, remainder; private final String points; private final UUID researcher;
    public S2CResearchStationPacket(int id, BlockPos pos, CompoundTag progress,long energy,long rate,String points,long remainder,UUID researcher){this.containerId=id;this.pos=pos;this.progress=progress;this.energy=energy;this.rate=rate;this.points=points;this.remainder=remainder;this.researcher=researcher;}
    public S2CResearchStationPacket(FriendlyByteBuf b){containerId=b.readVarInt();pos=b.readBlockPos();progress=b.readNbt();energy=b.readLong();rate=b.readLong();points=b.readUtf(128);remainder=b.readLong();researcher=b.readBoolean()?b.readUUID():null;}
    public void write(FriendlyByteBuf b){b.writeVarInt(containerId);b.writeBlockPos(pos);b.writeNbt(progress);b.writeLong(energy);b.writeLong(rate);b.writeUtf(points,128);b.writeLong(remainder);b.writeBoolean(researcher!=null);if(researcher!=null)b.writeUUID(researcher);}
    public void run(Supplier<NetworkEvent.Context> sup){var c=sup.get();c.enqueueWork(()-> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->{var pl=Minecraft.getInstance().player;if(pl!=null&&pl.containerMenu instanceof ResearchStationMenu m&&m.containerId==containerId&&m.station()!=null&&m.station().getBlockPos().equals(pos))m.setClientState(new ResearchStationMenu.CompoundState(progress,energy,rate,points,remainder,researcher,pos));}));c.setPacketHandled(true);}
}
