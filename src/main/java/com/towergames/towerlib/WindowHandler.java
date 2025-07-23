package com.towergames.towerlib;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.swing.*;

public class WindowHandler {
    private final TowerGame game;
    private boolean init, fullscreen;
    private int windowWidthDefault = 800, windowHeightDefault = 600, windowWidth, windowHeight, fullscreenWidth, fullscreenHeight;
    private float mouseX = Float.NaN, mouseY;
    private long window;
    private String title = "Tower Games";

    public WindowHandler(TowerGame game) {
        this.game = game;
    }

    public void createWindow() {
        if (init) {
            return;
        }
        game.getLogger().info("Start to creating window...");
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        if (fullscreen) {
            GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            fullscreenWidth = vidmode.width();
            fullscreenHeight = vidmode.height();
            game.getLogger().info("Creating fullscreen window: {}x{}", fullscreenWidth, fullscreenHeight);
            window = GLFW.glfwCreateWindow(fullscreenWidth, fullscreenHeight, title, GLFW.glfwGetPrimaryMonitor(), 0);
        } else {
            game.getLogger().info("Creating window: {}x{}", windowWidthDefault, windowHeightDefault);
            window = GLFW.glfwCreateWindow(windowWidthDefault, windowHeightDefault, title, 0, 0);
            windowWidth = windowWidthDefault;
            windowHeight = windowHeightDefault;
        }
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            game.getLogger().info("Framebuffer size callback: {}x{}", width, height);
            if (isFullscreen()) {
                fullscreenWidth = width;
                fullscreenHeight = height;
            } else {
                windowWidth = width;
                windowHeight = height;
            }
        });
        GLFW.glfwSetCursorPosCallback(window, (window, posX, posY) -> {
            if (Float.isNaN(mouseX)) {
                mouseX = (float) posX;
                mouseY = (float) posY;
                return;
            }
            float moveX = (float) (posX - mouseX);
            float moveY = (float) (posY - mouseY);
            game.onMouseMove(moveX, moveY);
        });
        GLFW.glfwSetScrollCallback(window, (window, xOffset, yOffset) -> game.onMouseScroll((float) xOffset, (float) yOffset));
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            game.onKeyboardInput(key, scancode, action, mods);
        });
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();
        game.getLogger().info("---- OpenGL Context Info ----");
        game.getLogger().info("Vendor: {}" , GL11.glGetString(GL11.GL_VENDOR));
        game.getLogger().info("Renderer: {}" , GL11.glGetString(GL11.GL_RENDERER));
        game.getLogger().info("Version: {}" , GL11.glGetString(GL11.GL_VERSION));
        game.getLogger().info("-----------------------------");
        init = true;
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public void closeWindow() {
        GLFW.glfwSetWindowShouldClose(window, true);
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        fullscreenWidth = vidmode.width();
        fullscreenHeight = vidmode.height();
        if (fullscreen) {
            game.getLogger().info("Set window to fullscreen...");
            GLFW.glfwSetWindowMonitor(window, GLFW.glfwGetPrimaryMonitor(), 0, 0, fullscreenWidth, fullscreenHeight,
                    GLFW.GLFW_DONT_CARE);
        } else {
            game.getLogger().info("Set fullscreen to window...");
            GLFW.glfwSetWindowMonitor(window, 0, (fullscreenWidth - windowWidthDefault) / 2, (fullscreenHeight - windowHeightDefault) / 2,
                    windowWidthDefault, windowHeightDefault, GLFW.GLFW_DONT_CARE);
        }
        this.fullscreen = fullscreen;
    }

    public int getWidth() {
        return fullscreen ? fullscreenWidth : windowWidth;
    }

    public int getHeight() {
        return fullscreen ? fullscreenHeight : windowHeight;
    }

    public void setWindowSize(int width, int height) {
        windowWidth = width;
        windowHeight = height;
        if (!fullscreen) {
            GLFW.glfwSetWindowMonitor(window, 0, 0, 0, width, height, GLFW.GLFW_DONT_CARE);
        }
    }

    public float getMouseX() {
        return mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getWindow() {
        return window;
    }
}
