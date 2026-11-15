package org.dishanhai.vmdiecore.monster.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.dishanhai.vmdiecore.monster.MonsterPerspective;
import org.dishanhai.vmdiecore.VMDieCore;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;

@Mod(value=VMDieCore.MOD_ID,dist=Dist.CLIENT)
public final class ClientEntrypoint {
    public static final KeyMapping ARCHIVE=new KeyMapping("key.monsterperspective.archive",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_O,"key.categories.monsterperspective");
    public ClientEntrypoint(IEventBus bus){
        bus.addListener((RegisterKeyMappingsEvent e)->e.register(ARCHIVE));
        NeoForge.EVENT_BUS.register(new ClientEvents());
    }
}
