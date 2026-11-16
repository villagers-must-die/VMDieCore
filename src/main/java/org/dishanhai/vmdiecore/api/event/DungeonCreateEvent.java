package org.dishanhai.vmdiecore.api.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.dishanhai.vmdiecore.api.DungeonTheme;
import org.dishanhai.vmdiecore.api.TierDefinition;

/**
 * 创建地牢前触发。取消后不创建。
 */
public class DungeonCreateEvent extends Event implements ICancellableEvent {
    private final MinecraftServer server;
    private final DungeonTheme theme;
    private final TierDefinition tier;

    public DungeonCreateEvent(MinecraftServer server, DungeonTheme theme, TierDefinition tier) {
        this.server = server;
        this.theme = theme;
        this.tier = tier;
    }

    public MinecraftServer server() {
        return server;
    }

    public DungeonTheme theme() {
        return theme;
    }

    public TierDefinition tier() {
        return tier;
    }
}
