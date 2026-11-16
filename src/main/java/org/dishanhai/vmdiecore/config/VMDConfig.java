package org.dishanhai.vmdiecore.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VMDConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_DUNGEONS;
    public static final ModConfigSpec.BooleanValue CONSOLE_REQUIRE_OP;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("dungeon");
        MAX_DUNGEONS = builder
                .comment("同时存在的地牢数量上限，超出后淘汰最早创建的")
                .defineInRange("maxDungeons", 8, 1, 64);
        builder.pop();
        builder.push("console");
        CONSOLE_REQUIRE_OP = builder
                .comment("地牢控制台是否要求 OP 权限")
                .define("requireOp", true);
        builder.pop();
        SPEC = builder.build();
    }

    public static int maxDungeons() {
        return MAX_DUNGEONS.get();
    }

    public static boolean consoleRequireOp() {
        return CONSOLE_REQUIRE_OP.get();
    }

    private VMDConfig() {
    }
}
