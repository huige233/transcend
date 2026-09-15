package com.mega.uom.common.command.player;

import com.mega.uom.util.entity.PlayerInvulnerableEntityData;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;

public class SetInvulnerablePlayerCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("setInvulnerable").requires((p_138087_) -> p_138087_.hasPermission(3))
                .executes(context -> setPlayer(context.getSource(), true))
                .then(Commands.literal("true")
                        .executes(context -> setPlayer(context.getSource(), !PlayerInvulnerableEntityData.isInvul(context.getSource().getPlayer())))
                )
                .then(Commands.literal("false")
                        .executes(context -> setPlayer(context.getSource(), false))
                )
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests((p_138084_, p_138085_) -> {
                            PlayerList playerlist = p_138084_.getSource().getServer().getPlayerList();
                            return SharedSuggestionProvider.suggest(playerlist.getPlayers().stream().map((p_289284_) -> p_289284_.getGameProfile().getName()), p_138085_);
                        })
                        .then(Commands.literal("true")
                                .executes(context -> setPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "targets"), true))
                        )
                        .then(Commands.literal("false")
                                .executes(context -> setPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "targets"), false))
                        )
                );
    }

    private static int setPlayers(CommandSourceStack p_138089_, Collection<GameProfile> p_138090_, boolean mode) throws CommandSyntaxException {
        PlayerList playerlist = p_138089_.getServer().getPlayerList();
        for (GameProfile gameprofile : p_138090_) {
            ServerPlayer player = playerlist.getPlayerByName(gameprofile.getName());
            if (player != null) {
                PlayerInvulnerableEntityData.setInvul(player, mode);
                p_138089_.sendSuccess(() -> Component.literal("set player " + player.getName() + " fe invulnerable " + mode).withStyle(ChatFormatting.YELLOW), false);
            }
        }

        return 0;
    }

    private static int setPlayer(CommandSourceStack p_138089_, boolean mode) throws CommandSyntaxException {
        ServerPlayer player = p_138089_.getPlayer();
        if (player != null) {
            PlayerInvulnerableEntityData.setInvul(player, mode);
            p_138089_.sendSuccess(() -> Component.literal("set player " + player.getName() + " fe invulnerable " + mode).withStyle(ChatFormatting.YELLOW), false);
        }
        return 0;
    }
}
