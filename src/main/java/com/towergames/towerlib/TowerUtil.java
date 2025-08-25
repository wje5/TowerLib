package com.towergames.towerlib;

import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TowerUtil {
    public static Vector4f color(int hexColor) {
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

    public static Vector3f getDirection(float pitch, float yaw) {
        return new Vector3f(org.joml.Math.sin(org.joml.Math.toRadians(yaw)) * org.joml.Math.cos(org.joml.Math.toRadians(-pitch)),
                org.joml.Math.sin(org.joml.Math.toRadians(pitch)),
                -org.joml.Math.cos(org.joml.Math.toRadians(yaw)) * org.joml.Math.cos(Math.toRadians(-pitch)));
    }

    public static String readToString(String path) {
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

    public static byte[] readToBytes(String path) {
        try (InputStream stream = getResourceAsStream(path)) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int n;
            while ((n = stream.read(data, 0, data.length)) > -1) {
                s.write(data, 0, n);
            }
            s.flush();
            return s.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file " + path + ": ", e);
        }
    }

    public static ByteBuffer readToBuffer(String path) {
        return toDirectBuffer(readToBytes(path));
    }

    public static InputStream getResourceAsStream(String path) {
        return TowerUtil.class.getClassLoader().getResourceAsStream(path);
    }

    public static URL getResourceURL(String path) {
        return TowerUtil.class.getClassLoader().getResource(path);
    }

    public static URI getResourceURI(String path) {
        try {
            return getResourceURL(path).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getResourcePath(String path) {
        URI uri = getResourceURI(path);
        String[] array = uri.toString().split("!");
        FileSystem fs;
        if ("jar".equals(uri.getScheme())) {
            try {
                fs = FileSystems.getFileSystem(URI.create(array[0]));
            } catch (FileSystemNotFoundException e) {
                try {
                    Map<String, String> env = new HashMap<>();
                    env.put("create", "true");
                    fs = FileSystems.newFileSystem(URI.create(array[0]), env);
                } catch (IOException e1) {
                    throw new RuntimeException(e);
                }
            }
            return fs.getPath(array[1]);
        } else {
            return Paths.get(uri);
        }
    }

    public static String getResourceAbsolutePath(String path) {
        URL url = getResourceURL(path);
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            List<String> l = Arrays.asList(url.getPath().split("/"));
            return String.join("/", l.subList(1, l.size()));
        }
        return url.getPath();
    }

    public static String getResourceAsTempFile(String path) {
        InputStream stream = getResourceAsStream(path);
        String[] a = path.split("\\.");
        String suffix = a.length > 1 ? "." + a[a.length - 1] : "";
        try {
            Path tempFile = Files.createTempFile("temp-", suffix);
            tempFile.toFile().deleteOnExit();
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer toDirectBuffer(byte[] data) {
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        ((Buffer) direct).flip();
        return direct;
    }

    public static ByteBuffer toDirectBuffer(ByteBuffer data) {
        ByteBuffer direct = ByteBuffer.allocateDirect(data.capacity());
        data.rewind();
        direct.put(data);
        ((Buffer) direct).flip();
        return direct;
    }

    public static int[] readUnsignedShortsToIntArray(ByteBuffer buffer) {
        short[] shorts = new short[buffer.remaining() / 2];
        buffer.asShortBuffer().get(shorts);
        int[] r = new int[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            r[i] = shorts[i] & 0xFFFF;
        }
        return r;
    }
}
