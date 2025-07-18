package com.towergames.towerlib;

import org.joml.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Stack;

public class GLHandler {
    private final TowerGame game;
    private Stack<GLState> stack;

    public GLHandler(TowerGame game) {
        this.game = game;
        stack = new Stack<>();
        stack.push(new GLState());
    }

    public GLState getState() {
        return stack.peek();
    }

    public void pushStack() {
        stack.push(new GLState(stack.peek()));
    }

    public void popStack() {
        stack.pop().applyState(stack.peek());
    }

    public void clearColor() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    public void clearDepth() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void swapBuffer() {
        GLFW.glfwSwapBuffers(game.getWindowHandler().getWindow());
    }

    public void checkError() {
        int error = 0;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            String s = "";
            switch (error) {
                case GL11.GL_INVALID_ENUM:
                    s = "INVALID_ENUM";
                    break;
                case GL11.GL_INVALID_VALUE:
                    s = "GL_INVALID_VALUE";
                    break;
                case GL11.GL_INVALID_OPERATION:
                    s = "GL_INVALID_OPERATION";
                    break;
                case GL11.GL_STACK_OVERFLOW:
                    s = "GL_STACK_OVERFLOW";
                    break;
                case GL11.GL_STACK_UNDERFLOW:
                    s = "GL_STACK_UNDERFLOW";
                    break;
                case GL11.GL_OUT_OF_MEMORY:
                    s = "GL_OUT_OF_MEMORY";
                    break;
                case GL30.GL_INVALID_FRAMEBUFFER_OPERATION:
                    s = "GL_INVALID_FRAMEBUFFER_OPERATION";
                    break;
            }
            game.getLogger().error("GLError: {}", s);
        }
    }

    public static class GLState {
        private boolean cullFront, cullFace, depthTest;
        private int depthFunc,program;
        private Vector4f clearColor = new Vector4f();
        private Vector4i viewport = new Vector4i();

        public GLState() {

        }

        public GLState(GLState state) {
            cullFront = state.cullFront;
            cullFace = state.cullFace;
            depthTest = state.depthTest;
            depthFunc = state.depthFunc;
            clearColor = state.clearColor;
            viewport = state.viewport;
        }

        public void applyState(GLState state) {
            cullFront(state.cullFront);
            cullFace(state.cullFace);
            depthTest(state.depthTest);
            depthFunc(state.depthFunc);
            clearColor(state.clearColor);
            viewport(state.viewport);
        }

        public GLState cullFront(boolean cullFront) {
            if (this.cullFront != cullFront) {
                GL11.glCullFace(cullFront ? GL11.GL_FRONT : GL11.GL_BACK);
            }
            this.cullFront = cullFront;
            return this;
        }

        public GLState cullFace(boolean enable) {
            if (this.cullFace != enable) {
                if (enable) {
                    GL11.glEnable(GL11.GL_CULL_FACE);
                } else {
                    GL11.glDisable(GL11.GL_CULL_FACE);
                }
            }
            this.cullFace = enable;
            return this;
        }

        public GLState depthTest(boolean enable) {
            if (this.depthTest != enable) {
                if (enable) {
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                } else {
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                }
            }
            this.depthTest = enable;
            return this;
        }

        public GLState depthFunc(int depthFunc) {
            if (this.depthFunc != depthFunc) {
                GL11.glDepthFunc(depthFunc);
            }
            this.depthFunc = depthFunc;
            return this;
        }

        public GLState clearColor(Vector4f clearColor) {
            if (!this.clearColor.equals(clearColor)) {
                GL11.glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
            }
            this.clearColor = clearColor;
            return this;
        }

        public GLState viewport(int x, int y, int width, int height) {
            return viewport(new Vector4i(x, y, width, height));
        }

        public GLState viewport(Vector4i v) {
            if (!this.viewport.equals(v)) {
                GL11.glViewport(v.x, v.y, v.z, v.w);
            }
            this.viewport = v;
            return this;
        }

        public GLState program(int program){
            if(this.program != program){
                GL20.glUseProgram(program);
            }
            this.program = program;
            return this;
        }
    }
}
