package org.dishanhai.vmdiecore.ui;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.dishanhai.vmdiecore.config.VMDConfig;
import org.dishanhai.vmdiecore.registry.VMDRegistries;

/** Carries the snapshot in the opening packet, before either side builds the widget tree. */
public final class DungeonConsoleMenu {
    // Keep below even the vanilla extra-menu-data limit, including the block position.
    private static final int MAX_SNAPSHOT_BYTES = 30_000;

    private DungeonConsoleMenu() {}

    public static boolean canUse(Player player, BlockPos pos) {
        return player.isAlive()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
                && player.level().getBlockState(pos).is(VMDRegistries.DUNGEON_CONSOLE.get())
                && (!VMDConfig.consoleRequireOp() || player.isCreative() || player.hasPermissions(2));
    }

    public static boolean open(ServerPlayer player, BlockPos pos) {
        if (!canUse(player, pos)) return false;
        DungeonConsoleSnapshot snapshot = DungeonConsoleSnapshot.capture(player);
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            snapshot.write(encoded);
            if (encoded.readableBytes() > MAX_SNAPSHOT_BYTES) {
                player.sendSystemMessage(Component.literal("地牢控制台数据过多，请使用地牢命令管理。"));
                return false;
            }
        } catch (RuntimeException exception) {
            player.sendSystemMessage(Component.literal("地牢控制台数据无法同步，请使用地牢命令管理。"));
            return false;
        } finally {
            encoded.release();
        }
        return player.openMenu(new Holder(player, pos, snapshot)).isPresent();
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inventory, RegistryFriendlyByteBuf data) {
        var holder = new Holder(inventory.player, data.readBlockPos(), DungeonConsoleSnapshot.read(data));
        return new ModularUIContainerMenu(VMDRegistries.CONSOLE_MENU.get(), windowId, inventory, holder);
    }

    public static final class Holder implements MenuProvider, IContainerUIHolder {
        public final Player player;
        public final BlockPos pos;
        public final DungeonConsoleSnapshot snapshot;

        public Holder(Player player, BlockPos pos, DungeonConsoleSnapshot snapshot) {
            this.player = player;
            this.pos = pos.immutable();
            this.snapshot = snapshot;
        }

        @Override
        public boolean isStillValid(Player player) {
            // Permission is authoritative on the server; COMMON config may differ on clients.
            return player.level().isClientSide || canUse(player, pos);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.vmdiecore.dungeon_console");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new ModularUIContainerMenu(VMDRegistries.CONSOLE_MENU.get(), id, inventory, this);
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            snapshot.write(buffer);
        }

        @Override
        public ModularUI createUI(Player player) {
            return DungeonConsoleUI.create(this);
        }
    }
}
