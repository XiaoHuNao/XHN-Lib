package com.xiaohunao.xhn_lib.common.util;

import org.joml.Vector3f;
import org.joml.Vector4f;



public class ColorUtils {
    /**
     * 将整数形式的颜色转换为包含RGB分量的Vector3f对象。
     * @param color 整数形式的颜色。
     * @return 包含RGB分量的Vector3f对象。
     */
    public static Vector3f colorToVector3f(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return new Vector3f(r, g, b);
    }

    /**
     * 将包含RGB分量的Vector3f对象转换为整数形式的颜色。
     * @param vector3f 包含RGB分量的Vector3f对象。
     * @return 整数形式的颜色。
     */
    public static int vector3fToColor(Vector3f vector3f) {
        int r = (int) (vector3f.x() * 255.0f);
        int g = (int) (vector3f.y() * 255.0f);
        int b = (int) (vector3f.z() * 255.0f);
        return r << 16 | g << 8 | b;
    }
    /**
     * 将整数形式的颜色转换为包含RGBA分量的Vector4f对象。
     * @param color 整数形式的颜色。
     * @return 包含RGBA分量的Vector4f对象。
     */
    public static Vector4f colorToVector4f(int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return new Vector4f(r, g, b, a);
    }

    /**
     * 将包含RGBA分量的Vector4f对象转换为整数形式的颜色。
     * @param vector4f 包含RGBA分量的Vector4f对象。
     * @return 整数形式的颜色。
     */
    public static int vector4fToColor(Vector4f vector4f) {
        int a = (int) (vector4f.w() * 255.0f);
        int r = (int) (vector4f.x() * 255.0f);
        int g = (int) (vector4f.y() * 255.0f);
        int b = (int) (vector4f.z() * 255.0f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 调整颜色的亮度。
     * @param color 原始颜色。
     * @param factor 亮度调整因子。
     * @return 调整亮度后的颜色。
     */
    public static int adjustBrightness(int color, float factor) {
        int a = color & 0xFF000000;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return a | (r << 16) | (g << 8) | b;
    }

    /**
     * 对两种颜色进行线性插值。
     * @param color1 第一种颜色。
     * @param color2 第二种颜色。
     * @param t 插值参数，范围从0到1。
     * @return 插值后的颜色。
     */
    public static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + t * (a2 - a1));
        int r = (int) (r1 + t * (r2 - r1));
        int g = (int) (g1 + t * (g2 - g1));
        int b = (int) (b1 + t * (b2 - b1));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

}