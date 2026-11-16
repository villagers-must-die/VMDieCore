package org.dishanhai.vmdiecore.api.event;

import net.neoforged.bus.api.Event;
import org.dishanhai.vmdiecore.api.DungeonRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据包加载后触发，其他模组借此注册进入规则。同名覆盖。
 */
public class RegisterDungeonRulesEvent extends Event {
    private final List<DungeonRule> definitions = new ArrayList<>();

    public void register(DungeonRule rule) {
        definitions.add(rule);
    }

    public List<DungeonRule> definitions() {
        return List.copyOf(definitions);
    }
}
