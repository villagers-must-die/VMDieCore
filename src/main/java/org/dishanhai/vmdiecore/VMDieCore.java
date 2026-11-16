package org.dishanhai.vmdiecore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.dishanhai.vmdiecore.command.DungeonCommands;
import org.dishanhai.vmdiecore.config.VMDConfig;
import org.dishanhai.vmdiecore.data.DungeonDefinitions;
import org.dishanhai.vmdiecore.dungeon.DungeonPool;
import org.dishanhai.vmdiecore.monster.MonsterPerspective;
import org.dishanhai.vmdiecore.registry.VMDRegistries;
import org.dishanhai.vmdiecore.world.OriginWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VMDieCore.MOD_ID)
public class VMDieCore {
    public static final String MOD_ID = "vmdiecore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VMDieCore(IEventBus modEventBus, ModContainer modContainer) {
        VMDRegistries.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, VMDConfig.SPEC);
        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(OriginWorld::onPlayerLogin);
        gameBus.addListener(OriginWorld::onMobSpawn);
        gameBus.addListener(DungeonDefinitions::onAddReload);
        gameBus.addListener(DungeonCommands::register);
        gameBus.addListener((ServerStartedEvent event) -> DungeonPool.restore(event.getServer()));
        gameBus.addListener((ServerStoppedEvent event) -> DungeonPool.clearAll());
        MonsterPerspective.initialize(modEventBus, modContainer);
    }
}
