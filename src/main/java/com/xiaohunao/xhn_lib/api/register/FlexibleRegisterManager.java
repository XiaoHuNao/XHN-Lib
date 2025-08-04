package com.xiaohunao.xhn_lib.api.register;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.xiaohunao.xhn_lib.api.data.loader.BaseDynamicLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.spongepowered.include.com.google.common.collect.HashMultimap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FlexibleRegisterManager {
    public static final FlexibleRegisterManager INSTANCE = new FlexibleRegisterManager();

    private final Map<ResourceKey<? extends Registry<?>>, Map<String, FlexibleRegister<?>>> registryMap = new ConcurrentHashMap<>();
    private final HashMultimap<BaseDynamicLoader<?>,FlexibleRegister<?>> dynamicLoaders = HashMultimap.create();


    private FlexibleRegisterManager() {
    }

    /**
     * 获取指定注册表的所有FlexibleRegister
     */
    public Collection<Map.Entry<String, FlexibleRegister<?>>> getFlexibleRegisters(ResourceKey<? extends Registry<?>> resourceKey) {
        Map<String, FlexibleRegister<?>> modRegisters = registryMap.get(resourceKey);
        return modRegisters != null ? modRegisters.entrySet() : Collections.emptyList();
    }

    /**
     * 添加一个FlexibleRegister到管理器
     */
    public void addRegister(FlexibleRegister<?> register) {
        ResourceKey<? extends Registry<?>> registryKey = register.getRegistryKey();
        String modId = register.getModId();

        registryMap.computeIfAbsent(registryKey, resourceKey -> new HashMap<>())
                .put(modId, register);

        if (register.supportsDynamicRegistration()) {
            BaseDynamicLoader<?> loader = register.getDynamicManager();
            if (loader != null) {
                dynamicLoaders.put(loader, register);
            }
        }
    }

    /**
     * 获取特定模组和注册表的FlexibleRegister
     */
    @SuppressWarnings("unchecked")
    public <T> FlexibleRegister<T> getFlexibleRegister(String modId, ResourceKey<? extends Registry<?>> registryKey) {
        Map<String, FlexibleRegister<?>> modRegisters = registryMap.get(registryKey);
        if (modRegisters == null) {
            return null;
        }
        return (FlexibleRegister<T>) modRegisters.get(modId);
    }

    /**
     * 检查是否包含指定注册表
     */
    public boolean containsRegistry(ResourceKey<? extends Registry<?>> registryKey) {
        return registryMap.containsKey(registryKey);
    }


    public Collection<FlexibleRegister<?>> getFlexibleRegister(BaseDynamicLoader<?> loader) {
        return dynamicLoaders.get(loader);
    }

    /**
     * 检查是否包含指定模组的指定注册表
     */
    public boolean containsModRegistry(String modId, ResourceKey<? extends Registry<?>> registryKey) {
        Map<String, FlexibleRegister<?>> modRegisters = registryMap.get(registryKey);
        return modRegisters != null && modRegisters.containsKey(modId);
    }

    public void registerAllDynamicLoaders() {
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            for (Map<String, FlexibleRegister<?>> modRegisters : registryMap.values()) {
                for (FlexibleRegister<?> register : modRegisters.values()) {
                    if (register.supportsDynamicRegistration()) {
                        BaseDynamicLoader<?> loader = register.getDynamicManager();
                        if (loader != null) {
                            event.addListener(loader);
                        }
                    }
                }
            }
        });
    }
}
