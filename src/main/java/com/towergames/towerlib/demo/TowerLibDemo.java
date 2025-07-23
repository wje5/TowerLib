package com.towergames.towerlib.demo;

import com.towergames.towerlib.*;
import org.lwjgl.glfw.GLFW;

public class TowerLibDemo extends TowerGame {
    private static TowerLibDemo game;

    @Override
    protected void preInit() {
        super.preInit();
        WindowHandler window = getWindowHandler();
        window.setTitle("Tower Lib Demo");
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

        gl.drawRect2D(0,0,100,100,TowerUtil.color(0xFFABCDEF));

        gl.swapBuffer();
        game.getGlHandler().checkError();
    }

    public static void main(String[] args) {
        game = new TowerLibDemo();
        game.run();
    }
}
