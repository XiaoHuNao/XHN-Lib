package com.xiaohunao.xhn_lib.common.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DynamicSerializerType<T> {
    private final IDynamicSerializer<T> serializer;

    
    private DynamicSerializerType(Builder<T> builder) {
        this.serializer = builder.serializer;
    }

    public static <T> DynamicSerializerType<T> of(Codec<T> codec){
        return builder(codec).build();
    }

    public IDynamicSerializer<T> getSerializer() {
        return serializer;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> Builder<T> builder(Codec<T> codec) {
        return new Builder<T>().codec(codec);
    }

    public static class Builder<T> {
        private IDynamicSerializer<T> serializer;


        public Builder<T> serializer(IDynamicSerializer<T> serializer) {
            this.serializer = serializer;
            return this;
        }
        

        public Builder<T> serializer(
                Function<JsonObject, T> reader,
                Function<T, JsonObject> writer) {
            return serializer(new IDynamicSerializer<>() {
                @Override
                public T read(ResourceLocation id, JsonElement json) {
                    if (json.isJsonObject()) {
                        return reader.apply(json.getAsJsonObject());
                    }
                    return null;
                }

                @Override
                public JsonElement write(T content) {
                    return writer.apply(content);
                }
            });
        }
        

        public Builder<T> codec(Codec<T> codec) {
            return codec(codec, t -> true);
        }

        public Builder<T> codec(Codec<T> codec, Predicate<T> shouldSerialize) {
            Objects.requireNonNull(codec);
            Objects.requireNonNull(shouldSerialize);
            
            return serializer(new IDynamicSerializer<T>() {
                @Override
                public T read(ResourceLocation id, JsonElement json) {
                    DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
                    return result.result().orElse(null);
                }
                
                @Override
                public JsonElement write(T content) {
                    if (!shouldSerialize.test(content)) {
                        return null;
                    }
                    DataResult<JsonElement> result = codec.encodeStart(JsonOps.INSTANCE, content);
                    return result.result().orElse(null);
                }
            });
        }
        

        public DynamicSerializerType<T> build() {
            if (serializer == null) {
                throw new IllegalStateException("必须设置序列化器");
            }
            return new DynamicSerializerType<>(this);
        }
    }
}
