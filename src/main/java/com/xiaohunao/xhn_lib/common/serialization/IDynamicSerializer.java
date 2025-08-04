package com.xiaohunao.xhn_lib.common.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 动态内容序列化器接口
 * 用于将动态内容序列化为JSON或从JSON反序列化
 *
 * @param <T> 内容类型
 */
public interface IDynamicSerializer<T> {
    Logger LOGGER = LoggerFactory.getLogger(IDynamicSerializer.class);

    /**
     * 从JSON读取内容
     *
     * @param json JSON元素
     * @return 内容实例，如果解析失败则返回null
     */
    @Nullable
    T read(JsonElement json);

    /**
     * 将内容写入JSON
     *
     * @param content 要序列化的内容
     * @return JSON元素，如果序列化失败则返回null
     */
    @Nullable
    JsonElement write(T content);

    /**
     * 创建一个新的序列化器构建器
     *
     * @param <T> 内容类型
     * @return 构建器实例
     */
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * 从Codec直接创建序列化器
     *
     * @param <T> 内容类型
     * @param codec 用于序列化/反序列化的Codec
     * @return 序列化器实例
     */
    static <T> IDynamicSerializer<T> of(Codec<T> codec) {
        return new Builder<T>().codec(codec).build();
    }

    /**
     * 序列化器构建器
     *
     * @param <T> 内容类型
     */
    class Builder<T> {
        private IDynamicSerializer<T> serializer;

        /**
         * 设置自定义序列化器
         *
         * @param serializer 自定义序列化器实例
         * @return 构建器实例
         */
        public Builder<T> serializer(IDynamicSerializer<T> serializer) {
            this.serializer = Objects.requireNonNull(serializer, "Serializer cannot be null");
            return this;
        }

        /**
         * 通过读写函数设置序列化器
         *
         * @param reader 从JsonObject读取内容的函数
         * @param writer 将内容写入JsonObject的函数
         * @return 构建器实例
         */
        public Builder<T> serializer(
                Function<JsonObject, T> reader,
                Function<T, JsonObject> writer) {
            Objects.requireNonNull(reader, "Reader function cannot be null");
            Objects.requireNonNull(writer, "Writer function cannot be null");

            return serializer(new IDynamicSerializer<>() {
                @Override
                public T read(JsonElement json) {
                    if (json == null || !json.isJsonObject()) {
                        LOGGER.warn("Expected JsonObject but got: {}", json);
                        return null;
                    }
                    try {
                        return reader.apply(json.getAsJsonObject());
                    } catch (Exception e) {
                        LOGGER.error("Error reading JSON: {}", json, e);
                        return null;
                    }
                }

                @Override
                public JsonElement write(T content) {
                    if (content == null) {
                        LOGGER.warn("Attempted to serialize null content");
                        return null;
                    }
                    try {
                        return writer.apply(content);
                    } catch (Exception e) {
                        LOGGER.error("Error writing content: {}", content, e);
                        return null;
                    }
                }
            });
        }

        /**
         * 通过Codec设置序列化器
         *
         * @param codec 用于序列化/反序列化的Codec
         * @return 构建器实例
         */
        public Builder<T> codec(Codec<T> codec) {
            return codec(codec, t -> true);
        }

        /**
         * 通过Codec和序列化条件设置序列化器
         *
         * @param codec 用于序列化/反序列化的Codec
         * @param shouldSerialize 决定是否应该序列化内容的谓词
         * @return 构建器实例
         */
        public Builder<T> codec(Codec<T> codec, Predicate<T> shouldSerialize) {
            Objects.requireNonNull(codec, "Codec cannot be null");
            Objects.requireNonNull(shouldSerialize, "Serialization predicate cannot be null");

            return serializer(new IDynamicSerializer<T>() {
                @Override
                public T read(JsonElement json) {
                    if (json == null) {
                        LOGGER.warn("Attempted to deserialize null JSON");
                        return null;
                    }

                    try {
                        DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
                        if (result.error().isPresent()) {
                            LOGGER.warn("Error parsing JSON: {}", result.error().get().message());
                        }
                        return result.result().orElse(null);
                    } catch (Exception e) {
                        LOGGER.error("Unexpected error during JSON deserialization", e);
                        return null;
                    }
                }

                @Override
                public JsonElement write(T content) {
                    if (content == null) {
                        LOGGER.warn("Attempted to serialize null content");
                        return null;
                    }

                    if (!shouldSerialize.test(content)) {
                        return null;
                    }

                    try {
                        DataResult<JsonElement> result = codec.encodeStart(JsonOps.INSTANCE, content);
                        if (result.error().isPresent()) {
                            LOGGER.warn("Error encoding content: {}", result.error().get().message());
                        }
                        return result.result().orElse(null);
                    } catch (Exception e) {
                        LOGGER.error("Unexpected error during content serialization: {}", content, e);
                        return null;
                    }
                }
            });
        }

        /**
         * 构建序列化器
         *
         * @return 构建的序列化器实例
         * @throws IllegalStateException 如果未设置序列化器
         */
        public IDynamicSerializer<T> build() {
            if (serializer == null) {
                throw new IllegalStateException("Serializer must be set");
            }
            return serializer;
        }
    }
}
