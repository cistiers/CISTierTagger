package ru.kirushkinx.cistiertagger.network;

import lombok.experimental.UtilityClass;
//? if <26.2 {
/*import net.minecraft.network.Connection;
*///?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if =26.2 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.Connection;
import ru.kirushkinx.cistiertagger.network.payload.HandshakePayload;
import ru.kirushkinx.cistiertagger.network.payload.RestrictionPayload;
//?}
//? if >=1.20.5 <26.2 {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.kirushkinx.cistiertagger.network.payload.HandshakePayload;
import ru.kirushkinx.cistiertagger.network.payload.RestrictionPayload;
*///?}
//? if <1.20.5 {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resources.Identifier;
import ru.kirushkinx.cistiertagger.CisTierTagger;
*///?}

@UtilityClass
public class RestrictionNetworking {

    private final ConnectionNoticeState<Connection> notice = new ConnectionNoticeState<>();

//? if >=26.1 {
    public void register() {
        PayloadTypeRegistry.clientboundPlay().register(RestrictionPayload.TYPE, RestrictionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
//?}
//? if >=1.20.5 <26.1 {
    /*public void register() {
        PayloadTypeRegistry.playS2C().register(RestrictionPayload.TYPE, RestrictionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
*///?}
//? if <1.20.5 {
    /*private final Identifier HANDSHAKE = new Identifier(CisTierTagger.MOD_ID, "handshake");
    private final Identifier RESTRICT = new Identifier(CisTierTagger.MOD_ID, "restrict");
*///?}

//? if >=26.1 {
        ClientPlayNetworking.registerGlobalReceiver(RestrictionPayload.TYPE, (payload, context) -> {
            ServerRestrictions.apply(payload.nametag(), payload.tab(), payload.chat());
            if (ServerRestrictions.isAnyRestricted()
                    && context.player() != null
                    && notice.shouldNotify(context.player().connection.getConnection())) {
                context.player().sendSystemMessage(RestrictionNotice.message());
            }
//?}
//? if >=1.20.5 <26.1 {
        /*ClientPlayNetworking.registerGlobalReceiver(RestrictionPayload.TYPE, (payload, context) -> {
            ServerRestrictions.apply(payload.nametag(), payload.tab(), payload.chat());
            if (ServerRestrictions.isAnyRestricted()
                    && context.player() != null
                    && notice.shouldNotify(context.player().connection.getConnection())) {
                context.player().displayClientMessage(RestrictionNotice.message(), false);
            }
*///?}
//? if <1.20.5 {
    /*public void register() {
        ClientPlayNetworking.registerGlobalReceiver(RESTRICT, (client, handler, buf, responseSender) -> {
            boolean nametag = buf.readBoolean();
            boolean tab = buf.readBoolean();
            boolean chat = buf.readBoolean();
            client.execute(() -> {
                ServerRestrictions.apply(nametag, tab, chat);
                if (ServerRestrictions.isAnyRestricted()
                        && client.player != null
                        && notice.shouldNotify(handler.getConnection())) {
                    client.player.displayClientMessage(RestrictionNotice.message(), false);
                }
            });
*///?}
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Channel registration follows join
//? if >=1.21.2 {
            client.schedule(() -> {
//?}
//? if <1.21.2 {
            /*client.tell(() -> {
*///?}
                if (client.getConnection() == handler && handler.getConnection().isConnected()) {
//? if >=1.20.5 {
                    sender.sendPacket(HandshakePayload.INSTANCE);
//?}
//? if <1.20.5 {
                    /*ClientPlayNetworking.send(HANDSHAKE, PacketByteBufs.create());
*///?}
                }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerRestrictions.clear());
    }
}
