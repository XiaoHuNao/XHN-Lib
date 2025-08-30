package com.xiaohunao.xhn_lib.common.event.subscriber;

import com.xiaohunao.xhn_lib.XHN_Lib;
import com.xiaohunao.xhn_lib.api.register.holder.FlexibleHolder;
import com.xiaohunao.xhn_lib.api.register.register.FlexibleRegister;
import com.xiaohunao.xhn_lib.api.register.FlexibleRegisterManager;
import com.xiaohunao.xhn_lib.common.network.s2c.DynamicLoaderSyncPayload;
import com.xiaohunao.xhn_lib.common.serialization.IDynamicSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Map;

@EventBusSubscriber(modid = XHN_Lib.MODID)
public class PlayerEventSubscriber {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        FlexibleRegisterManager registerManager = FlexibleRegisterManager.INSTANCE;

        for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
            ResourceKey<? extends Registry<?>> registryKey = registry.key();

            Collection<Map.Entry<String, FlexibleRegister<?>>> registers =
                    registerManager.getFlexibleRegisters(registryKey);


            for (Map.Entry<String, FlexibleRegister<?>> entry : registers) {
                String modId = entry.getKey();
                FlexibleRegister<?> register = entry.getValue();


                if (register.supportsDynamicRegistration()) {
                    IDynamicSerializer serializer = register.getDynamicManager().getSerializer();

                    // 发送该注册表的所有条目
                    for (FlexibleHolder<?, ?> holder : register.getAllEntries()) {
                        DynamicLoaderSyncPayload payload = new DynamicLoaderSyncPayload(
                                modId,
                                registryKey.location(),
                                holder.getKey().location(),
                                serializer.write(holder.get())
                        );

                        PacketDistributor.sendToPlayer(player, payload);
                    }
                }
            }
        }
    }

}