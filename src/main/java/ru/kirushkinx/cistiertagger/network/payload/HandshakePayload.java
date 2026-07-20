package ru.kirushkinx.cistiertagger.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;

/** Client -> server announce on join so the server knows this client runs the mod. */
public record HandshakePayload() implements CustomPacketPayload {

    public static final @NotNull HandshakePayload INSTANCE = new HandshakePayload();

    public static final @NotNull Type<HandshakePayload> TYPE = new Type<>(new ResourceLocation(CisTierTagger.MOD_ID, "handshake"));

    public static final @NotNull StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<HandshakePayload> type() {
        return TYPE;
    }
}
