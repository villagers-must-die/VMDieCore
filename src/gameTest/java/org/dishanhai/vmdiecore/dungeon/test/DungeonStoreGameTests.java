package org.dishanhai.vmdiecore.dungeon.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.dishanhai.vmdiecore.dungeon.DungeonStore;

@GameTestHolder("monsterperspective")
@PrefixGameTestTemplate(false)
public final class DungeonStoreGameTests {
    @GameTest(template = "empty")
    public static void deletingHighestIdDoesNotReuseItAfterRestart(GameTestHelper helper) {
        DungeonStore store = new DungeonStore();
        store.put(stored(7));
        store.remove(7);
        DungeonStore loaded = DungeonStore.load(store.save(new CompoundTag(), helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        helper.assertTrue(loaded.all().isEmpty(), "Deleted entry stays deleted");
        helper.assertTrue(loaded.nextId() == 8, "ID high-water mark survives an empty store and restart");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyStoreDerivesAndPersistsHighWaterMark(GameTestHelper helper) {
        DungeonStore store = new DungeonStore();
        store.put(stored(4));
        CompoundTag legacy = store.save(new CompoundTag(), helper.getLevel().registryAccess());
        legacy.remove("next_id");
        DungeonStore loaded = DungeonStore.load(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.nextId() == 5, "Legacy entries establish a safe next ID");
        loaded.remove(4);
        CompoundTag saved = loaded.save(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(saved.getInt("next_id") == 5, "Legacy migration persists its high-water mark");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingDefinitionKeepsManifestAndDimensionFiles(GameTestHelper helper) throws java.io.IOException {
        var server = helper.getLevel().getServer();
        DungeonStore store = DungeonStore.get(server);
        int id = store.nextId();
        var entry = stored(id);
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, entry.key());
        var directory = ((org.dishanhai.vmdiecore.mixin.ServerLevelsAccess) server).getStorageSource().getDimensionPath(key);
        helper.assertTrue(!java.nio.file.Files.exists(directory), "Test dimension must not already exist");
        java.nio.file.Files.createDirectories(directory);
        var marker = directory.resolve("retained-test-data");
        java.nio.file.Files.writeString(marker, "player chest data");
        store.put(entry);
        try {
            org.dishanhai.vmdiecore.dungeon.DungeonPool.restore(server);
            helper.assertTrue(store.all().contains(entry), "Missing definition must not remove manifest entry");
            helper.assertTrue(java.nio.file.Files.exists(marker), "Missing definition must not erase dimension files");
            DungeonStore reloaded = DungeonStore.load(store.save(new CompoundTag(), helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            helper.assertTrue(reloaded.all().contains(entry), "Quarantined entry survives another save and load");
        } finally {
            store.remove(id);
            java.nio.file.Files.deleteIfExists(marker);
            java.nio.file.Files.deleteIfExists(directory);
        }
        helper.succeed();
    }

    private static DungeonStore.Stored stored(int id) {
        return new DungeonStore.Stored(id, "example:temporarily_missing", 1, true, 1L,
                ResourceLocation.fromNamespaceAndPath("vmdiecore", "dungeon_" + id + "_abcd"), BlockPos.ZERO, 42L);
    }
}
