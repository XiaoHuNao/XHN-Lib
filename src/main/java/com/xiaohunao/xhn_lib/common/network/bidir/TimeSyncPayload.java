package com.xiaohunao.xhn_lib.common.network.bidir;


import com.xiaohunao.xhn_lib.XHN_Lib;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TimeSyncPayload(long time) implements CustomPacketPayload {
    public static final Type<TimeSyncPayload> TYPE = new Type<>(XHN_Lib.asResource("time_sync"));
    public static final StreamCodec<ByteBuf, TimeSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TimeSyncPayload::time,
            TimeSyncPayload::new
    );

    @Override @NotNull
    public CustomPacketPayload. Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandle(final TimeSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.isLocalPlayer()) {
                ((ClientLevel)player.level()).setDayTime(payload.time);
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
    public static void serverHandle(final TimeSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ((ServerLevel)player.level()).setDayTime(payload.time);
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}
