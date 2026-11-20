package org.dishanhai.vmdiecore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
abstract class MetalScaffoldingCollisionMixin {
    @Unique
    private static final ResourceKey<Block> vmdiecore$METAL_SCAFFOLDING = ResourceKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("kubejs", "metal_scaffolding"));

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void vmdiecore$metalScaffoldingCollision(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context, CallbackInfoReturnable<VoxelShape> result) {
        // Keep the original shape for context-free queries (including the state cache).
        if (context == CollisionContext.empty()
                || !state.getBlock().builtInRegistryHolder().is(vmdiecore$METAL_SCAFFOLDING)) {
            return;
        }

        VoxelShape full = Shapes.block();
        result.setReturnValue(context.isAbove(full, pos, true) ? full : Shapes.empty());
    }
}
