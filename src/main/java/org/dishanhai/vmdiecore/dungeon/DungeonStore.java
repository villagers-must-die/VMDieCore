package org.dishanhai.vmdiecore.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地牢清单的持久化。玩家在地牢里退出存档时，靠它把维度重建回来。
 */
public final class DungeonStore extends SavedData {
    private static final String DATA_NAME = "vmdiecore_dungeons";
    private static final SavedData.Factory<DungeonStore> FACTORY =
            new SavedData.Factory<>(DungeonStore::new, DungeonStore::load, DataFixTypes.LEVEL);

    private final Map<Integer, Stored> entries = new LinkedHashMap<>();

    public record Stored(int id, String theme, int level, boolean hasExit, long createdAt,
                         ResourceLocation key, BlockPos anchor, long seed) {

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", id);
            tag.putString("theme", theme);
            tag.putInt("level", level);
            tag.putBoolean("has_exit", hasExit);
            tag.putLong("created_at", createdAt);
            tag.putString("key", key.toString());
            tag.putInt("anchor_x", anchor.getX());
            tag.putInt("anchor_y", anchor.getY());
            tag.putInt("anchor_z", anchor.getZ());
            tag.putLong("seed", seed);
            return tag;
        }

        static Stored load(CompoundTag tag) {
            ResourceLocation key = ResourceLocation.tryParse(tag.getString("key"));
            ResourceLocation theme = ResourceLocation.tryParse(tag.getString("theme"));
            if (key == null || theme == null) {
                return null;
            }
            return new Stored(tag.getInt("id"), theme.toString(), tag.getInt("level"),
                    tag.getBoolean("has_exit"), tag.getLong("created_at"), key,
                    new BlockPos(tag.getInt("anchor_x"), tag.getInt("anchor_y"), tag.getInt("anchor_z")),
                    tag.getLong("seed"));
        }
    }

    public static DungeonStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static DungeonStore load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonStore store = new DungeonStore();
        ListTag list = tag.getList("dungeons", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Stored stored = Stored.load(list.getCompound(i));
            if (stored != null) {
                store.entries.put(stored.id(), stored);
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Stored stored : entries.values()) {
            list.add(stored.save());
        }
        tag.put("dungeons", list);
        return tag;
    }

    public Collection<Stored> all() {
        return entries.values();
    }

    public void put(Stored stored) {
        entries.put(stored.id(), stored);
        setDirty();
    }

    public void remove(int id) {
        if (entries.remove(id) != null) {
            setDirty();
        }
    }

    public int nextId() {
        return entries.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }
}
