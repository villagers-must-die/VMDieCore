package org.dishanhai.vmdiecore.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.registry.VMDRegistries;

@EventBusSubscriber(modid = VMDieCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DungeonConsoleScreens {
    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(VMDRegistries.CONSOLE_MENU.get(), ModularUIContainerScreen::new);
    }

    private DungeonConsoleScreens() {}
}
