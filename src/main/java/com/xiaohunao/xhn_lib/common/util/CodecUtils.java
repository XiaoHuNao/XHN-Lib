package com.xiaohunao.xhn_lib.common.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class CodecUtils {

    public static <K, V> Codec<Multimap<K, V>> multimapCodec(Codec<K> keyCodec, Codec<V> valueCodec) {
        record KeyValuePair<K, V>(K key, List<V> values) {}
        
        Codec<KeyValuePair<K, V>> pairCodec = RecordCodecBuilder.create(instance -> instance.group(
                keyCodec.fieldOf("key").forGetter(KeyValuePair::key),
                Codec.list(valueCodec).fieldOf("values").forGetter(KeyValuePair::values)
        ).apply(instance, KeyValuePair::new));

        return Codec.list(pairCodec).xmap(
            pairs -> {
                Multimap<K, V> result = HashMultimap.create();
                for (KeyValuePair<K, V> pair : pairs) {
                    for (V value : pair.values()) {
                        result.put(pair.key(), value);
                    }
                }
                return result;
            },
            multimap -> {
                Map<K, List<V>> groupedMap = new HashMap<>();
                for (Map.Entry<K, V> entry : multimap.entries()) {
                    groupedMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                             .add(entry.getValue());
                }
                
                return groupedMap.entrySet().stream()
                    .map(entry -> new KeyValuePair<>(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            }
        );
    }

    public static <K, V> Codec<Map<K, V>> complexKeyMap(Codec<K> keyCodec, Codec<V> valueCodec) {
        return Codec.pair(keyCodec, valueCodec)
                .listOf()
                .xmap(
                        pairList -> pairList.stream().collect(Collectors.toMap(
                                Pair::getFirst,
                                Pair::getSecond,
                                (existingValue, duplicateValue) -> {
                                    return duplicateValue;
                                },
                                LinkedHashMap::new
                        )),
                        originalMap -> originalMap.entrySet().stream()
                                .map(mapEntry -> Pair.of(mapEntry.getKey(), mapEntry.getValue()))
                                .toList()
                );
    }

}
