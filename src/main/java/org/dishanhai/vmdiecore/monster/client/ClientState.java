package org.dishanhai.vmdiecore.monster.client;

import java.util.*;
import org.dishanhai.vmdiecore.monster.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class ClientState {
    public static final Map<UUID,String> RACES=new HashMap<>();
    private static boolean pendingOpen;
    public static void accept(Network.Sync p){
        RACES.put(p.player(),p.race());var mc=Minecraft.getInstance();
        if(mc.level!=null){Player player=mc.level.getPlayerByUUID(p.player());if(player!=null){player.getPersistentData().putString("mp_race",p.race());player.refreshDimensions();}}
        if(p.open())pendingOpen=true;
    }
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null)return;
        for(Player p:mc.level.players()){
            String id=RACES.get(p.getUUID());
            if(id!=null && !id.equals(p.getPersistentData().getString("mp_race"))){p.getPersistentData().putString("mp_race",id);p.refreshDimensions();}
        }
        if(pendingOpen && mc.screen==null){pendingOpen=false;mc.setScreen(new ArchiveScreen());}
    }
    public static void clear(){RACES.clear();pendingOpen=false;ClientEvents.clear();}
}
