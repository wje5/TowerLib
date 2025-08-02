package com.towergames.towerlib.demo;

import com.towergames.towerlib.*;
import org.lwjgl.glfw.GLFW;

public class TowerLibDemo extends TowerGame {
    private static TowerLibDemo game;
    private FontManager.Font testFont;
    private GLHandler.Texture texture;

    @Override
    protected void preInit() {
        super.preInit();
        WindowHandler window = getWindowHandler();
        window.setTitle("Tower Lib Demo");
    }

    @Override
    protected void init() {
        super.init();
        testFont = game.getFontManager().loadFont("fonts/NotoSansCJK-Regular.ttc");
        texture = game.getGlHandler().loadTexture("textures/112923224_p2.jpg", false);
    }

    @Override
    protected void postInit() {
        super.postInit();
        WindowHandler window = getWindowHandler();
        getKeyBindingManager().registerKeyBinding(new KeyBindingManager.KeyBinding("fullscreen", GLFW.GLFW_KEY_K, 0, true) {
            @Override
            protected void onPress() {
                window.setFullscreen(!window.isFullscreen());
            }
        });
    }

    @Override
    protected void doRender() {
        super.doRender();
        GLHandler gl = game.getGlHandler();
        WindowHandler window = game.getWindowHandler();
        gl.getState().viewport(0, 0, window.getWidth(), window.getHeight());
        gl.getState().clearColor(TowerUtil.color(0xFF7CA0A0));
//        float f = (float) (Math.sin(GLFW.glfwGetTime()) * 0.5f + 0.5f);
//        gl.getState().clearColor(new Vector4f(f, f, 0.7f, 1.0f));
        gl.clearColor();
        gl.clearDepth();

        gl.drawRect2D(0, 0, 100, 100, TowerUtil.color(0xFFABCDEF));
//        testFont.getTexture().drawRect2D(100, 100);
        texture.drawRect2D(100,100,100,100,100,100);
        texture.drawRect2D(150,200,150,200,100,100);
        texture.drawRect2D(250,0,250,0,600,600);
        gl.swapBuffer();
        game.getGlHandler().checkError();
    }

    public static void main(String[] args) {
        game = new TowerLibDemo();
        game.run();
    }
}
