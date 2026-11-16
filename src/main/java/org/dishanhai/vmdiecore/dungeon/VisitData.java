package org.dishanhai.vmdiecore.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class VisitData {
    private static final String TAG = "vmdiecore_visits";

    public static int visits(ServerPlayer player, int dungeonId) {
        CompoundTag root = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(TAG);
        return root.getInt(Integer.toString(dungeonId));
    }

    public static int increment(ServerPlayer player, int dungeonId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag root = persisted.getCompound(TAG);
        int next = root.getInt(Integer.toString(dungeonId)) + 1;
        root.putInt(Integer.toString(dungeonId), next);
        persisted.put(TAG, root);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return next;
    }

    public static void clear(ServerPlayer player, int dungeonId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag root = persisted.getCompound(TAG);
        root.remove(Integer.toString(dungeonId));
        persisted.put(TAG, root);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private VisitData() {
    }
}
