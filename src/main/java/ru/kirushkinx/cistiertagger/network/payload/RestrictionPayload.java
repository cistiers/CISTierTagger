package ru.kirushkinx.cistiertagger.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;

/** Server -> client packet telling the client which display surfaces it must not draw. */
public record RestrictionPayload(boolean nametag, boolean tab, boolean chat) implements CustomPacketPayload {

    public static final @NotNull Type<RestrictionPayload> TYPE = new Type<>(new ResourceLocation(CisTierTagger.MOD_ID, "restrict"));

    public static final @NotNull StreamCodec<RegistryFriendlyByteBuf, RestrictionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.nametag);
                buf.writeBoolean(payload.tab);
                buf.writeBoolean(payload.chat);
            },
            buf -> new RestrictionPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

    @Override
    public @NotNull Type<RestrictionPayload> type() {
        return TYPE;
    }
}
