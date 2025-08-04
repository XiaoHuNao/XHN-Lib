package com.xiaohunao.xhn_lib.common.network;

import com.xiaohunao.xhn_lib.XHN_Lib;
import com.xiaohunao.xhn_lib.common.network.bidir.TimeSyncPayload;
import com.xiaohunao.xhn_lib.common.network.s2c.DynamicLoaderSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = XHN_Lib.MODID, bus = EventBusSubscriber.Bus.MOD)
public class XHNNetwork {
    private static final String VERSION = "0.0.1";

    @SubscribeEvent
    public static void registerPayload(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToClient(DynamicLoaderSyncPayload.TYPE,DynamicLoaderSyncPayload.STREAM_CODEC, DynamicLoaderSyncPayload::clientHandle);

        registrar.playBidirectional(TimeSyncPayload.TYPE, TimeSyncPayload.STREAM_CODEC, new DirectionalPayloadHandler<>(TimeSyncPayload::clientHandle,TimeSyncPayload::serverHandle));


    }
}
