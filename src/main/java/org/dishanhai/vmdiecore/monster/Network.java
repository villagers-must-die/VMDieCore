package org.dishanhai.vmdiecore.monster;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Network {
    public record Sync(UUID player, String race, boolean open) implements CustomPacketPayload {
        public static final Type<Sync> TYPE=new Type<>(MonsterPerspective.id("sync"));
        public static final StreamCodec<FriendlyByteBuf,Sync> CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.player);b.writeUtf(p.race,32);b.writeBoolean(p.open);},
            b->new Sync(b.readUUID(),b.readUtf(32),b.readBoolean()));
        public Type<Sync> type(){return TYPE;}
    }
    public record Choose(String race) implements CustomPacketPayload {
        public static final Type<Choose> TYPE=new Type<>(MonsterPerspective.id("choose"));
        public static final StreamCodec<FriendlyByteBuf,Choose> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.race,32),b->new Choose(b.readUtf(32)));
        public Type<Choose> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e) {
        var r=e.registrar("1");
        r.playToClient(Sync.TYPE,Sync.CODEC,(p,c)->org.dishanhai.vmdiecore.monster.client.ClientState.accept(p));
        r.playToServer(Choose.TYPE,Choose.CODEC,(p,c)->{
            if(c.player() instanceof ServerPlayer sp && Race.parse(p.race)!=null) RaceEvents.choose(sp,Race.parse(p.race),false);
        });
    }
    public static Sync packet(ServerPlayer p,boolean open){return new Sync(p.getUUID(),p.getPersistentData().getString("mp_race"),open);}
    public static void sync(ServerPlayer p){PacketDistributor.sendToAllPlayers(packet(p,false));}
}
