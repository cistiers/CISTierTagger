package ru.kirushkinx.cistiertagger.network;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.gui.Layout;
import ru.kirushkinx.cistiertagger.network.payload.HandshakePayload;
import ru.kirushkinx.cistiertagger.network.payload.RestrictionPayload;

@UtilityClass
public class RestrictionNetworking {

    private static final int ACCENT = Layout.COLOR_WARN & 0xFFFFFF;
    private static final int DIM = Layout.COLOR_DIM & 0xFFFFFF;

    public void register() {
        PayloadTypeRegistry.playS2C().register(RestrictionPayload.TYPE, RestrictionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HandshakePayload.TYPE, HandshakePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(RestrictionPayload.TYPE, (payload, context) -> {
            ServerRestrictions.apply(payload.nametag(), payload.tab(), payload.chat());
            if (ServerRestrictions.isAnyRestricted() && context.player() != null) {
                context.player().displayClientMessage(message(), false);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sender.sendPacket(HandshakePayload.INSTANCE));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerRestrictions.clear());
    }

    private static Component message() {
        MutableComponent hover = Component.translatable("cistiertagger.restriction.hover")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DIM)));
        if (ServerRestrictions.isNametagRestricted()) hover.append(surface("nametag"));
        if (ServerRestrictions.isTabRestricted()) hover.append(surface("tab"));
        if (ServerRestrictions.isChatRestricted()) hover.append(surface("chat"));

        return Component.literal("ℹ ")
                .append(Component.translatable("cistiertagger.restriction.chat"))
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(ACCENT))
                        .withHoverEvent(new HoverEvent.ShowText(hover)));
    }

    private static Component surface(@NotNull String key) {
        return Component.literal("\n- ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DIM)))
                .append(Component.translatable("cistiertagger.button." + key)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(ACCENT))));
    }
}
