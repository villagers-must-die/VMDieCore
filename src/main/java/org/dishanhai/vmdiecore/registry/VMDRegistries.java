package org.dishanhai.vmdiecore.registry;

import net.minecraft.core.registries.Registries;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import org.dishanhai.vmdiecore.ui.DungeonConsoleMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.block.DungeonConsoleBlock;
import org.dishanhai.vmdiecore.block.DungeonConsoleBlockEntity;

public final class VMDRegistries {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, VMDieCore.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ModularUIContainerMenu>> CONSOLE_MENU =
            MENUS.register("dungeon_console", () -> IMenuTypeExtension.create(DungeonConsoleMenu::create));

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, VMDieCore.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, VMDieCore.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VMDieCore.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VMDieCore.MOD_ID);

    public static final DeferredHolder<Block, DungeonConsoleBlock> DUNGEON_CONSOLE =
            BLOCKS.register("dungeon_console", () -> new DungeonConsoleBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredHolder<Item, BlockItem> DUNGEON_CONSOLE_ITEM =
            ITEMS.register("dungeon_console", () -> new BlockItem(DUNGEON_CONSOLE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DungeonConsoleBlockEntity>> CONSOLE_BE =
            BLOCK_ENTITIES.register("dungeon_console",
                    () -> BlockEntityType.Builder.of(DungeonConsoleBlockEntity::new, DUNGEON_CONSOLE.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("vmdiecore", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.vmdiecore"))
                    .icon(() -> new ItemStack(DUNGEON_CONSOLE_ITEM.get()))
                    .displayItems((params, output) -> output.accept(DUNGEON_CONSOLE_ITEM.get()))
                    .build());

    public static void register(IEventBus bus) {
        MENUS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }

    private VMDRegistries() {
    }
}
