package com.towergames.towerlib;


import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TowerGame {
    private Logger logger = LoggerFactory.getLogger(TowerGame.class);
    private WindowHandler windowHandler;
    private GLHandler glHandler;
    private KeyBindingManager keyBindingManager;

    protected void run() {
        preInit();
        init();
        postInit();
        loop();
    }

    protected void preInit() {
        windowHandler = new WindowHandler(this);
    }

    protected void init() {
        windowHandler.createWindow();
        glHandler = new GLHandler(this);
    }

    protected void postInit() {
        keyBindingManager = new KeyBindingManager(this);
    }

    protected void doRender() {

    }

    protected void onMouseMove(float moveX, float moveY) {

    }

    protected void onMouseScroll(float moveX, float moveY) {

    }

    protected void onKeyboardInput(int key, int scancode, int action, int mods) {
        keyBindingManager.onKeyBoardInput(key, scancode, action, mods);
    }

    private void loop() {
        while (!windowHandler.shouldClose()) {
            GLFW.glfwPollEvents();
            doRender();
        }
    }

    public void exit() {
        windowHandler.closeWindow();
    }

    public Logger getLogger() {
        return logger;
    }

    public WindowHandler getWindowHandler() {
        return windowHandler;
    }

    public GLHandler getGlHandler() {
        return glHandler;
    }

    public KeyBindingManager getKeyBindingManager() {
        return keyBindingManager;
    }
}
