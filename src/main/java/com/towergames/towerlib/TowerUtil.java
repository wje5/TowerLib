package com.towergames.towerlib;

import org.joml.Vector4f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TowerUtil {
    public static Vector4f rgba(int hexColor) {
        int alpha = (hexColor >> 24) & 0xFF;
        int red = (hexColor >> 16) & 0xFF;
        int green = (hexColor >> 8) & 0xFF;
        int blue = hexColor & 0xFF;
        float a = alpha / 255.0f;
        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;
        return new Vector4f(r, g, b, a);
    }

    public static String readAll(String path) {
        StringBuilder source = new StringBuilder();
        try (InputStream inputStream = getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String s;
            while ((s = reader.readLine()) != null) {
                source.append(s).append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file " + path + ": ", e);
        }
        return source.toString();
    }

    public static InputStream getResourceAsStream(String path) {
        return TowerUtil.class.getClassLoader().getResourceAsStream(path);
    }
}
