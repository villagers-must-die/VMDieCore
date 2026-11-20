package org.dishanhai.vmdiecore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("monsterperspective")
@PrefixGameTestTemplate(false)
public final class MetalScaffoldingGameTests {
    @GameTest(template = "empty")
    public static void ordinaryVanillaBlocksKeepCollision(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(pos));
        var context = CollisionContext.of(player);
        helper.assertTrue(Block.isShapeFullBlock(Blocks.STONE.defaultBlockState()
                .getCollisionShape(helper.getLevel(), pos, context)), "Stone stays solid even inside the block");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scriptedMetalScaffoldingUsesApproachHeight(GameTestHelper helper) {
        if (!ModList.get().isLoaded("kubejs")) {
            // KubeJS is an optional runtime dependency. Supply it to run this integration case.
            helper.succeed();
            return;
        }

        var target = BuiltInRegistries.BLOCK.getOptional(kubejs("metal_scaffolding")).orElseThrow();
        var control = BuiltInRegistries.BLOCK.getOptional(kubejs("metal_scaffolding_control")).orElseThrow();
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var base = Vec3.atBottomCenterOf(pos);
        for (boolean bottom : new boolean[]{false, true}) {
            var state = target.defaultBlockState().setValue(BlockStateProperties.BOTTOM, bottom);
            level.setBlockAndUpdate(pos, state);
            player.setPos(base.add(0, 1.25, 0));
            helper.assertTrue(Block.isShapeFullBlock(state.getCollisionShape(level, pos, CollisionContext.of(player))),
                    "The upper surface supports falling entities for both BOTTOM states");
            player.move(MoverType.SELF, new Vec3(0, -0.5, 0));
            helper.assertTrue(Math.abs(player.getY() - (pos.getY() + 1.0)) < 1.0e-5,
                    "Entity movement lands at the exact upper surface");
            player.setPos(base.add(0, 0.2, 0));
            var inside = CollisionContext.of(player);
            helper.assertTrue(state.getCollisionShape(level, pos, inside).isEmpty(), "Side/inside movement is open");
            helper.assertTrue(player.onClimbable(), "The existing climbable tag still works inside");
            helper.assertTrue(Block.isShapeFullBlock(state.getShape(level, pos, inside)), "Selection outline stays full");
            helper.assertTrue(Block.isShapeFullBlock(state.getCollisionShape(level, pos, CollisionContext.empty())),
                    "Context-free collision queries keep the scripted shape");
            helper.assertTrue(Block.isShapeFullBlock(control.defaultBlockState().getCollisionShape(level, pos, inside)),
                    "Other scripted blocks keep their original collision");
            player.setPos(base.add(0, -0.25, 0));
            helper.assertTrue(state.getCollisionShape(level, pos, CollisionContext.of(player)).isEmpty(),
                    "An entity below the block can move upward through it");
        }
        helper.succeed();
    }

    private static ResourceLocation kubejs(String path) {
        return ResourceLocation.fromNamespaceAndPath("kubejs", path);
    }
}
