package org.dishanhai.vmdiecore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.dishanhai.vmdiecore.registry.VMDRegistries;

public class DungeonConsoleBlockEntity extends BlockEntity {
    public DungeonConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(VMDRegistries.CONSOLE_BE.get(), pos, state);
    }
}
