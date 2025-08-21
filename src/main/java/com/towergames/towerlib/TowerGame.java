package com.towergames.towerlib;


import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TowerGame {
    private Logger logger = LoggerFactory.getLogger(TowerGame.class);
    private WindowHandler windowHandler;
    private GLHandler glHandler;
    private FontManager fontManager;
    private ModelManager modelManager;
    private KeyBindingManager keyBindingManager;
    private int fps;
    private final long startTime = System.nanoTime();
    private Timer updateFPS = createTimer().setTimer(1);

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
        fontManager = new FontManager(this);
        modelManager = new ModelManager(this);
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
        long lastRender = System.nanoTime();
        while (!windowHandler.shouldClose()) {
            long startRender = System.nanoTime();
            if (updateFPS.isTimesUp()) {
                fps = (int) (1000000000 / (startRender - lastRender));
                updateFPS.setTimer(0.5f);
            }
            lastRender = startRender;
            GLFW.glfwPollEvents();
            doRender();
        }
    }

    public void exit() {
        windowHandler.closeWindow();
    }

    public long getNanosFromStart() {
        return System.nanoTime() - startTime;
    }

    public float getSecFromStart() {
        return getNanosFromStart() * 0.000000001f;
    }

    public int getFPS() {
        return fps;
    }

    public Timer createTimer() {
        return new Timer();
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

    public FontManager getFontManager() {
        return fontManager;
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public KeyBindingManager getKeyBindingManager() {
        return keyBindingManager;
    }

    public class Timer {
        private long time;

        private Timer() {

        }

        public Timer setTimeStamp(long timeStamp) {
            this.time = timeStamp;
            return this;
        }

        public Timer setTimer(float sec) {
            setTimeStamp(getNanosFromStart() + (long) (sec * 1000000000L));
            return this;
        }

        public long get() {
            return time - getNanosFromStart();
        }

        public boolean isTimesUp() {
            return get() <= 0;
        }
    }
}
