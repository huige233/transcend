package com.mega.uom.common.command.entity;

import com.mega.uom.util.entity.EntityActuallyHurt;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

public class ForceSetHealthCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("forceSetHealth")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                                .executes((p_137810_) -> set(p_137810_.getSource(), EntityArgument.getEntities(p_137810_, "targets"), FloatArgumentType.getFloat(p_137810_, "value")))
                        )
                );
    }

    private static int set(CommandSourceStack p_137814_, Collection<? extends Entity> p_137815_, float value) {
        for (Entity entity : p_137815_) {
            if (entity instanceof LivingEntity living)
                EntityActuallyHurt.catchSetTrueHealth(living, value);

        }

        if (p_137815_.size() == 1) {
            p_137814_.sendSuccess(() -> Component.translatable("commands.fantasy_ending.force_set_health.success.single", p_137815_.iterator().next().getDisplayName()), true);
        } else {
            p_137814_.sendSuccess(() -> Component.translatable("commands.fantasy_ending.force_set_health.success.multiple", p_137815_.size()), true);
        }

        return p_137815_.size();
    }
}
