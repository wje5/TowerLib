package com.towergames.towerlib;

import org.joml.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.util.*;

public class GLHandler {
    private final TowerGame game;
    private Map<String, Integer> shaders = new HashMap<>();
    private Stack<GLState> stack;
    public final Program pos;

    public GLHandler(TowerGame game) {
        this.game = game;
        stack = new Stack<>();
        stack.push(new GLState());
        pos = createProgram("shaders/pos.vs", "shaders/uColor.fs");
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

    public Program createProgram(String... shaderPaths) {
        return new Program(shaderPaths);
    }

    public VAO createVAO() {
        return new VAO();
    }

    public class Program {
        private int id;
        private Map<String, Integer> uniformLocations = new HashMap<>();
        private Map<String, Object> uniforms = new HashMap<>();

        private Program(String... shaderPaths) {
            int[] shaderIDs = new int[shaderPaths.length];
            for (int i = 0; i < shaderPaths.length; i++) {
                String path = shaderPaths[i];
                if (shaders.containsKey(path)) {
                    shaderIDs[i] = shaders.get(path);
                    continue;
                }
                int type = 0;
                if (path.endsWith(".vs")) {
                    type = GL20.GL_VERTEX_SHADER;
                } else if (path.endsWith(".fs")) {
                    type = GL20.GL_FRAGMENT_SHADER;
                } else if (path.endsWith(".gs")) {
                    type = GL32.GL_GEOMETRY_SHADER;
                } else {
                    throw new RuntimeException("Unrecognized shader suffix: " + path);
                }
                shaderIDs[i] = GL20.glCreateShader(type);
                GL20.glShaderSource(shaderIDs[i], TowerUtil.readAll(path));
                GL20.glCompileShader(shaderIDs[i]);
                if (GL20.glGetShaderi(shaderIDs[i], GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                    throw new RuntimeException("Failed to compile shader: " + path + "\n" + GL20.glGetShaderInfoLog(shaderIDs[i]));
                }
                shaders.put(path, shaderIDs[i]);
            }
            id = GL20.glCreateProgram();
            for (int shader : shaderIDs) {
                GL20.glAttachShader(id, shader);
            }
            GL20.glLinkProgram(id);
            if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("Failed to link program: " + Arrays.toString(shaderPaths) + "\n" + GL20.glGetProgramInfoLog(id));
            }
            game.getLogger().debug("Program linked: {}", Arrays.toString(shaderPaths));
        }

        public int getUniformLocation(String name) {
            Integer location = uniformLocations.get(name);
            if (location != null) {
                return location;
            }
            location = GL20.glGetUniformLocation(id, name);
            uniformLocations.put(name, location);
            return location;
        }

        public Program uniform(String name, int value) {
            Object o = uniforms.put(name, value);
            use();
            if (!Integer.valueOf(value).equals(o)) {
                GL20.glUniform1i(getUniformLocation(name), value);
            }
            return this;
        }

        public Program uniform(String name, boolean flag) {
            return uniform(name, flag ? 1 : 0);
        }

        public Program uniform(String name, float value) {
            Object o = uniforms.put(name, value);
            use();
            if (!Float.valueOf(value).equals(o)) {
                GL20.glUniform1f(getUniformLocation(name), value);
            }
            return this;
        }

        public Program uniform1f(String name, float... values) {
            if (values.length == 1) {
                return uniform(name, values[0]);
            }
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform1fv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform2f(String name, float... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform2fv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform(String name, Vector2f v) {
            return uniform2f(name, v.x, v.y);
        }

        public Program uniform3f(String name, float... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform3fv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform(String name, Vector3f v) {
            return uniform3f(name, v.x, v.y, v.z);
        }

        public Program uniform4f(String name, float... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform4fv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform(String name, Vector4f v) {
            return uniform4f(name, v.x, v.y, v.z, v.w);
        }

        public Program uniformMat2(String name, float... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniformMatrix2fv(getUniformLocation(name), false, values);
            }
            return this;
        }

        public Program uniform(String name, Matrix2f v) {
            return uniformMat2(name, v.m00, v.m01, v.m10, v.m11);
        }

        public Program uniformMat3(String name, float... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniformMatrix3fv(getUniformLocation(name), false, values);
            }
            return this;
        }

        public Program uniform(String name, Matrix3f v) {
            return uniformMat3(name, v.m00, v.m01, v.m02, v.m10, v.m11, v.m12, v.m20, v.m21, v.m22);
        }

        public Program uniformMat4(String name, float... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniformMatrix4fv(getUniformLocation(name), false, values);
            }
            return this;
        }

        public Program uniform(String name, Matrix4f v) {
            return uniformMat4(name, v.get(new float[16]));
        }

        public Program uniform1i(String name, int... values) {
            if (values.length == 1) {
                return uniform(name, values[0]);
            }
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform1iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform2i(String name, int... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform2iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform3i(String name, int... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform3iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform4i(String name, int... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!values.equals(o)) {
                GL20.glUniform4iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program use() {
            getState().program(id);
            return this;
        }
    }

    public class VAO {
        private int vao, vbo, vboDataCount, ebo, eboDataCount;
        private int[] attribSize = new int[16];

        private VAO() {
            getState().vao(vao = GL30.glGenVertexArrays(), 0).vbo(vbo = GL15.glGenBuffers());
        }

        public VAO vboData(float[] data, int type) {
            getState().vao(vao, ebo).vbo(vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, type);
            vboDataCount = data.length;
            return this;
        }

        public VAO vboData(float[] data) {
            return this.vboData(data, GL15.GL_STATIC_DRAW);
        }

        public VAO vertexAttrib(int index, int size, int type, boolean normalized, int stride, long pointer) {
            getState().vao(vao, ebo).vbo(vbo);
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
            GL20.glEnableVertexAttribArray(index);
            attribSize[index] = size;
            return this;
        }

        public VAO vertexAttrib(int index, int size, int type, int stride, long pointer) {
            return vertexAttrib(index, size, type, false, stride, pointer);
        }

        public VAO vertexAttrib(int index, int size, int stride, long pointer) {
            return vertexAttrib(index, size, GL11.GL_FLOAT, stride, pointer);
        }

        public VAO eboData(int[] data, int type) {
            getState().vao(vao, ebo);
            if (ebo == 0) {
                getState().ebo(ebo = GL15.glGenBuffers());
            }
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, type);
            eboDataCount = data.length;
            return this;
        }

        public VAO eboData(int[] data) {
            return eboData(data, GL15.GL_STATIC_DRAW);
        }

        public void drawElements(int mode, int count) {
            getState().vao(vao, ebo);
            GL11.glDrawElements(mode, count, GL11.GL_UNSIGNED_INT, 0);
        }

        public void drawElements(int mode) {
            drawElements(mode, eboDataCount);
        }

        public void drawElements() {
            drawElements(GL11.GL_TRIANGLES);
        }

        public void drawArrays(int mode, int first, int count) {
            getState().vao(vao, ebo);
            GL11.glDrawArrays(mode, first, count);
        }

        public void drawArrays(int mode, int count) {
            drawArrays(mode, 0, count);
        }

        public void drawArrays(int mode) {
            drawArrays(mode, vboDataCount / Arrays.stream(attribSize).sum());
        }
    }

    public static class GLState {
        private boolean cullFront, cullFace, depthTest;
        private int depthFunc, program, vao, vbo, ebo;
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

        public GLState program(int program) {
            if (this.program != program) {
                GL20.glUseProgram(program);
            }
            this.program = program;
            return this;
        }

        public GLState vao(int vao, int ebo) {
            if (this.vao != vao) {
                GL30.glBindVertexArray(vao);
            }
            this.vao = vao;
            this.ebo = ebo;
            return this;
        }

        public GLState vbo(int vbo) {
            if (this.vbo != vbo) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            }
            this.vbo = vbo;
            return this;
        }

        public GLState ebo(int ebo) {
            if (this.ebo != ebo) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            }
            this.ebo = ebo;
            return this;
        }
    }
}
