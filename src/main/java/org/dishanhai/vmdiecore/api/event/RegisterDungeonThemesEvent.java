package org.dishanhai.vmdiecore.api.event;

import net.neoforged.bus.api.Event;
import org.dishanhai.vmdiecore.api.DungeonTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据包加载后触发，其他模组借此注册地牢种类。同名覆盖。
 */
public class RegisterDungeonThemesEvent extends Event {
    private final List<DungeonTheme> definitions = new ArrayList<>();

    public void register(DungeonTheme theme) {
        definitions.add(theme);
    }

    public List<DungeonTheme> definitions() {
        return List.copyOf(definitions);
    }
}
