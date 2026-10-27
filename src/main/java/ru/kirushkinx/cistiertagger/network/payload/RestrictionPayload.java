//? if >=1.20.5 {
package ru.kirushkinx.cistiertagger.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.20.5 {
import net.minecraft.resources.Identifier;
//?}
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;

/** Server -> client packet telling the client which display surfaces it must not draw. */
public record RestrictionPayload(boolean nametag, boolean tab, boolean chat) implements CustomPacketPayload {

//? if >=1.21 {
    public static final @NotNull Type<RestrictionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CisTierTagger.MOD_ID, "restrict"));
//?}
//? if =1.20.5 {
    /*public static final @NotNull Type<RestrictionPayload> TYPE = new Type<>(new Identifier(CisTierTagger.MOD_ID, "restrict"));
*///?}

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
//?}
