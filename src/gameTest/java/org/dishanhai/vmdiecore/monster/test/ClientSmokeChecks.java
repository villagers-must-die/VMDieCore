package org.dishanhai.vmdiecore.monster.test;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.dishanhai.vmdiecore.VMDieCore;
import org.dishanhai.vmdiecore.monster.client.ArchiveScreen;
import org.dishanhai.vmdiecore.monster.client.ClientEntrypoint;

/** Opt-in startup check. Never opens a world and is excluded from the release JAR. */
@EventBusSubscriber(modid = VMDieCore.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeChecks {
    @SubscribeEvent
    public static void skipFirstRunPrompt(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("vmdiecore.clientSmokeTest")) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof AccessibilityOnboardingScreen) client.setScreen(new TitleScreen());
    }

    @SubscribeEvent
    public static void titleReady(ScreenEvent.Init.Post event) {
        if (!Boolean.getBoolean("vmdiecore.clientSmokeTest") || !(event.getScreen() instanceof TitleScreen)) return;
        Minecraft client = Minecraft.getInstance();
        if (!Arrays.asList(client.options.keyMappings).contains(ClientEntrypoint.ARCHIVE)) {
            throw new IllegalStateException("Migrated monster archive key was not registered by VMDieCore");
        }
        if (!I18n.exists("key.monsterperspective.archive")) {
            throw new IllegalStateException("Legacy namespace language resources were not loaded");
        }
        if (new ArchiveScreen().getTitle().getString().isEmpty()) {
            throw new IllegalStateException("Migrated archive screen could not be constructed");
        }
        LogUtils.getLogger().info("VMDieCore client startup checks passed");
        client.execute(client::stop);
    }
}
