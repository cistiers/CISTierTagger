package ru.kirushkinx.cistiertagger.network;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resources.ResourceLocation;
import ru.kirushkinx.cistiertagger.CisTierTagger;

@UtilityClass
public class RestrictionNetworking {

    private final ResourceLocation HANDSHAKE = new ResourceLocation(CisTierTagger.MOD_ID, "handshake");
    private final ResourceLocation RESTRICT = new ResourceLocation(CisTierTagger.MOD_ID, "restrict");

    public void register() {
        ClientPlayNetworking.registerGlobalReceiver(RESTRICT, (client, handler, buf, responseSender) -> {
            boolean nametag = buf.readBoolean();
            boolean tab = buf.readBoolean();
            boolean chat = buf.readBoolean();
            client.execute(() -> {
                ServerRestrictions.apply(nametag, tab, chat);
                if (ServerRestrictions.isAnyRestricted() && client.player != null) {
                    client.player.displayClientMessage(RestrictionNotice.message(), false);
                }
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientPlayNetworking.send(HANDSHAKE, PacketByteBufs.create()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerRestrictions.clear());
    }
}
