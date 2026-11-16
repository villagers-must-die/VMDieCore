package org.dishanhai.vmdiecore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.dishanhai.vmdiecore.data.DungeonDefinitions;
import org.dishanhai.vmdiecore.dungeon.DungeonInstance;
import org.dishanhai.vmdiecore.dungeon.DungeonPool;
import org.dishanhai.vmdiecore.world.DungeonLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 给其他模组用的门面。不暴露 ServerLevel，维度只给 key。
 */
public final class VMDungeons {

    /** 按种类与等级创建地牢，返回实例 id。 */
    public static OptionalInt create(MinecraftServer server, ResourceLocation theme, int level) {
        return DungeonApi.create(theme, level);
    }

    /** 删除指定地牢，id 不存在返回 false。 */
    public static boolean destroy(MinecraftServer server, int id) {
        return DungeonApi.destroy(id);
    }

    /** 把玩家传进地牢，校验不过返回 false。 */
    public static boolean enter(ServerPlayer player, int id) {
        return DungeonApi.enter(player, id);
    }

    /** 把玩家送回起源大厅，不在任何地牢里返回 false。 */
    public static boolean leave(ServerPlayer player) {
        return DungeonApi.leave(player);
    }

    /** 查地牢的维度 key、锚点与种子。 */
    public static Optional<DungeonHandle> handle(int id) {
        DungeonInstance info = DungeonPool.info(id).orElse(null);
        DungeonLevel level = DungeonPool.level(id);
        if (info == null || level == null) {
            return Optional.empty();
        }
        return Optional.of(new DungeonHandle(info.id(), info.theme(), info.level(), level.dimension(),
                level.anchor(), level.getSeed()));
    }

    /** 列出当前所有在内存里的地牢。 */
    public static List<DungeonHandle> handles() {
        List<DungeonHandle> handles = new ArrayList<>();
        for (DungeonInstance info : DungeonPool.list()) {
            handle(info.id()).ifPresent(handles::add);
        }
        return handles;
    }

    /** 列出所有已加载的地牢种类。 */
    public static Collection<DungeonTheme> themes() {
        return DungeonDefinitions.themes();
    }

    /** 按 id 查地牢种类。 */
    public static Optional<DungeonTheme> theme(ResourceLocation id) {
        return DungeonDefinitions.theme(id);
    }

    /** 按 id 查维度档。 */
    public static Optional<DungeonProfile> profile(ResourceLocation id) {
        return DungeonDefinitions.profile(id);
    }

    /** 按 id 查进入规则。 */
    public static Optional<DungeonRule> rule(ResourceLocation id) {
        return DungeonDefinitions.rule(id);
    }

    private VMDungeons() {
    }
}
