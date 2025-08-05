package com.xiaohunao.xhn_lib.common.network.s2c;


import com.google.gson.JsonElement;
import com.xiaohunao.xhn_lib.XHN_Lib;
import com.xiaohunao.xhn_lib.api.register.FlexibleRegister;
import com.xiaohunao.xhn_lib.api.register.FlexibleRegisterManager;
import com.xiaohunao.xhn_lib.common.codec.XHNStreamCodecs;
import com.xiaohunao.xhn_lib.common.util.RegistryUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public record DynamicLoaderSyncPayload(String modId,ResourceLocation registry,ResourceLocation key, JsonElement data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DynamicLoaderSyncPayload> TYPE = new CustomPacketPayload.Type<>(XHN_Lib.asResource("dynamic_loader_s2c_sync"));


    public static final StreamCodec<ByteBuf, DynamicLoaderSyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,DynamicLoaderSyncPayload::modId,
        ResourceLocation.STREAM_CODEC, DynamicLoaderSyncPayload::registry,
        ResourceLocation.STREAM_CODEC, DynamicLoaderSyncPayload::key,
        XHNStreamCodecs.JSON_ELEMENT, DynamicLoaderSyncPayload::data,
        DynamicLoaderSyncPayload::new
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicLoaderSyncPayload.class);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandle(final DynamicLoaderSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!player.isLocalPlayer()) {
                return;
            }

            FlexibleRegisterManager registerManager = FlexibleRegisterManager.INSTANCE;
            ResourceKey<Registry<Object>> registryKey = ResourceKey.createRegistryKey(payload.registry());

            MappedRegistry registry = null;


            if (BuiltInRegistries.REGISTRY.containsKey(payload.registry())) {
                Registry<?> registry1 = BuiltInRegistries.REGISTRY.get(payload.registry());
                if (registry1 != null){
                    registry = (MappedRegistry)registry1;
                }
            }

            if (registry == null) {
                LOGGER.warn("Registry is null for key: {}", payload.registry());
                return;
            }

            if (registry.containsKey(payload.key)){
                LOGGER.warn("Dynamic registration for existing key: {} in registry: {}", payload.key(), payload.registry());
                return;
            }

            if (!registerManager.containsModRegistry(payload.modId(), registryKey)) {
                LOGGER.warn("Received dynamic registration for unknown registry/mod combination: {} from {}",
                        payload.registry(), payload.modId());
                return;
            }

            FlexibleRegister<?> flexibleRegister = registerManager.getFlexibleRegister(payload.modId(), registryKey);
            if (flexibleRegister == null || !flexibleRegister.supportsDynamicRegistration()) {
                LOGGER.warn("Registry does not support dynamic registration: {}", payload.registry());
                return;
            }

            try {
                Object value = flexibleRegister.tryDecodeDynamicValue(payload.data());
                if (value == null) {
                    LOGGER.error("Failed to decode dynamic value for {}", payload.key());
                    return;
                }

                RegistryUtils.safeRegistryOperation(registry, mappedRegistry -> {
                    RegistryUtils.register(mappedRegistry, payload.key, value);
                    flexibleRegister.setEntriesChanged(true);
                });
                LOGGER.info("Successfully registered dynamic entry: {} in {}", payload.key(), payload.registry());
            } catch (Exception e) {
                LOGGER.error("Error processing dynamic registration for {}: {}",
                        payload.key(), e.getMessage(), e);
            }
        });
    }
}

