package com.towergames.towerlib;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private final TowerGame game;
    private final long lib;
    private final GLHandler.Program programText;
    private final GLHandler.VAO vaoText;

    public FontManager(TowerGame game) {
        this.game = game;
        game.getLogger().info("Initializing FontManager...");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.mallocPointer(1);
            int error = FreeType.FT_Init_FreeType(pointer);
            if (error != 0) {
                throw new RuntimeException("Failed to initialize Freetype, error " + error);
            }
            lib = pointer.get();
        }
        GLHandler gl = game.getGlHandler();
        programText = gl.createProgram("shaders/xyuv.vs", "shaders/alpha.fs");
        vaoText = gl.createVAO().vertexAttrib(0, 4, 0, 0).bindEBO(gl.ebo10000Rects, 60000, GL11.GL_UNSIGNED_INT);
    }

    public Font loadFont(String path) {
        return new Font(path);
    }

    public class Font {
        private final FT_Face face;
        private final String path;
        private GLHandler.Texture texture;
        private Map<Integer, Map<Character, Char>> map = new HashMap<>();
        private Map<Integer, Integer> xOffsets = new HashMap<>();
        private Map<Integer, Integer> yOffsets = new HashMap<>();
        private int yPosition, textureWidth = 2048, textureHeight = 2048;

        private Font(String path) {
            this.path = path;
            String tempFilePath = TowerUtil.getResourceAsTempFile(path);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer pointer = stack.mallocPointer(1);
                int error = FreeType.FT_New_Face(lib, tempFilePath, 0, pointer);
                if (error != 0) {
                    throw new RuntimeException("Failed to load font: " + path + ", error " + error);
                }
                face = FT_Face.create(pointer.get());
                texture = game.getGlHandler().createTexture(false);
                texture.image(GL11.GL_RED, textureWidth, textureHeight, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, null);
            }
        }

        private Char getChar(char c, int size) {
            if (!map.containsKey(size)) {
                map.put(size, new HashMap<>());
            }
            Map<Character, Char> m = map.get(size);
            if (m.containsKey(c)) {
                return m.get(c);
            }
            int error = FreeType.FT_Set_Pixel_Sizes(face, 0, size);
            if (error != 0) {
                throw new RuntimeException("Error in set font size: font " + path + ", char " + c + ", error " + error);
            }
            error = FreeType.FT_Load_Char(face, c, FreeType.FT_LOAD_RENDER);
            if (error != 0) {
                throw new RuntimeException("Error in load char: font " + path + ", char " + c + ", error " + error);
            }
            GLHandler gl = game.getGlHandler();
            FT_GlyphSlot glyph = face.glyph();
            FT_Bitmap bitmap = glyph.bitmap();
            int width = bitmap.width();
            int height = bitmap.rows();
            if (!xOffsets.containsKey(height)) {
                xOffsets.put(height, 0);
            }
            int xOffset = xOffsets.get(height);
            if (!yOffsets.containsKey(height)) {
                yOffsets.put(height, yPosition);
                yPosition += height;
            }
            int yOffset = yOffsets.get(height);
            gl.getState().unpackAlignment(1);
            if (width != 0 || height != 0) {
                texture.subImage(xOffset, yOffset, width, height, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, bitmap.buffer(width * height));
            }
            xOffsets.put(height, xOffset + width);
            Char ch = new Char(glyph.bitmap_left(), glyph.bitmap_top(), xOffset, yOffset, width, height, (int) glyph.advance().x());
            m.put(c, ch);
//            game.getLogger().debug("char {} u{} v{} bearing{}/{} size{}/{}", c, ch.u, ch.v, ch.bearingX, ch.bearingY, ch.width, ch.height);
            return ch;

        }

        public float renderText(float x, float y, int size, float lineSpacing, float maxWidth, Vector4f color, String... texts) {
            GLHandler gl = game.getGlHandler();
            WindowHandler window = game.getWindowHandler();
            int charCount = Arrays.stream(texts).mapToInt(String::length).sum();
            float[] data = new float[charCount * 16];
            float xOffset = 0;
            float yOffset = size;
            int i = 0;
            float textBlockWidth = 0;
            for (String text : texts) {
                char[] chars = text.toCharArray();
                for (char ch : chars) {
                    Char c = getChar(ch, size);
                    if (maxWidth > 0 && xOffset + c.bearingX + c.width > maxWidth) {
                        yOffset += lineSpacing;
                        xOffset = 0;
                    }
                    if (xOffset + c.bearingX + c.width > textBlockWidth) {
                        textBlockWidth = xOffset + c.bearingX + c.width;
                    }
                    data[i * 16] = xOffset + c.bearingX;
                    data[i * 16 + 1] = yOffset - c.bearingY;
                    data[i * 16 + 2] = c.u * 1.0f / textureWidth;
                    data[i * 16 + 3] = c.v * 1.0f / textureHeight;

                    data[i * 16 + 4] = xOffset + c.bearingX;
                    data[i * 16 + 5] = yOffset - c.bearingY + c.height;
                    data[i * 16 + 6] = c.u * 1.0f / textureWidth;
                    data[i * 16 + 7] = (c.v + c.height) * 1.0f / textureHeight;

                    data[i * 16 + 8] = xOffset + c.bearingX + c.width;
                    data[i * 16 + 9] = yOffset - c.bearingY + c.height;
                    data[i * 16 + 10] = (c.u + c.width) * 1.0f / textureWidth;
                    data[i * 16 + 11] = (c.v + c.height) * 1.0f / textureHeight;

                    data[i * 16 + 12] = xOffset + c.bearingX + c.width;
                    data[i * 16 + 13] = yOffset - c.bearingY;
                    data[i * 16 + 14] = (c.u + c.width) * 1.0f / textureWidth;
                    data[i * 16 + 15] = c.v * 1.0f / textureHeight;
                    xOffset += c.advance * 0.015625f;  // 1/64
                    i++;
                }
                yOffset += lineSpacing;
                xOffset = 0;
            }
            vaoText.vboData(data, GL15.GL_DYNAMIC_DRAW);
            gl.getState().texture0(texture);
            programText.uniform("uColor", color).uniform("uTexture", 0);
            gl.getState().depthTest(false);
            gl.getState().pushMVP();
                    gl.getState().model(new Matrix4f().translate(x, y, 0.0f)).view(new Matrix4f())
                    .projection(new Matrix4f().ortho(0.0f, window.getWidth(), window.getHeight(), 0.0f, 0.0f, 1.0f)).applyMVP();
            vaoText.drawElements(GL11.GL_TRIANGLES, charCount * 6);
            gl.getState().popMVP();
            return textBlockWidth;
        }

        public float renderText(float x, float y, int size, float maxWidth, Vector4f color, String... texts) {
            return renderText(x, y, size, size, maxWidth, color, texts);
        }

        public class Char {
            private final int bearingX, bearingY, u, v, width, height, advance;

            private Char(int bearingX, int bearingY, int u, int v, int width, int height, int advance) {
                this.bearingX = bearingX;
                this.bearingY = bearingY;
                this.u = u;
                this.v = v;
                this.width = width;
                this.height = height;
                this.advance = advance;
            }
        }
    }
}
