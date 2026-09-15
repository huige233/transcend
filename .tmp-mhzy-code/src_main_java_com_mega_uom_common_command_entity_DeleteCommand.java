package com.mega.uom.common.command.entity;

import com.mega.endinglib.mixin.accessor.AccessorServerLevel;
import com.mega.endinglib.util.time.TimeStopRandom;
import com.mega.uom.common.network.PacketHandler;
import com.mega.uom.common.network.s2c.entity.DeleteEntityPacket;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraftforge.entity.PartEntity;

import java.util.Collection;

public class DeleteCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("delete")
                .requires(cs -> cs.hasPermission(3))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes((p_137810_) -> delete(p_137810_.getSource(), EntityArgument.getEntities(p_137810_, "targets")))
                );
    }

    private static int delete(CommandSourceStack p_137814_, Collection<? extends Entity> p_137815_) {
        for (Entity entity : p_137815_) {
            if (entity instanceof ServerPlayer player) {
                player.connection.disconnect(Component.literal("Player Entity Deleted").withStyle(ChatFormatting.RED));
                continue;
            }
            PacketHandler.sendToAll(new DeleteEntityPacket(entity.getId()));
            Level level = entity.level();
            entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            entity.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            entity.checkDespawn();
            entity.setInvisible(true);
            entity.tickCount = 0;
            entity.hurtMarked = false;
            if (entity instanceof LivingEntity living) {
                living.activeEffects.clear();
                living.invulnerableTime = 0;
                living.deathTime = 19;
            }
            entity.setLevelCallback(EntityInLevelCallback.NULL);
            if (level instanceof ServerLevel serverLevel) {
                ((AccessorServerLevel) serverLevel).getEntityTickList().remove(entity);
            }
        }

        if (p_137815_.size() == 1) {
            p_137814_.sendSuccess(() -> Component.translatable("commands.fantasy_ending.delete.success.single", p_137815_.iterator().next().getDisplayName()), true);
        } else {
            p_137814_.sendSuccess(() -> Component.translatable("commands.fantasy_ending.delete.success.multiple", p_137815_.size()), true);
        }

        return p_137815_.size();
    }
}
