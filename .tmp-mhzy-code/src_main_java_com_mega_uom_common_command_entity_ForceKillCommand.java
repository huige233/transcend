package com.mega.uom.common.command.entity;

import com.google.common.collect.ImmutableList;
import com.mega.uom.util.entity.EntityActuallyHurt;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Collection;

public class ForceKillCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("forceKill")
                .requires(cs -> cs.hasPermission(2))
                .executes((p_137817_) -> kill(p_137817_.getSource(), ImmutableList.of(p_137817_.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes((p_137810_) -> kill(p_137810_.getSource(), EntityArgument.getEntities(p_137810_, "targets")))
                );
    }

    private static int kill(CommandSourceStack p_137814_, Collection<? extends Entity> p_137815_) {
        for (Entity entity : p_137815_) {
            if (entity instanceof LivingEntity living)
                new EntityActuallyHurt(living).actuallyHurt(living.level().damageSources().generic(), Float.POSITIVE_INFINITY, true);
            entity.gameEvent(GameEvent.ENTITY_DIE);
        }

        if (p_137815_.size() == 1) {
            p_137814_.sendSuccess(() -> Component.translatable("commands.kill.success.single", p_137815_.iterator().next().getDisplayName()), true);
        } else {
            p_137814_.sendSuccess(() -> Component.translatable("commands.kill.success.multiple", p_137815_.size()), true);
        }

        return p_137815_.size();
    }
}
