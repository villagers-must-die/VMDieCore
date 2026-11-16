package org.dishanhai.vmdiecore.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 进入地牢前触发。取消后不传送。
 */
public class DungeonEnterEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final int dungeonId;

    public DungeonEnterEvent(ServerPlayer player, int dungeonId) {
        this.player = player;
        this.dungeonId = dungeonId;
    }

    public ServerPlayer player() {
        return player;
    }

    public int dungeonId() {
        return dungeonId;
    }
}
