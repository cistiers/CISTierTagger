package ru.kirushkinx.cistiertagger.network;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.kirushkinx.cistiertagger.network.payload.HandshakePayload;
import ru.kirushkinx.cistiertagger.network.payload.RestrictionPayload;

@UtilityClass
public class RestrictionNetworking {

    public void register() {
        PayloadTypeRegistry.playS2C().register(RestrictionPayload.TYPE, RestrictionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HandshakePayload.TYPE, HandshakePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(RestrictionPayload.TYPE, (payload, context) -> {
            ServerRestrictions.apply(payload.nametag(), payload.tab(), payload.chat());
            if (ServerRestrictions.isAnyRestricted() && context.player() != null) {
                context.player().displayClientMessage(RestrictionNotice.message(), false);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sender.sendPacket(HandshakePayload.INSTANCE));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerRestrictions.clear());
    }
}
