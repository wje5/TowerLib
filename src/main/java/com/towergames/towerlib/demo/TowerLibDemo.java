package com.towergames.towerlib.demo;

import com.towergames.towerlib.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class TowerLibDemo extends TowerGame {
    private static TowerLibDemo game;
    private FontManager.Font testFont;
    private GLHandler.Texture texture;
    private boolean scene3D;

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
        texture = game.getGlHandler().loadTexture("textures/uv.png", false, false);
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
        getKeyBindingManager().registerKeyBinding(new KeyBindingManager.KeyBinding("exit", GLFW.GLFW_KEY_ESCAPE, 0, false) {
            @Override
            protected void onPress() {
                window.closeWindow();
            }
        });
        getKeyBindingManager().registerKeyBinding(new KeyBindingManager.KeyBinding("switchScene", GLFW.GLFW_KEY_M, 0, true) {
            @Override
            protected void onPress() {
                scene3D = !scene3D;
            }
        });
    }

    @Override
    protected void doRender() {
        super.doRender();
        GLHandler gl = getGlHandler();
        WindowHandler window = getWindowHandler();
        gl.getState().viewport(0, 0, window.getWidth(), window.getHeight());
        gl.getState().clearColor(TowerUtil.color(0xFF7CA0A0));
//        float f = (float) (Math.sin(GLFW.glfwGetTime()) * 0.5f + 0.5f);
//        gl.getState().clearColor(new Vector4f(f, f, 0.7f, 1.0f));
        gl.clearColor();
        gl.clearDepth();
        gl.getState().cullFace(false).depthTest(true).blend(true).blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (scene3D) {
            Vector3f cameraPos = new Vector3f(0.0f, 10.0f, 10.0f);
            float cameraPitch = -45f;
            float cameraYaw = 0.0f;
            float scale = 0.01f;
            gl.getState().model(new Matrix4f())
                    .view(new Matrix4f().lookAt(cameraPos, new Vector3f(cameraPos).add(TowerUtil.getDirection(cameraPitch, cameraYaw)), new Vector3f(0.0f, 1.0f, 0.0f)))
                    .projection(new Matrix4f().ortho(window.getWidth() * -0.5f * scale, window.getWidth() * 0.5f * scale,
                            window.getHeight() * -0.5f * scale, window.getHeight() * 0.5f * scale, 0.1f, 10000f));
            gl.getState().depthTest(true).texture0(texture);
            gl.basic.uniform("uColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f)).uniform("uTexture", 0);

            gl.getState().pushMVP();
            gl.getState().model.rotateX((float) Math.toRadians(90f)).scale(8f).translate(-0.5f, -0.5f, 0.0f);
            gl.getState().applyMVP();
            gl.vaoRect.drawElements();
            gl.getState().popMVP();

            testFont.renderText(0, 100, 48, 0, TowerUtil.color(0xFFFFFFFF), "FPS: " + game.getFPS(), "K to switch fullscreen", "Esc to quit", "M to 2D scene");
            gl.getState().depthTest(false).cullFace(false);
            gl.getState().pushMVP();
            gl.getState().model.scale(1f);
            gl.getState().applyMVP();
            Map<String, Float> animations = new HashMap<>();
            animations.put("Act1", game.getSecFromStart() % 3f);
            getModelManager().loadModel("models/act_test_3.glb").doRender(false, animations);
//            getModelManager().loadModel("models/maiden_test_3.glb").doRender(false);
            gl.getState().popMVP();
        } else {
            gl.drawRect2D(0, 0, 100, 100, TowerUtil.color(0xFFABCDEF));
            float width = testFont.renderText(0, 100, 48, 0, TowerUtil.color(0xFFFFFFFF), "K to switch fullscreen", "Esc to quit", "M to 3D scene");
            texture.drawRect2D(width, 0);
        }
        gl.swapBuffer();
        gl.checkError();
    }

    public static void main(String[] args) {
        game = new TowerLibDemo();
        game.run();
    }
}
