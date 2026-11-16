package org.dishanhai.vmdiecore.world;

import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * 构造 ServerLevel 时必须传一个进度监听器，这里什么都不做。
 */
public final class EmptyProgressListener implements ChunkProgressListener {
    @Override
    public void updateSpawnPos(ChunkPos pos) {
    }

    @Override
    public void onStatusChange(ChunkPos pos, ChunkStatus status) {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
