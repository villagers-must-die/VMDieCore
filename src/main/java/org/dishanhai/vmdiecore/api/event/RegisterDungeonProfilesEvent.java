package org.dishanhai.vmdiecore.api.event;

import net.neoforged.bus.api.Event;
import org.dishanhai.vmdiecore.api.DungeonProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据包加载后触发，其他模组借此注册维度档。同名覆盖。
 */
public class RegisterDungeonProfilesEvent extends Event {
    private final List<DungeonProfile> definitions = new ArrayList<>();

    public void register(DungeonProfile profile) {
        definitions.add(profile);
    }

    public List<DungeonProfile> definitions() {
        return List.copyOf(definitions);
    }
}
