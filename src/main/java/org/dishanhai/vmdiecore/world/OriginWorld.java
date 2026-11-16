package org.dishanhai.vmdiecore.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.registry.VMDRegistries;

public final class OriginWorld {
    public static final ResourceKey<Level> ORIGIN = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(VMDieCore.MOD_ID, "origin"));
    public static final double SPAWN_X = 0.5;
    public static final double SPAWN_Y = 64.0;
    public static final double SPAWN_Z = 0.5;
    public static final BlockPos SPAWN_POS = BlockPos.containing(SPAWN_X, SPAWN_Y, SPAWN_Z);

    private static final String PLAYER_INITIALIZED = "vmdiecore_initial_spawn";
    private static final String ORIGIN_DATA = "vmdiecore_origin";

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(PLAYER_INITIALIZED)) return;
        ServerLevel level = player.server.getLevel(ORIGIN);
        if (level == null) return;
        OriginData data = level.getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(OriginData::new, OriginData::load), ORIGIN_DATA);
        if (!data.initialized) {
            buildLobby(level);
            data.initialized = true;
            data.setDirty();
        }
        player.setRespawnPosition(ORIGIN, SPAWN_POS, 0.0F, true, false);
        player.teleportTo(level, SPAWN_X, SPAWN_Y, SPAWN_Z, 0.0F, 0.0F);
        persisted.putBoolean(PLAYER_INITIALIZED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void onMobSpawn(MobSpawnEvent.PositionCheck event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION && type != MobSpawnType.PATROL) return;
        ResourceKey<Level> dimension = event.getLevel().getLevel().dimension();
        if (dimension.equals(ORIGIN)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static boolean sendHome(ServerPlayer player) {
        ServerLevel level = player.server.getLevel(ORIGIN);
        if (level == null) return false;
        player.setRespawnPosition(ORIGIN, SPAWN_POS, 0.0F, true, false);
        player.teleportTo(level, SPAWN_X, SPAWN_Y, SPAWN_Z, 0.0F, 0.0F);
        return true;
    }

    public static void buildLobby(ServerLevel level) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                boolean wall = Math.abs(x) == 10 || Math.abs(z) == 10;
                level.setBlock(cursor.set(x, 63, z), stone, 2);
                for (int y = 64; y <= 70; y++) {
                    level.setBlock(cursor.set(x, y, z), wall ? stone : air, 2);
                }
                level.setBlock(cursor.set(x, 71, z), stone, 2);
            }
        }
        for (int[] lamp : new int[][]{
                {-10, -10}, {-10, 10}, {10, -10}, {10, 10},
                {-10, 0}, {10, 0}, {0, -10}, {0, 10},
                {-5, -10}, {5, 10}, {-10, 5}, {10, -5}}) {
            level.setBlock(cursor.set(lamp[0], 71, lamp[1]), Blocks.GLOWSTONE.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(4, 64, 4), VMDRegistries.DUNGEON_CONSOLE.get().defaultBlockState(), 3);
        BlockPos signPos = new BlockPos(5, 64, 5);
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), 3);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText front = new SignText()
                    .setMessage(0, Component.literal("地牢控制台"))
                    .setMessage(1, Component.literal("右键打开界面"));
            sign.setText(front, true);
            sign.setChanged();
        }
    }

    private OriginWorld() {
    }

    public static final class OriginData extends SavedData {
        private boolean initialized;

        private static OriginData load(CompoundTag tag, HolderLookup.Provider registries) {
            OriginData data = new OriginData();
            data.initialized = tag.getBoolean("initialized");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("initialized", initialized);
            return tag;
        }
    }
}
