package com.xiaohunao.xhn_lib.common.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.xiaohunao.xhn_lib.common.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public interface XHNStreamCodecs {
    StreamCodec<ByteBuf, JsonElement> JSON_ELEMENT = new StreamCodec<>() {
        @Override
        public JsonElement decode(ByteBuf buffer) {
            var str = Utf8String.read(buffer, Integer.MAX_VALUE);
            return str.isEmpty() || str.equals("null") ? JsonNull.INSTANCE : JsonUtils.fromString(str);
        }

        @Override
        public void encode(ByteBuf buffer, @Nullable JsonElement value) {
            if (value == null || value.isJsonNull()) {
                Utf8String.write(buffer, "", Integer.MAX_VALUE);
            } else {
                Utf8String.write(buffer, JsonUtils.toString(value), Integer.MAX_VALUE);
            }
        }
    };
}
