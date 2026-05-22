package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionHandler;
import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import com.huige233.transcend.entity.boss.BossPhase;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.world.TranscendDimensions;
import com.huige233.transcend.world.arena.TranscendArenaManager;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.nexus.NexusManager;
import com.huige233.transcend.world.nexus.NexusType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandHandler {

    private static final Map<String, RegistryObject<Item>> ITEM_MAP = Map.ofEntries(
            Map.entry("wand_basic", ModItems.wand_basic),
            Map.entry("wand_advanced", ModItems.wand_advanced),
            Map.entry("wand_master", ModItems.wand_master),
            Map.entry("ancient_glyph", ModItems.ancient_glyph),
            Map.entry("rift_fragment", ModItems.rift_fragment),
            Map.entry("transcendence_core", ModItems.transcendence_core),
            Map.entry("transcend_ingot", ModItems.transcend_ingot),
            Map.entry("epic_ingot", ModItems.epic_ingot),
            Map.entry("magic_crystal", ModItems.magic_crystal),
            Map.entry("refined_magic_crystal", ModItems.refined_magic_crystal),
            Map.entry("mana_storage", ModItems.mana_storage),
            Map.entry("enhance_power", ModItems.enhance_power),
            Map.entry("enhance_duration", ModItems.enhance_duration),
            Map.entry("enhance_efficiency", ModItems.enhance_efficiency),
            Map.entry("enhance_special", ModItems.enhance_special),
            Map.entry("transcend_curio", ModItems.transcend_curio),
            Map.entry("transcend_shield", ModItems.transcend_shield),
            Map.entry("spell_workbench", ModItems.spell_workbench_item)
    );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // ─── /tr_fly ─────────────────────────────────────────────────
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

        // ─── /tr_give <item> [count] ─────────────────────────────────
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

        // ─── /tr_boss <type> [phase] ─────────────────────────────────
        dispatcher.register(Commands.literal("tr_boss")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("warden")
                        .executes(ctx -> summonBoss(ctx.getSource(), ModEntities.ELEMENTAL_WARDEN.get(), 0))
                        .then(Commands.argument("phase", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> summonBoss(ctx.getSource(), ModEntities.ELEMENTAL_WARDEN.get(),
                                        IntegerArgumentType.getInteger(ctx, "phase")))))
                .then(Commands.literal("weaver")
                        .executes(ctx -> summonBoss(ctx.getSource(), ModEntities.VOID_WEAVER.get(), 0))
                        .then(Commands.argument("phase", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> summonBoss(ctx.getSource(), ModEntities.VOID_WEAVER.get(),
                                        IntegerArgumentType.getInteger(ctx, "phase")))))
                .then(Commands.literal("avatar")
                        .executes(ctx -> summonBoss(ctx.getSource(), ModEntities.TRANSCENDENCE_AVATAR.get(), 0))
                        .then(Commands.argument("phase", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> summonBoss(ctx.getSource(), ModEntities.TRANSCENDENCE_AVATAR.get(),
                                        IntegerArgumentType.getInteger(ctx, "phase"))))));

        // ─── /tr_talent_points set|add <amount> ─────────────────────
        dispatcher.register(Commands.literal("tr_talent_points")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerAscensionData data = AscensionCapability.get(player);
                                    data.addTalentPoints(amount - data.getTalentPoints());
                                    AscensionHandler.syncToClient(player, data);
                                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.translatable("msg.transcend.talent_points_set", amount)
                                                    .withStyle(ChatFormatting.GREEN)), false);
                                    return 1;
                                })))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerAscensionData data = AscensionCapability.get(player);
                                    data.addTalentPoints(amount);
                                    AscensionHandler.syncToClient(player, data);
                                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.translatable("msg.transcend.talent_points_added",
                                                    amount, data.getTalentPoints())
                                                    .withStyle(ChatFormatting.GREEN)), false);
                                    return 1;
                                }))));

        // ─── /tr_xp set|add|setlevel|query <amount> [player] ─────────────
        //     仿 /xp 指令风格,管理飞升 XP/等级
        dispatcher.register(Commands.literal("tr_xp")
                .requires(source -> source.hasPermission(2))
                // /tr_xp query
                .then(Commands.literal("query").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    PlayerAscensionData data = AscensionCapability.get(player);
                    long cur = data.getAscensionXP();
                    long nextLv = data.getXPForNextLevel();
                    int lv = data.getAscensionLevel();
                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(String.format(
                                    "等级 %d/%d  XP %d  下一级 %d  天赋点 %d",
                                    lv, com.huige233.transcend.ascension.PlayerAscensionData.MAX_LEVEL,
                                    cur, (nextLv == Long.MAX_VALUE ? -1 : nextLv),
                                    data.getTalentPoints())).withStyle(ChatFormatting.AQUA)), false);
                    return 1;
                }))
                // /tr_xp set <amount>
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long amount = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerAscensionData data = AscensionCapability.get(player);
                                    int oldLv = data.getAscensionLevel();
                                    data.setAscensionXP(amount);
                                    int newLv = data.getAscensionLevel();
                                    AscensionHandler.applyPersistentStats(player, data);
                                    AscensionHandler.syncToClient(player, data);
                                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.literal(String.format(
                                                    "XP 设为 %d  等级 %d→%d",
                                                    amount, oldLv, newLv))
                                                    .withStyle(ChatFormatting.GREEN)), false);
                                    return 1;
                                })))
                // /tr_xp add <amount>
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long amount = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerAscensionData data = AscensionCapability.get(player);
                                    int oldLv = data.getAscensionLevel();
                                    boolean leveledUp = data.addAscensionXP(amount);
                                    int newLv = data.getAscensionLevel();
                                    if (leveledUp) {
                                        AscensionHandler.applyPersistentStats(player, data);
                                    }
                                    AscensionHandler.syncToClient(player, data);
                                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.literal(String.format(
                                                    "+%d XP  现 %d  等级 %d→%d",
                                                    amount, data.getAscensionXP(), oldLv, newLv))
                                                    .withStyle(ChatFormatting.GREEN)), false);
                                    return 1;
                                })))
                // /tr_xp setlevel <level>
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("level",
                                IntegerArgumentType.integer(0,
                                        com.huige233.transcend.ascension.PlayerAscensionData.MAX_LEVEL))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int newLv = IntegerArgumentType.getInteger(context, "level");
                                    PlayerAscensionData data = AscensionCapability.get(player);
                                    int oldLv = data.getAscensionLevel();
                                    data.setAscensionLevel(newLv);
                                    AscensionHandler.applyPersistentStats(player, data);
                                    AscensionHandler.syncToClient(player, data);
                                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.literal(String.format(
                                                    "等级设为 %d (从 %d)  天赋点 %d",
                                                    newLv, oldLv, data.getTalentPoints()))
                                                    .withStyle(ChatFormatting.GREEN)), false);
                                    return 1;
                                }))));

        // ─── /tr_class <class> ───────────────────────────────────────
        dispatcher.register(Commands.literal("tr_class")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("class", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (MageClass mc : MageClass.values()) {
                                if (mc.isSelected()) builder.suggest(mc.id);
                            }
                            builder.suggest("none");
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            MageClass mc = MageClass.getById(StringArgumentType.getString(context, "class"));
                            PlayerAscensionData data = AscensionCapability.get(player);
                            data.forceSetClass(mc);
                            AscensionHandler.applyPersistentStats(player, data);
                            AscensionHandler.syncToClient(player, data);
                            context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                    .withStyle(ChatFormatting.GOLD)
                                    .append(Component.translatable("msg.transcend.class_set", mc.getDisplayName())
                                            .withStyle(ChatFormatting.GREEN)), false);
                            return 1;
                        })));

        // ─── /tr_stage get|set <stage> ───────────────────────────────
        dispatcher.register(Commands.literal("tr_stage")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PlayerAscensionData data = AscensionCapability.get(player);
                            context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                    .withStyle(ChatFormatting.GOLD)
                                    .append(Component.literal("Stage: " + data.getStage() + "/4")
                                            .withStyle(ChatFormatting.AQUA)), false);
                            return 1;
                        }))
                .then(Commands.literal("set")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(0, 4))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int stage = IntegerArgumentType.getInteger(context, "stage");
                                    PlayerAscensionData data = AscensionCapability.get(player);
                                    data.forceSetStage(stage);
                                    AscensionHandler.applyPersistentStats(player, data);
                                    AscensionHandler.syncToClient(player, data);
                                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.literal("Stage set to " + stage)
                                                    .withStyle(ChatFormatting.GREEN)), false);
                                    return 1;
                                }))));

        // ─── /tr_dimension list|<name> ───────────────────────────────
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
                                    context.getSource().sendFailure(Component.literal("Invalid dimension: " + dimName));
                                    return 0;
                                }
                                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
                                ServerLevel targetLevel = player.server.getLevel(key);
                                if (targetLevel == null) {
                                    context.getSource().sendFailure(Component.literal("Dimension not found: " + dimName));
                                    return 0;
                                }
                                // 特殊处理：确保自定义维度结构已生成
                                if (key == TranscendDimensions.ARENA_LEVEL) {
                                    TranscendArenaManager.ensureArena(targetLevel);
                                } else if (key == TranscendDimensions.NEXUS_LEVEL) {
                                    NexusManager.ensureAllStructures(targetLevel);
                                }
                                // 确定传送坐标
                                double x = 0.5, y = 100, z = 0.5;
                                if (key == TranscendDimensions.ARENA_LEVEL) {
                                    y = TranscendArenaManager.ARENA_Y + 2.0;
                                } else if (key == TranscendDimensions.NEXUS_LEVEL) {
                                    BlockPos spawn = NexusType.ISOLATION.getPlatformCenter().above(2);
                                    x = spawn.getX() + 0.5;
                                    y = spawn.getY();
                                    z = spawn.getZ() + 0.5;
                                } else {
                                    BlockPos spawn = targetLevel.getSharedSpawnPos();
                                    x = spawn.getX() + 0.5;
                                    y = spawn.getY() + 1;
                                    z = spawn.getZ() + 0.5;
                                }
                                player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
                                player.setDeltaMovement(Vec3.ZERO);
                                player.fallDistance = 0.0F;
                                if (key == TranscendDimensions.NEXUS_LEVEL) {
                                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                            net.minecraft.world.effect.MobEffects.NIGHT_VISION, 6000, 0, false, false));
                                }
                                String finalDimName = dimName;
                                context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.literal("Teleported to " + finalDimName)
                                                .withStyle(ChatFormatting.GREEN)), true);
                                return 1;
                            } catch (Throwable t) {
                                Transcend.LOGGER.error("tr_dimension command failed", t);
                                context.getSource().sendFailure(Component.literal("Error: " + t.getMessage()));
                                return 0;
                            }
                        })));

        // ─── /tr_mana [set <amount>] ─────────────────────────────────
        dispatcher.register(Commands.literal("tr_mana")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!(player.level() instanceof ServerLevel sl)) return 0;
                    ChunkPos chunkPos = player.chunkPosition();
                    ChunkManaSavedData manaData = ChunkManaSavedData.get(sl);
                    float mana = manaData.getMana(chunkPos);
                    float percent = mana / ChunkManaSavedData.DEFAULT_MANA * 100;
                    String bar = buildManaBar(mana, ChunkManaSavedData.DEFAULT_MANA);
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("[Mana] ").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(String.format("%.1f/%.0f ", mana, ChunkManaSavedData.DEFAULT_MANA))
                                    .withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(bar))
                            .append(Component.literal(String.format(" (%.0f%%)", percent))
                                    .withStyle(mana > 60 ? ChatFormatting.GREEN :
                                            mana > 30 ? ChatFormatting.YELLOW : ChatFormatting.RED)));
                    player.sendSystemMessage(Component.literal("")
                            .append(Component.literal("[Chunk] ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(String.format("(%d, %d)", chunkPos.x, chunkPos.z))
                                    .withStyle(ChatFormatting.GRAY)));
                    return 1;
                })
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0, 15000))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!(player.level() instanceof ServerLevel sl)) return 0;
                                    float amount = FloatArgumentType.getFloat(context, "amount");
                                    ChunkPos chunkPos = player.chunkPosition();
                                    ChunkManaSavedData manaData = ChunkManaSavedData.get(sl);
                                    manaData.setMana(chunkPos, amount);
                                    context.getSource().sendSuccess(() -> Component.literal("[Transcend] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.literal(String.format(
                                                    "Chunk (%d,%d) mana set to %.1f",
                                                    chunkPos.x, chunkPos.z, amount))
                                                    .withStyle(ChatFormatting.GREEN)), false);
                                    return 1;
                                }))));

        // ─── /tr_fix [player] ────────────────────────────────────────
        dispatcher.register(Commands.literal("tr_fix")
                .requires(source -> source.hasPermission(2))
                .executes(context -> fixPlayer(context.getSource(),
                        context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> fixPlayer(context.getSource(),
                                EntityArgument.getPlayer(context, "target")))));
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private static int fixPlayer(CommandSourceStack source, ServerPlayer player) {
        int fixes = 0;

        // 修复 Health
        float health = player.getHealth();
        if (Float.isNaN(health) || Float.isInfinite(health) || health <= 0) {
            float maxHp = player.getMaxHealth();
            if (Float.isNaN(maxHp) || Float.isInfinite(maxHp) || maxHp <= 0) {
                // MaxHealth 属性也坏了，先清除异常 modifier 再恢复
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

        // 修复 AbsorptionAmount
        float absorption = player.getAbsorptionAmount();
        if (Float.isNaN(absorption) || Float.isInfinite(absorption) || absorption < 0) {
            player.setAbsorptionAmount(0.0F);
            fixes++;
        }

        // 清除 invulnerable 残留
        if (player.isInvulnerable() && !player.isCreative() && !player.isSpectator()) {
            player.setInvulnerable(false);
            fixes++;
        }

        // 重置死亡状态
        if (player.deathTime != 0) {
            player.deathTime = 0;
            fixes++;
        }

        // 回血到满
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        // 恢复饥饿值
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);

        // 清除所有负面效果
        player.removeAllEffects();

        int finalFixes = fixes;
        String targetName = player.getName().getString();
        source.sendSuccess(() -> Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Fixed " + targetName + " (" + finalFixes + " issues repaired)")
                        .withStyle(ChatFormatting.GREEN)), true);

        if (fixes > 0) {
            player.sendSystemMessage(Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Your player data has been repaired.")
                            .withStyle(ChatFormatting.GREEN)));
        }
        return 1;
    }

    private static String buildManaBar(float current, float max) {
        int filled = (int) (current / max * 20);
        filled = Math.max(0, Math.min(20, filled));
        StringBuilder sb = new StringBuilder();
        sb.append("\u00a7b");
        for (int i = 0; i < filled; i++) sb.append('|');
        sb.append("\u00a78");
        for (int i = filled; i < 20; i++) sb.append('|');
        sb.append("\u00a7r");
        return sb.toString();
    }

    private static int giveItem(CommandSourceStack source, String itemName, int count) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            RegistryObject<Item> reg = ITEM_MAP.get(itemName);
            if (reg == null) {
                source.sendFailure(Component.literal("Unknown item: " + itemName).withStyle(ChatFormatting.RED));
                return 0;
            }
            ItemStack stack = new ItemStack(reg.get(), count);
            player.getInventory().add(stack);
            source.sendSuccess(() -> Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Gave " + count + "x " + itemName).withStyle(ChatFormatting.GREEN)), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractTranscendBoss> int summonBoss(CommandSourceStack source, EntityType<T> type, int phase) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ServerLevel level = source.getLevel();
            T boss = type.create(level);
            if (boss != null) {
                boss.setPos(player.getX() + 3, player.getY(), player.getZ());
                level.addFreshEntity(boss);
                if (phase >= 2 && phase <= 4) {
                    BossPhase targetPhase = BossPhase.values()[phase - 1];
                    boss.forceSetPhase(targetPhase);
                }
                int displayPhase = phase > 0 ? phase : 1;
                source.sendSuccess(() -> Component.literal("[Transcend] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("Summoned boss (Phase " + displayPhase + ")!")
                                .withStyle(ChatFormatting.RED)), true);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
