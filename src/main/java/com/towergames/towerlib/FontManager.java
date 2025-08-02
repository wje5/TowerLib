package com.towergames.towerlib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class FontManager {
    private final TowerGame game;
    private final long lib;

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
        private int yPosition;

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
                texture.image(GL11.GL_RED, 2048, 2048, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, null);
                loadChar(' ',48);
                String text = "render Test渲染测试";
                for(char c:text.toCharArray()){
                    game.getLogger().debug(c+"");
                    loadChar(c,48);
                }
            }
        }

        private void loadChar(char c, int size) {
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
            if(width != 0 || height !=0){
                texture.subImage(xOffset, yOffset, width, height, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, bitmap.buffer(width * height));
            }
            xOffsets.put(height,xOffset + width);
            if (!map.containsKey(size)) {
                map.put(size, new HashMap<>());
            }
            Map<Character, Char> m = map.get(size);
            m.put(c,new Char(xOffset,yOffset,width,height));
        }

        public GLHandler.Texture getTexture() {
            return texture;
        }

        public class Char {
            private final int u, v, width, height;

            private Char(int u, int v, int width, int height) {
                this.u = u;
                this.v = v;
                this.width = width;
                this.height = height;
            }
        }
    }
}
