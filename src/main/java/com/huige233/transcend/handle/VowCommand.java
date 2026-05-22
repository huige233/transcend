package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionHandler;
import com.huige233.transcend.ascension.AscensionVow;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.ascension.VowRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /transcend_vow — 飞升誓约管理命令。
 *
 * <p>子命令：
 * <ul>
 *   <li>{@code list} — 列出当前玩家四阶段已选誓约</li>
 *   <li>{@code available <stage>} — 列出指定阶段可选的誓约</li>
 *   <li>{@code set <stage> <vowId>} — 为指定阶段绑定一个誓约（玩家飞升阶段必须 ≥ stage）</li>
 *   <li>{@code clear <stage>} — 解除指定阶段的誓约</li>
 * </ul>
 *
 * <p>誓约改变后会自动重新注入属性并同步到客户端。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VowCommand {

    private static final SuggestionProvider<CommandSourceStack> VOW_SUGGESTIONS = (ctx, builder) -> {
        Integer stage = null;
        try { stage = IntegerArgumentType.getInteger(ctx, "stage"); } catch (IllegalArgumentException ignored) {}
        if (stage == null) {
            for (AscensionVow v : VowRegistry.all()) builder.suggest(v.getId());
        } else {
            for (AscensionVow v : VowRegistry.getVowsForStage(stage)) builder.suggest(v.getId());
        }
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("transcend_vow")
                // /transcend_vow list
                .then(Commands.literal("list").executes(VowCommand::doList))
                // /transcend_vow available <stage>
                .then(Commands.literal("available")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 4))
                                .executes(VowCommand::doAvailable)))
                // /transcend_vow set <stage> <vowId>
                .then(Commands.literal("set")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 4))
                                .then(Commands.argument("vow", StringArgumentType.string())
                                        .suggests(VOW_SUGGESTIONS)
                                        .executes(VowCommand::doSet))))
                // /transcend_vow clear <stage>
                .then(Commands.literal("clear")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 4))
                                .executes(VowCommand::doClear))));
    }

    private static int doList(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            PlayerAscensionData data = AscensionCapability.get(player);
            ctx.getSource().sendSuccess(() -> Component.literal("[Transcend Vows]").withStyle(ChatFormatting.GOLD), false);
            for (int s = 1; s <= 4; s++) {
                String id = data.getVowForStage(s);
                final int stage = s;
                if (id == null || id.isEmpty()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("  Stage " + stage + ": (none)")
                            .withStyle(ChatFormatting.GRAY), false);
                } else {
                    AscensionVow v = VowRegistry.get(id);
                    ctx.getSource().sendSuccess(() -> Component.literal("  Stage " + stage + ": ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable(v != null ? v.getTranslationKey() : id)
                                    .withStyle(ChatFormatting.AQUA)), false);
                }
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("[Transcend] " + e.getMessage()));
            return 0;
        }
    }

    private static int doAvailable(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        int stage = IntegerArgumentType.getInteger(ctx, "stage");
        ctx.getSource().sendSuccess(() ->
                Component.literal("[Stage " + stage + " Vows]").withStyle(ChatFormatting.GOLD), false);
        for (AscensionVow v : VowRegistry.getVowsForStage(stage)) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + v.getId() + " - ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(v.getTranslationKey()).withStyle(ChatFormatting.AQUA)), false);
        }
        return 1;
    }

    private static int doSet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int stage = IntegerArgumentType.getInteger(ctx, "stage");
            String vowId = StringArgumentType.getString(ctx, "vow");

            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) {
                ctx.getSource().sendFailure(Component.literal("[Transcend] Unknown vow: " + vowId));
                return 0;
            }
            if (vow.getStage() != stage) {
                ctx.getSource().sendFailure(Component.literal(
                        "[Transcend] Vow '" + vowId + "' belongs to stage " + vow.getStage()
                                + ", not stage " + stage));
                return 0;
            }

            PlayerAscensionData data = AscensionCapability.get(player);
            if (data.getStage() < stage) {
                ctx.getSource().sendFailure(Component.literal(
                        "[Transcend] You have not yet reached ascension stage " + stage
                                + " (current: " + data.getStage() + ")"));
                return 0;
            }

            data.setVowForStage(stage, vowId);
            AscensionHandler.applyPersistentStats(player, data);
            AscensionHandler.syncToClient(player, data);

            ctx.getSource().sendSuccess(() -> Component.literal("[Transcend] Bound stage " + stage + " vow: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable(vow.getTranslationKey()).withStyle(ChatFormatting.AQUA)), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("[Transcend] " + e.getMessage()));
            return 0;
        }
    }

    private static int doClear(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int stage = IntegerArgumentType.getInteger(ctx, "stage");
            PlayerAscensionData data = AscensionCapability.get(player);
            data.setVowForStage(stage, "");
            AscensionHandler.applyPersistentStats(player, data);
            AscensionHandler.syncToClient(player, data);
            ctx.getSource().sendSuccess(() -> Component.literal("[Transcend] Cleared stage " + stage + " vow")
                    .withStyle(ChatFormatting.YELLOW), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("[Transcend] " + e.getMessage()));
            return 0;
        }
    }
}
