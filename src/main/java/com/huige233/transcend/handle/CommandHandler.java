package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Map;

/** 注册并执行管理员飞行切换、物品给予、维度传送、玩家修复与终结击杀命令。 */
@Mod.EventBusSubscriber

public class CommandHandler {

    private static final Map<String, RegistryObject<Item>> ITEM_MAP = Map.ofEntries(
            Map.entry("transcend_ingot", ModItems.transcend_ingot),
            Map.entry("epic_ingot", ModItems.epic_ingot),
            Map.entry("transcend_sword", ModItems.transcend_sword),
            Map.entry("transcend_shield", ModItems.transcend_shield),
            Map.entry("transcend_curio", ModItems.transcend_curio),
            Map.entry("thelasttotem", ModItems.thelasttotem),
            Map.entry("transcend_editor_device", ModItems.transcend_editor_device),
            Map.entry("transcend_helmet", ModItems.transcend_helmet),
            Map.entry("transcend_chestplate", ModItems.transcend_chestplate),
            Map.entry("transcend_leggings", ModItems.transcend_leggings),
            Map.entry("transcend_boots", ModItems.transcend_boots)
    );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("tr_fly")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    boolean canFly = player.getAbilities().mayfly;
                    if (canFly) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                        context.getSource().sendSuccess(
                                () -> Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("command.transcend.fly.off")
                                                .withStyle(ChatFormatting.RED)), false);
                    } else {
                        player.getAbilities().mayfly = true;
                        player.onUpdateAbilities();
                        context.getSource().sendSuccess(
                                () -> Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("command.transcend.fly.on")
                                                .withStyle(ChatFormatting.GREEN)), false);
                    }
                    return 1;
                }));

        dispatcher.register(Commands.literal("tr_give")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            ITEM_MAP.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> giveItem(context.getSource(),
                                StringArgumentType.getString(context, "item"), 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> giveItem(context.getSource(),
                                        StringArgumentType.getString(context, "item"),
                                        IntegerArgumentType.getInteger(context, "count"))))));

        dispatcher.register(Commands.literal("tr_dimension")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            context.getSource().sendSuccess(() -> Component.literal("[Dimensions]")
                                    .withStyle(ChatFormatting.GOLD), false);
                            for (ServerLevel level : server.getAllLevels()) {
                                String dimName = level.dimension().location().toString();
                                context.getSource().sendSuccess(() -> Component.literal("  - " + dimName)
                                        .withStyle(ChatFormatting.GRAY), false);
                            }
                            return 1;
                        }))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            for (ServerLevel level : server.getAllLevels()) {
                                builder.suggest(level.dimension().location().toString());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                String dimName = StringArgumentType.getString(context, "name");
                                ResourceLocation rl = ResourceLocation.tryParse(dimName);
                                if (rl == null) {
                                    context.getSource().sendFailure(Component.translatable("command.transcend.dimension.invalid", dimName));
                                    return 0;
                                }
                                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
                                ServerLevel targetLevel = player.server.getLevel(key);
                                if (targetLevel == null) {
                                    context.getSource().sendFailure(Component.translatable("command.transcend.dimension.not_found", dimName));
                                    return 0;
                                }
                                BlockPos spawn = targetLevel.getSharedSpawnPos();
                                double x = spawn.getX() + 0.5;
                                double y = spawn.getY() + 1;
                                double z = spawn.getZ() + 0.5;
                                player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
                                player.setDeltaMovement(Vec3.ZERO);
                                player.fallDistance = 0.0F;
                                String finalDimName = dimName;
                                context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("command.transcend.dimension.teleported", finalDimName)
                                                .withStyle(ChatFormatting.GREEN)), true);
                                return 1;
                            } catch (Throwable t) {
                                Transcend.LOGGER.error("tr_dimension command failed", t);
                                context.getSource().sendFailure(Component.translatable("command.transcend.error", t.getMessage()));
                                return 0;
                            }
                        })));

        dispatcher.register(Commands.literal("tr_fix")
                .requires(source -> source.hasPermission(2))
                .executes(context -> fixPlayer(context.getSource(),
                        context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                         .executes(context -> fixPlayer(context.getSource(),
                                 EntityArgument.getPlayer(context, "target")))));

        
        dispatcher.register(Commands.literal("tr_dead_inside")
                .requires(source -> source.hasPermission(2))
                .executes(context -> deadInsideLookingAt(context.getSource()))
                .then(Commands.argument("target", EntityArgument.entities())
                        .executes(context -> deadInsideTargets(context.getSource(),
                                EntityArgument.getEntities(context, "target")))));
    }

    
    private static int deadInsideLookingAt(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.transcend.requires_player").withStyle(ChatFormatting.RED));
            return 0;
        }
        var hit = player.pick(5.0, 1.0F, false);
        Vec3 endPos = hit.getLocation();
        var targets = player.level().getEntities(player,
                player.getBoundingBox().expandTowards(endPos).inflate(1.5),
                e -> e instanceof net.minecraft.world.entity.LivingEntity l && l.isAlive() && e != player);
        if (targets.isEmpty()) {
            source.sendFailure(Component.translatable("command.transcend.no_living_target").withStyle(ChatFormatting.RED));
            return 0;
        }
        int count = 0;
        for (var e : targets) {
            com.huige233.transcend.util.TranscendDeadInside.apply((net.minecraft.world.entity.LivingEntity) e, player);
            count++;
            if (count >= 5) break;
        }
        final int n = count;
        source.sendSuccess(() -> Component.literal("[Transcend] dead inside × " + n)
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return n;
    }

    
    private static int deadInsideTargets(CommandSourceStack source, Iterable<? extends net.minecraft.world.entity.Entity> targets) {
        ServerPlayer attacker = null;
        try {
            attacker = source.getPlayerOrException();
        } catch (Exception ignored) {
        }
        int count = 0;
        for (var e : targets) {
            if (e instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()) {
                com.huige233.transcend.util.TranscendDeadInside.apply(living, attacker);
                count++;
            }
        }
        final int n = count;
        source.sendSuccess(() -> Component.literal("[Transcend] dead inside × " + n)
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return n;
    }

    private static int fixPlayer(CommandSourceStack source, ServerPlayer player) {
        int fixes = 0;

        float health = player.getHealth();
        if (Float.isNaN(health) || Float.isInfinite(health) || health <= 0) {
            float maxHp = player.getMaxHealth();
            if (Float.isNaN(maxHp) || Float.isInfinite(maxHp) || maxHp <= 0) {
                AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    new ArrayList<>(maxHealthAttr.getModifiers()).forEach(
                            mod -> maxHealthAttr.removeModifier(mod.getId()));
                    maxHealthAttr.setBaseValue(20.0);
                }
                maxHp = player.getMaxHealth();
            }
            player.setHealth(maxHp);
            fixes++;
        }

        float absorption = player.getAbsorptionAmount();
        if (Float.isNaN(absorption) || Float.isInfinite(absorption) || absorption < 0) {
            player.setAbsorptionAmount(0.0F);
            fixes++;
        }

        if (player.isInvulnerable() && !player.isCreative() && !player.isSpectator()) {
            player.setInvulnerable(false);
            fixes++;
        }

        if (player.deathTime != 0) {
            player.deathTime = 0;
            fixes++;
        }

        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);

        player.removeAllEffects();

        int finalFixes = fixes;
        String targetName = player.getName().getString();
        source.sendSuccess(() -> Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("command.transcend.repair.completed", targetName, finalFixes)
                        .withStyle(ChatFormatting.GREEN)), true);

        if (fixes > 0) {
            player.sendSystemMessage(Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("command.transcend.repair.player_notice")
                            .withStyle(ChatFormatting.GREEN)));
        }
        return 1;
    }

    private static int giveItem(CommandSourceStack source, String itemName, int count) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            RegistryObject<Item> reg = ITEM_MAP.get(itemName);
            if (reg == null) {
                source.sendFailure(Component.translatable("command.transcend.item.unknown", itemName).withStyle(ChatFormatting.RED));
                return 0;
            }
            ItemStack stack = new ItemStack(reg.get(), count);
            player.getInventory().add(stack);
            source.sendSuccess(() -> Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("command.transcend.item.gave", count, itemName).withStyle(ChatFormatting.GREEN)), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.transcend.error", e.getMessage()));
            return 0;
        }
    }
}