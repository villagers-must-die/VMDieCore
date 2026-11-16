package org.dishanhai.vmdiecore.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.dishanhai.vmdiecore.api.DungeonInfo;
import org.dishanhai.vmdiecore.registry.VMDRegistries;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("monsterperspective")
@PrefixGameTestTemplate(false)
public final class DungeonConsoleGameTests {
    @GameTest(template = "empty")
    public static void consoleSnapshotRoundTripBuildsSameTree(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath("snapshot_test", "remote_only");
        var source = new DungeonConsoleSnapshot(8, 2, 3,
                List.of(new DungeonConsoleSnapshot.Theme(id, "仅服务端种类", List.of(
                        new DungeonConsoleSnapshot.Tier(1, "第一层"),
                        new DungeonConsoleSnapshot.Tier(2, "第二层")))),
                List.of(new DungeonInfo(7, id, "仅服务端种类", 2, "第二层", true, 4)));
        FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.write(bytes);
            var decoded = DungeonConsoleSnapshot.read(bytes);
            helper.assertTrue(source.equals(decoded), "All menu fields survive the opening packet");
            helper.assertTrue(bytes.readableBytes() == 0, "Decoder consumes exactly the snapshot");
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var serverUI = DungeonConsoleUI.create(new DungeonConsoleMenu.Holder(player, BlockPos.ZERO, source));
            var remoteUI = DungeonConsoleUI.create(new DungeonConsoleMenu.Holder(player, BlockPos.ZERO, decoded));
            List<String> serverTree = new ArrayList<>();
            List<String> remoteTree = new ArrayList<>();
            describe(serverUI.ui.rootElement, serverTree);
            describe(remoteUI.ui.rootElement, remoteTree);
            helper.assertTrue(serverTree.equals(remoteTree), "The opening snapshot produces the same ordered widget tree");
            helper.assertTrue(remoteTree.stream().anyMatch(s -> s.contains("仅服务端种类")),
                    "Menu uses its snapshot even when the theme was never registered locally");
            helper.assertTrue(remoteTree.stream().anyMatch(s -> s.contains("次数4")), "Player-specific visits are synchronized");
        } finally {
            bytes.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void consoleSnapshotDoesNotLeakBetweenMenus(GameTestHelper helper) {
        var source = new DungeonConsoleSnapshot(8, 0, 0, List.of(), List.of());
        List<DungeonConsoleSnapshot.Theme> mutable = new ArrayList<>();
        var isolated = new DungeonConsoleSnapshot(8, 0, 0, mutable, List.of());
        mutable.add(new DungeonConsoleSnapshot.Theme(ResourceLocation.withDefaultNamespace("test"), "other", List.of()));
        helper.assertTrue(isolated.equals(source), "Snapshot copies source lists rather than retaining shared state");
        FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.write(bytes);
            var decoded = DungeonConsoleSnapshot.read(bytes);
            helper.assertTrue(decoded.themes().isEmpty() && decoded.dungeons().isEmpty(), "Empty snapshot remains empty");
        } finally {
            bytes.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void consoleActionsRequireNearbyConsole(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, VMDRegistries.DUNGEON_CONSOLE.get().defaultBlockState());
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        helper.assertTrue(DungeonConsoleMenu.canUse(player, pos), "Authorized nearby player can use console");
        player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
        helper.assertTrue(!DungeonConsoleMenu.canUse(player, pos), "Walking away invalidates outstanding callbacks");
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        helper.getLevel().removeBlock(pos, false);
        helper.assertTrue(!DungeonConsoleMenu.canUse(player, pos), "Breaking the console invalidates outstanding callbacks");
        helper.succeed();
    }

    private static void describe(UIElement element, List<String> nodes) {
        nodes.add(element.getClass().getName() + (element instanceof TextElement text ? ":" + text.getText().getString() : ""));
        for (UIElement child : element.getChildren()) describe(child, nodes);
    }
}
