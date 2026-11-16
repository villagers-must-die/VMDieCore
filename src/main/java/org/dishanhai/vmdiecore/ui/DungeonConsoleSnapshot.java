package org.dishanhai.vmdiecore.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.dishanhai.vmdiecore.api.DungeonApi;
import org.dishanhai.vmdiecore.api.DungeonInfo;
import org.dishanhai.vmdiecore.config.VMDConfig;
import org.dishanhai.vmdiecore.data.DungeonDefinitions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One menu's immutable server snapshot. Never installed into client global registries. */
public record DungeonConsoleSnapshot(int maxDungeons, int profileCount, int ruleCount,
                                     List<Theme> themes, List<DungeonInfo> dungeons) {
    private static final int MAX_ENTRIES = 4096;

    public DungeonConsoleSnapshot {
        themes = List.copyOf(themes);
        dungeons = List.copyOf(dungeons);
    }

    public record Theme(ResourceLocation id, String name, List<Tier> tiers) {
        public Theme { tiers = List.copyOf(tiers); }
    }

    public record Tier(int level, String name) {}

    public static DungeonConsoleSnapshot capture(ServerPlayer player) {
        var themes = DungeonDefinitions.themes().stream()
                .sorted(Comparator.comparing(theme -> theme.id()))
                .map(theme -> new Theme(theme.id(), theme.name(), theme.tierList().stream()
                        .map(tier -> new Tier(tier.level(), tier.name())).toList())).toList();
        return new DungeonConsoleSnapshot(VMDConfig.maxDungeons(), DungeonDefinitions.profileCount(),
                DungeonDefinitions.ruleCount(), themes, DungeonApi.listFor(player));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maxDungeons);
        buffer.writeVarInt(profileCount);
        buffer.writeVarInt(ruleCount);
        writeCount(buffer, themes.size());
        for (Theme theme : themes) {
            buffer.writeResourceLocation(theme.id());
            buffer.writeUtf(theme.name());
            writeCount(buffer, theme.tiers().size());
            for (Tier tier : theme.tiers()) {
                buffer.writeVarInt(tier.level());
                buffer.writeUtf(tier.name());
            }
        }
        writeCount(buffer, dungeons.size());
        for (DungeonInfo info : dungeons) {
            buffer.writeVarInt(info.id());
            buffer.writeResourceLocation(info.theme());
            buffer.writeUtf(info.themeName());
            buffer.writeVarInt(info.level());
            buffer.writeUtf(info.levelName());
            buffer.writeBoolean(info.hasExit());
            buffer.writeVarInt(info.visits());
        }
    }

    public static DungeonConsoleSnapshot read(FriendlyByteBuf buffer) {
        int max = buffer.readVarInt();
        int profiles = buffer.readVarInt();
        int rules = buffer.readVarInt();
        List<Theme> themes = new ArrayList<>();
        for (int i = readCount(buffer); i > 0; i--) {
            var id = buffer.readResourceLocation();
            var name = buffer.readUtf();
            List<Tier> tiers = new ArrayList<>();
            for (int j = readCount(buffer); j > 0; j--) {
                tiers.add(new Tier(buffer.readVarInt(), buffer.readUtf()));
            }
            themes.add(new Theme(id, name, tiers));
        }
        List<DungeonInfo> dungeons = new ArrayList<>();
        for (int i = readCount(buffer); i > 0; i--) {
            dungeons.add(new DungeonInfo(buffer.readVarInt(), buffer.readResourceLocation(), buffer.readUtf(),
                    buffer.readVarInt(), buffer.readUtf(), buffer.readBoolean(), buffer.readVarInt()));
        }
        return new DungeonConsoleSnapshot(max, profiles, rules, themes, dungeons);
    }

    private static void writeCount(FriendlyByteBuf buffer, int count) {
        if (count > MAX_ENTRIES) throw new IllegalArgumentException("Too many console entries");
        buffer.writeVarInt(count);
    }

    private static int readCount(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid console entry count");
        return count;
    }
}
