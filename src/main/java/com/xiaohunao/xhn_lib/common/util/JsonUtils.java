package com.xiaohunao.xhn_lib.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.jetbrains.annotations.Nullable;

public class JsonUtils {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setLenient().serializeNulls().create();

    public static JsonElement fromString(@Nullable String string) {
        if (string == null || string.isEmpty() || string.equals("null")) {
            return JsonNull.INSTANCE;
        }

        try {
            return GSON.fromJson(string, JsonElement.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return JsonNull.INSTANCE;
    }

    public static String toString(JsonElement json) {
        return GSON.toJson(json);
    }
}
