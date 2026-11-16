package org.dishanhai.vmdiecore.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.api.DungeonProfile;
import org.dishanhai.vmdiecore.api.DungeonTheme;
import org.dishanhai.vmdiecore.api.GeneratorSettings;
import org.dishanhai.vmdiecore.api.TierDefinition;
import org.dishanhai.vmdiecore.api.event.DungeonCreateEvent;
import org.dishanhai.vmdiecore.api.event.DungeonDestroyEvent;
import org.dishanhai.vmdiecore.config.VMDConfig;
import org.dishanhai.vmdiecore.data.DungeonDefinitions;
import org.dishanhai.vmdiecore.mixin.ServerLevelsAccess;
import org.dishanhai.vmdiecore.world.DungeonLevel;
import org.dishanhai.vmdiecore.world.DungeonLevelFactory;
import org.dishanhai.vmdiecore.world.OriginWorld;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * 地牢登记表。运行时维度都挂在这里，存档清单在 {@link DungeonStore}。
 */
public final class DungeonPool {
    private static final Map<Integer, Entry> ENTRIES = new LinkedHashMap<>();
    private static int nextId = 1;

    private record Entry(DungeonInstance info, DungeonLevel level) {
    }

    public static OptionalInt create(DungeonTheme theme, TierDefinition tier) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return OptionalInt.empty();
        }
        DungeonProfile profile = DungeonDefinitions.profile(tier.profileId()).orElse(null);
        if (profile == null) {
            VMDieCore.LOGGER.warn("创建失败：维度档 {} 不存在", tier.profileId());
            return OptionalInt.empty();
        }
        DungeonCreateEvent event = new DungeonCreateEvent(server, theme, tier);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            VMDieCore.LOGGER.info("创建地牢被取消：{} 等级 {}", theme.id(), tier.level());
            return OptionalInt.empty();
        }
        int guard = 0;
        while (ENTRIES.size() >= VMDConfig.maxDungeons() && guard++ < 64) {
            if (!evictOldest()) break;
        }
        if (ENTRIES.size() >= VMDConfig.maxDungeons()) {
            VMDieCore.LOGGER.warn("创建失败：地牢已达上限 {}，没有可淘汰的", VMDConfig.maxDungeons());
            return OptionalInt.empty();
        }
        int id = nextId++;
        DungeonLevel level = DungeonLevelFactory.create(server, id, profile);
        ResourceKey<Level> key = level.dimension();
        ((ServerLevelsAccess) server).getLevelsMap().put(key, level);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        try {
            build(level, tier, profile);
        } catch (RuntimeException exception) {
            VMDieCore.LOGGER.error("生成地牢 #{} 失败", id, exception);
            unload(server, level);
            return OptionalInt.empty();
        }
        DungeonInstance info = new DungeonInstance(id, theme.id(), tier.level(), tier.hasExit(),
                System.currentTimeMillis());
        ENTRIES.put(id, new Entry(info, level));
        DungeonStore.get(server).put(new DungeonStore.Stored(id, theme.id().toString(), tier.level(),
                tier.hasExit(), info.createdAt(), key.location(), level.anchor(), level.seed()));
        VMDieCore.LOGGER.info("创建地牢 #{} {} 等级 {} 锚点 {} 种子 {}", id, theme.id(), tier.level(),
                level.anchor(), level.seed());
        return OptionalInt.of(id);
    }

    private static void build(DungeonLevel level, TierDefinition tier, DungeonProfile profile) {
        BlockPos anchor;
        if (profile.generator() instanceof GeneratorSettings.Empty) {
            anchor = DungeonLevelFactory.randomAnchor(level, profile.anchor());
            level.setAnchor(anchor);
            DungeonLevelFactory.forceLoad(level, anchor);
            level.getChunk(anchor.getX() >> 4, anchor.getZ() >> 4);
            if (!DungeonLevelFactory.placeStructure(level, anchor, tier.pools(), profile.jigsaw())) {
                throw new IllegalStateException("结构池全部拼装失败");
            }
        } else {
            level.getChunk(0, 0);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
            anchor = new BlockPos(0, surface + profile.jigsaw().landingOffset(), 0);
            level.setAnchor(anchor);
            DungeonLevelFactory.forceLoad(level, anchor);
            VMDieCore.LOGGER.info("地形生成 种子 {} 地表 {}", level.getSeed(), surface);
        }
        DungeonLevelFactory.prepareLanding(level, level.landing(), profile.landing());
        if (profile.fixedTime() >= 0L) {
            level.setDayTime(profile.fixedTime());
        }
    }

    public static boolean destroy(int id) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) {
            return false;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            ENTRIES.remove(id);
            return true;
        }
        DungeonDestroyEvent event = new DungeonDestroyEvent(server, id, entry.level().dimension());
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            VMDieCore.LOGGER.info("销毁地牢 #{} 被取消", id);
            return false;
        }
        ENTRIES.remove(id);
        unload(server, entry.level());
        DungeonStore.get(server).remove(id);
        VMDieCore.LOGGER.info("销毁地牢 #{}", id);
        return true;
    }

    private static void unload(MinecraftServer server, DungeonLevel level) {
        for (ServerPlayer player : List.copyOf(level.players())) {
            OriginWorld.sendHome(player);
        }
        ResourceKey<Level> key = level.dimension();
        ((ServerLevelsAccess) server).getLevelsMap().remove(key);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
        // 关存储要排在已入队的方块与 POI 任务之后，否则 POI 读取会永久等待。
        if (server.isRunning()) {
            server.execute(() -> release(server, level, key));
        } else {
            release(server, level, key);
        }
    }

    private static void release(MinecraftServer server, DungeonLevel level, ResourceKey<Level> key) {
        try {
            level.close();
        } catch (IOException exception) {
            VMDieCore.LOGGER.warn("关闭地牢维度 {} 失败", key.location(), exception);
        }
        deleteRecursively(((ServerLevelsAccess) server).getStorageSource().getDimensionPath(key));
    }

    private static void deleteRecursively(Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(DungeonPool::deleteQuietly);
        } catch (IOException exception) {
            VMDieCore.LOGGER.warn("清理地牢目录 {} 失败", path, exception);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            VMDieCore.LOGGER.warn("删除 {} 失败", path, exception);
        }
    }

    private static boolean evictOldest() {
        Optional<Map.Entry<Integer, Entry>> oldest = ENTRIES.entrySet().stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().info().createdAt()));
        if (oldest.isEmpty()) {
            return false;
        }
        destroy(oldest.get().getKey());
        return true;
    }

    public static DungeonLevel level(int id) {
        Entry entry = ENTRIES.get(id);
        return entry == null ? null : entry.level();
    }

    public static Optional<DungeonInstance> info(int id) {
        Entry entry = ENTRIES.get(id);
        return entry == null ? Optional.empty() : Optional.of(entry.info());
    }

    public static List<DungeonInstance> list() {
        return ENTRIES.values().stream().map(Entry::info).toList();
    }

    public static OptionalInt idOf(Level level) {
        for (Map.Entry<Integer, Entry> entry : ENTRIES.entrySet()) {
            if (entry.getValue().level() == level) {
                return OptionalInt.of(entry.getKey());
            }
        }
        if (!(level instanceof DungeonLevel dungeon)) {
            return OptionalInt.empty();
        }
        return ENTRIES.containsKey(dungeon.id()) ? OptionalInt.of(dungeon.id()) : OptionalInt.empty();
    }

    public static void clearAll() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        // 停机时关存储会等地块任务，故只清引用；文件由下次启动统一清。
        VMDieCore.LOGGER.info("释放 {} 个地牢维度引用", ENTRIES.size());
        ENTRIES.clear();
        nextId = 1;
    }

    public static void restore(MinecraftServer server) {
        DungeonStore store = DungeonStore.get(server);
        List<Integer> dead = new ArrayList<>();
        for (DungeonStore.Stored stored : store.all()) {
            ResourceLocation themeId = ResourceLocation.tryParse(stored.theme());
            if (themeId == null) {
                VMDieCore.LOGGER.warn("丢弃地牢 #{}：种类 id 无效", stored.id());
                dead.add(stored.id());
                continue;
            }
            DungeonProfile profile = DungeonDefinitions.theme(themeId)
                    .flatMap(theme -> theme.tier(stored.level()))
                    .flatMap(tier -> DungeonDefinitions.profile(tier.profileId()))
                    .orElse(null);
            if (profile == null) {
                VMDieCore.LOGGER.warn("丢弃地牢 #{}：定义已不存在", stored.id());
                dead.add(stored.id());
                continue;
            }
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, stored.key());
            DungeonLevel level = DungeonLevelFactory.revive(server, stored.id(), key, stored.anchor(), profile, stored.seed());
            ((ServerLevelsAccess) server).getLevelsMap().put(key, level);
            server.markWorldsDirty();
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
            ENTRIES.put(stored.id(), new Entry(new DungeonInstance(stored.id(), themeId, stored.level(),
                    stored.hasExit(), stored.createdAt()), level));
            VMDieCore.LOGGER.info("恢复地牢 #{} {}", stored.id(), key.location());
        }
        for (int id : dead) {
            store.remove(id);
        }
        nextId = store.nextId();
        cleanupOrphans(server, store);
    }

    private static void cleanupOrphans(MinecraftServer server, DungeonStore store) {
        Set<String> keep = new HashSet<>();
        for (DungeonStore.Stored stored : store.all()) {
            keep.add(stored.key().getPath());
        }
        Path parent = ((ServerLevelsAccess) server).getStorageSource()
                .getDimensionPath(DungeonLevelFactory.key(1)).getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                if (name.matches("dungeon_\\d+(_[0-9a-f]+)?") && !keep.contains(name)) {
                    deleteRecursively(path);
                }
            }
        } catch (IOException exception) {
            VMDieCore.LOGGER.warn("清理残留地牢维度失败", exception);
        }
    }

    private DungeonPool() {
    }
}
