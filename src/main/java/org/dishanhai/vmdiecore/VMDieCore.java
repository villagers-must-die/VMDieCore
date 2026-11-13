package org.dishanhai.vmdiecore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.fml.common.Mod;

@Mod(VMDieCore.MOD_ID)
public class VMDieCore {
    public static final String MOD_ID = "vmdiecore";
    private static final ResourceKey<Level> ORIGIN = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MOD_ID, "origin"));
    private static final BlockPos SPAWN = new BlockPos(0, 65, 0);
    private static final String PLAYER_INITIALIZED = "vmdiecore_initial_spawn";

    public VMDieCore() {
        NeoForge.EVENT_BUS.addListener(VMDieCore::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(VMDieCore::onMobSpawn);
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(PLAYER_INITIALIZED)) return;
        ServerLevel level = player.server.getLevel(ORIGIN);
        if (level == null) return;
        OriginData data = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(OriginData::new, OriginData::load), "vmdiecore_origin");
        if (!data.initialized) {
            createSpawnRoom(level);
            data.initialized = true;
            data.setDirty();
        }
        player.setRespawnPosition(ORIGIN, SPAWN, 0, true, false);
        player.teleportTo(level, 0.5, 65, 0.5, 0, 0);
        persisted.putBoolean(PLAYER_INITIALIZED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void onMobSpawn(MobSpawnEvent.PositionCheck event) {
        MobSpawnType type = event.getSpawnType();
        if (event.getLevel().getLevel().dimension().equals(ORIGIN)
                && (type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private static void createSpawnRoom(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = 63; y <= 70; y++) {
                    boolean boundary = x == -4 || x == 4 || z == -4 || z == 4 || y == 63 || y == 70;
                    var state = boundary ? Blocks.BEDROCK.defaultBlockState() : y == 64 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    level.setBlock(pos.set(x, y, z), state, 2);
                }
            }
        }
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-2, 2}) {
                level.setBlock(new BlockPos(x, 65, z), Blocks.TORCH.defaultBlockState(), 3);
            }
        }
    }

    private static final class OriginData extends SavedData {
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
