package org.dishanhai.vmdiecore.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * 玩家离开地牢后触发，只通知。
 */
public class DungeonLeaveEvent extends Event {
    private final ServerPlayer player;
    private final int dungeonId;

    public DungeonLeaveEvent(ServerPlayer player, int dungeonId) {
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
