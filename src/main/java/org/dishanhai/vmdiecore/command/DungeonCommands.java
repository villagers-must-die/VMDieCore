package org.dishanhai.vmdiecore.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.dishanhai.vmdiecore.api.DungeonApi;
import org.dishanhai.vmdiecore.api.DungeonInfo;
import org.dishanhai.vmdiecore.api.DungeonTheme;
import org.dishanhai.vmdiecore.data.DungeonDefinitions;
import org.dishanhai.vmdiecore.dungeon.DungeonPool;

import java.util.List;
import java.util.OptionalInt;

public final class DungeonCommands {

    private static final SuggestionProvider<CommandSourceStack> THEME_SUGGESTIONS = (context, builder) -> {
        for (DungeonTheme theme : DungeonDefinitions.themes()) {
            builder.suggest(theme.id().toString());
        }
        return builder.buildFuture();
    };

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vmdiecore")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dungeon")
                        .then(Commands.literal("create")
                                .then(Commands.argument("theme", ResourceLocationArgument.id())
                                        .suggests(THEME_SUGGESTIONS)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(DungeonCommands::create))))
                        .then(Commands.literal("destroy")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(DungeonCommands::destroy)))
                        .then(Commands.literal("enter")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(DungeonCommands::enter)))
                        .then(Commands.literal("leave")
                                .executes(DungeonCommands::leave))
                        .then(Commands.literal("debug")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(DungeonCommands::debug)))
                        .then(Commands.literal("list")
                                .executes(DungeonCommands::list))
                        .then(Commands.literal("reload")
                                .executes(DungeonCommands::reload))
                        .then(Commands.literal("current")
                                .executes(DungeonCommands::current))));
    }

    private static int create(CommandContext<CommandSourceStack> context) {
        ResourceLocation theme = ResourceLocationArgument.getId(context, "theme");
        int level = IntegerArgumentType.getInteger(context, "level");
        OptionalInt created = DungeonApi.create(theme, level);
        if (created.isEmpty()) {
            context.getSource().sendFailure(Component.literal("创建失败：种类 " + theme + " 等级 " + level));
            return 0;
        }
        int id = created.getAsInt();
        var dungeon = DungeonPool.level(id);
        String tail = dungeon == null ? "" : " 锚点 " + dungeon.anchor().toShortString();
        context.getSource().sendSuccess(() -> Component.literal("已创建地牢 #" + id + tail), false);
        return id;
    }

    private static int destroy(CommandContext<CommandSourceStack> context) {
        int id = IntegerArgumentType.getInteger(context, "id");
        if (!DungeonApi.destroy(id)) {
            context.getSource().sendFailure(Component.literal("删除失败：地牢 #" + id + " 不存在"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("已删除地牢 #" + id), false);
        return id;
    }

    private static int enter(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("该命令只能由玩家执行"));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(context, "id");
        if (!DungeonApi.enter(player, id)) {
            context.getSource().sendFailure(Component.literal("进入失败：地牢 #" + id));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("已进入地牢 #" + id), false);
        return id;
    }

    private static int leave(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("该命令只能由玩家执行"));
            return 0;
        }
        if (!DungeonApi.leave(player)) {
            context.getSource().sendFailure(Component.literal("离开失败：你不在任何地牢里"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("已返回起源大厅"), false);
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> context) {
        int id = IntegerArgumentType.getInteger(context, "id");
        var level = DungeonPool.level(id);
        if (level == null) {
            context.getSource().sendFailure(Component.literal("地牢 #" + id + " 不在运行"));
            return 0;
        }
        boolean listed = context.getSource().getServer().getLevel(level.dimension()) == level;
        long ticks = 0L;
        int entities = 0;
        for (var entity : level.getAllEntities()) {
            entities++;
            ticks += entity.tickCount;
        }
        long sum = ticks;
        int count = entities;
        var pos = level.getSharedSpawnPos();
        context.getSource().sendSuccess(() -> Component.literal(
                "维度 " + level.dimension().location() + " 在表 " + listed + " 玩家 " + level.players().size()
                        + " 实体 " + count + " tick 总和 " + sum + " 出生点已加载 " + level.isLoaded(pos)
                        + " 天 " + level.getDayTime()), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        List<DungeonInfo> dungeons = player == null ? DungeonApi.list() : DungeonApi.listFor(player);
        if (dungeons.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("当前没有地牢"), false);
            return 0;
        }
        for (DungeonInfo info : dungeons) {
            String line = "#" + info.id() + " " + info.themeName() + " " + info.levelName()
                    + (info.hasExit() ? " 有撤离点" : " 无撤离点")
                    + (player == null ? "" : " 进入 " + info.visits() + " 次");
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return dungeons.size();
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        DungeonDefinitions.reloadNow(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal(
                "重载完成：种类 " + DungeonDefinitions.themeCount()
                        + " 维度档 " + DungeonDefinitions.profileCount()
                        + " 规则 " + DungeonDefinitions.ruleCount()), false);
        return 1;
    }

    private static int current(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("该命令只能由玩家执行"));
            return 0;
        }
        OptionalInt current = DungeonApi.current(player);
        if (current.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("你不在任何地牢里"), false);
            return 0;
        }
        int id = current.getAsInt();
        context.getSource().sendSuccess(() -> Component.literal("当前地牢 #" + id), false);
        return id;
    }

    private DungeonCommands() {
    }
}
