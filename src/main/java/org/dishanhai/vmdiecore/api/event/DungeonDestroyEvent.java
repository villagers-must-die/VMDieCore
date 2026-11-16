package org.dishanhai.vmdiecore.api.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 删除地牢前触发。取消后不删除。
 */
public class DungeonDestroyEvent extends Event implements ICancellableEvent {
    private final MinecraftServer server;
    private final int dungeonId;
    private final ResourceKey<Level> dimension;

    public DungeonDestroyEvent(MinecraftServer server, int dungeonId, ResourceKey<Level> dimension) {
        this.server = server;
        this.dungeonId = dungeonId;
        this.dimension = dimension;
    }

    public MinecraftServer server() {
        return server;
    }

    public int dungeonId() {
        return dungeonId;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }
}
