package org.dishanhai.vmdiecore.block;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.dishanhai.vmdiecore.config.VMDConfig;
import org.dishanhai.vmdiecore.ui.DungeonConsoleUI;

public class DungeonConsoleBlock extends Block implements EntityBlock, BlockUIMenuType.BlockUI {
    public DungeonConsoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonConsoleBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        // 配置要求权限时只放行创造模式或二级权限。
        if (VMDConfig.consoleRequireOp() && !player.isCreative() && !player.hasPermissions(2)) {
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return DungeonConsoleUI.create(holder);
    }
}
