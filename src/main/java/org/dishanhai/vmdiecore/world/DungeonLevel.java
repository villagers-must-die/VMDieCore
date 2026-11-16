package org.dishanhai.vmdiecore.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 地牢维度。运行时创建，销毁即丢弃。
 */
public class DungeonLevel extends ServerLevel {
    private static long bootingSeed;

    private final int id;
    private final long levelSeed;
    private BlockPos anchor = BlockPos.ZERO;
    private int landingOffset = 8;

    DungeonLevel(MinecraftServer server, Executor executor, LevelStorageSource.LevelStorageAccess access,
                 ServerLevelData levelData, ResourceKey<Level> key, LevelStem stem,
                 ChunkProgressListener listener, long seed, int id) {
        super(server, executor, access, levelData, key, stem, listener, false, seed, List.<CustomSpawner>of(),
                false, new RandomSequences(seed));
        this.id = id;
        this.levelSeed = seed;
    }

    public long seed() {
        return levelSeed;
    }

    public int id() {
        return id;
    }

    public BlockPos anchor() {
        return anchor;
    }

    public void setAnchor(BlockPos pos) {
        anchor = pos.immutable();
    }

    public void setLandingOffset(int offset) {
        landingOffset = offset;
    }

    public BlockPos landing() {
        return anchor.below(landingOffset);
    }

    static void prepareSeed(long seed) {
        bootingSeed = seed;
    }

    @Override
    public long getSeed() {
        return levelSeed != 0L ? levelSeed : bootingSeed;
    }
}
