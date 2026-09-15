package com.mega.uom.common.command;

import com.mega.uom.common.command.entity.DeleteCommand;
import com.mega.uom.common.command.entity.ForceKillCommand;
import com.mega.uom.common.command.entity.ForceSetHealthCommand;
import com.mega.uom.common.command.entity.SoftGetHealthZeroCommand;
import com.mega.uom.common.command.others.CheckCustomRenderersCommand;
import com.mega.uom.common.command.others.DisplayInfoCommand;
import com.mega.uom.common.command.others.ModifyWindowCommand;
import com.mega.uom.common.command.player.DataAllCommand;
import com.mega.uom.common.command.player.SetInvulnerablePlayerCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandsEvent {
    @SubscribeEvent
    public static void load(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("fantasy_ending")
                        .then(KillAllUomsCommand.register())
                        .then(Commands.literal("entity")
                                .then(ForceSetHealthCommand.register())
                                .then(ForceKillCommand.register())
                                .then(SoftGetHealthZeroCommand.register())
                                .then(DeleteCommand.register())

                        )
                        .then(Commands.literal("others")
                                .then(ModifyWindowCommand.register())
                                //.then(SkillRenderTestCommand.register())
                                .then(CheckCustomRenderersCommand.register())
                                .then(DisplayInfoCommand.register())
                        )
                        .then(Commands.literal("player")
                                .then(SetInvulnerablePlayerCommand.register())
                                .then(DataAllCommand.register())
                        )

        );
    }
}
