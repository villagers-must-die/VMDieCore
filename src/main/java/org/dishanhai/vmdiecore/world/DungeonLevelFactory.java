package org.dishanhai.vmdiecore.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.api.DungeonProfile;
import org.dishanhai.vmdiecore.api.GeneratorSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 按维度档建地牢维度。
 */
public final class DungeonLevelFactory {
    public static final long DAY_TIME = 13000L;

    public static ResourceKey<Level> key(int id) {
        int salt = ThreadLocalRandom.current().nextInt(0x1000, 0x10000);
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(VMDieCore.MOD_ID,
                        "dungeon_" + id + "_" + Integer.toHexString(salt)));
    }

    public static DungeonLevel create(MinecraftServer server, int id, DungeonProfile profile) {
        return newLevel(server, key(id), profile, server.overworld().getRandom().nextLong(), id);
    }

    public static DungeonLevel revive(MinecraftServer server, int id, ResourceKey<Level> key, BlockPos anchor,
                                      DungeonProfile profile, long seed) {
        DungeonLevel level = newLevel(server, key, profile, seed, id);
        level.setAnchor(anchor);
        return level;
    }

    private static DungeonLevel newLevel(MinecraftServer server, ResourceKey<Level> key,
                                         DungeonProfile profile, long seed, int id) {
        LevelStem stem = new LevelStem(dimensionType(server, profile.dimensionType()),
                generator(server, profile.generator()));
        DungeonLevel.prepareSeed(seed);
        try {
            DungeonLevel level = new DungeonLevel(server,
                    ((org.dishanhai.vmdiecore.mixin.ServerLevelsAccess) server).getExecutor(),
                    ((org.dishanhai.vmdiecore.mixin.ServerLevelsAccess) server).getStorageSource(),
                    new DungeonLevelData(server.getWorldData(), server.getWorldData().overworldData()),
                    key, stem, new EmptyProgressListener(), seed, id);
            level.setLandingOffset(profile.jigsaw().landingOffset());
            return level;
        } finally {
            DungeonLevel.prepareSeed(0L);
        }
    }

    private static Holder<DimensionType> dimensionType(MinecraftServer server, ResourceLocation id) {
        ResourceKey<DimensionType> key = ResourceKey.create(Registries.DIMENSION_TYPE, id);
        return server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(key);
    }

    private static ChunkGenerator generator(MinecraftServer server, GeneratorSettings settings) {
        if (settings instanceof GeneratorSettings.Noise noise) {
            return noiseGenerator(server, noise);
        }
        if (settings instanceof GeneratorSettings.Flat flat) {
            return flatGenerator(server, flat);
        }
        return emptyGenerator(server);
    }

    private static ChunkGenerator noiseGenerator(MinecraftServer server, GeneratorSettings.Noise noise) {
        var settings = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .get(ResourceKey.create(Registries.NOISE_SETTINGS, noise.settings()))
                .orElseThrow(() -> new IllegalArgumentException("未知的 noise settings：" + noise.settings()));
        return new NoiseBasedChunkGenerator(biomeSource(server, noise.biomeSource()), settings);
    }

    private static BiomeSource biomeSource(MinecraftServer server, JsonElement json) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        if (!json.isJsonPrimitive()) {
            return BiomeSource.CODEC.parse(ops, json).getOrThrow(
                    message -> new IllegalArgumentException("biome_source 无法解析：" + message));
        }
        ResourceLocation id = ResourceLocation.parse(json.getAsString());
        var preset = server.registryAccess()
                .lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .get(ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, id));
        if (preset.isPresent()) {
            return MultiNoiseBiomeSource.createFromPreset(preset.get());
        }
        MapCodec<? extends BiomeSource> codec = server.registryAccess()
                .lookupOrThrow(Registries.BIOME_SOURCE)
                .get(ResourceKey.create(Registries.BIOME_SOURCE, id))
                .map(Holder::value)
                .orElseThrow(() -> new IllegalArgumentException("未知的 biome_source：" + id));
        return codec.codec().parse(ops, new JsonObject()).getOrThrow(message ->
                new IllegalArgumentException("biome_source " + id + " 需要参数，请改写成对象形式：" + message));
    }

    private static ChunkGenerator flatGenerator(MinecraftServer server, GeneratorSettings.Flat flat) {
        List<FlatLayerInfo> layers = new ArrayList<>();
        for (GeneratorSettings.Layer layer : flat.layers()) {
            Block block = BuiltInRegistries.BLOCK.get(layer.block());
            layers.add(new FlatLayerInfo(layer.height(), block));
        }
        Holder<Biome> biome = server.registryAccess().lookupOrThrow(Registries.BIOME)
                .getOrThrow(ResourceKey.create(Registries.BIOME, flat.biome()));
        return new FlatLevelSource(flatSettings(server).withBiomeAndLayers(layers,
                Optional.<HolderSet<StructureSet>>empty(), biome));
    }

    private static ChunkGenerator emptyGenerator(MinecraftServer server) {
        Holder<Biome> biome = server.registryAccess().lookupOrThrow(Registries.BIOME)
                .getOrThrow(Biomes.PLAINS);
        return new FlatLevelSource(flatSettings(server).withBiomeAndLayers(List.of(),
                Optional.<HolderSet<StructureSet>>empty(), biome));
    }

    private static FlatLevelGeneratorSettings flatSettings(MinecraftServer server) {
        return FlatLevelGeneratorSettings.getDefault(
                server.registryAccess().lookupOrThrow(Registries.BIOME),
                server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),
                server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE));
    }

    public static boolean placeStructure(DungeonLevel level, BlockPos origin, List<ResourceLocation> pools,
                                         DungeonProfile.Jigsaw jigsaw) {
        if (pools.isEmpty()) {
            VMDieCore.LOGGER.warn("结构池列表为空");
            return false;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        ResourceLocation target = ResourceLocation.withDefaultNamespace("empty");
        for (ResourceLocation candidate : pools) {
            var holder = registry.getHolder(candidate);
            if (holder.isEmpty()) {
                VMDieCore.LOGGER.warn("结构池 {} 不存在", candidate);
                continue;
            }
            if (JigsawPlacement.generateJigsaw(level, holder.get(), target, jigsaw.depth(), origin,
                    jigsaw.keepJigsaws())) {
                VMDieCore.LOGGER.info("使用结构池 {}", candidate);
                return true;
            }
            VMDieCore.LOGGER.warn("结构池 {} 拼装失败", candidate);
        }
        return false;
    }

    public static BlockPos randomAnchor(DungeonLevel level, DungeonProfile.Anchor anchor) {
        RandomSource random = level.getRandom();
        return new BlockPos(
                random.nextIntBetweenInclusive(-anchor.radius(), anchor.radius()),
                random.nextIntBetweenInclusive(anchor.minY(), anchor.maxY()),
                random.nextIntBetweenInclusive(-anchor.radius(), anchor.radius()));
    }

    public static void prepareLanding(DungeonLevel level, BlockPos landing, DungeonProfile.Landing config) {
        Block floor = BuiltInRegistries.BLOCK.get(config.floor());
        int half = Math.max(0, config.size() / 2);
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                level.setBlock(landing.below().offset(x, 0, z), floor.defaultBlockState(), 2);
                for (int y = 0; y <= 2; y++) {
                    level.setBlock(landing.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    public static void forceLoad(DungeonLevel level, BlockPos pos) {
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
    }

    private DungeonLevelFactory() {
    }
}
