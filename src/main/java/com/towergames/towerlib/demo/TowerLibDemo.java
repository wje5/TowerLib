package com.towergames.towerlib.demo;

import com.towergames.towerlib.*;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

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
        gl.getState().clearColor(TowerUtil.rgba(0xFF7CA0A0));
//        float f = (float) (Math.sin(GLFW.glfwGetTime()) * 0.5f + 0.5f);
//        gl.getState().clearColor(new Vector4f(f, f, 0.7f, 1.0f));
        gl.clearColor();
        gl.clearDepth();


        gl.pos.use();
        gl.swapBuffer();
        game.getGlHandler().checkError();
    }

    public static void main(String[] args) {
        game = new TowerLibDemo();
        game.run();
    }
}
