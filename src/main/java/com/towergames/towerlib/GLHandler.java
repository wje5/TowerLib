package com.towergames.towerlib;

import de.javagl.jgltf.model.image.PixelData;
import de.javagl.jgltf.model.image.PixelDatas;
import org.joml.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.util.*;

public class GLHandler {
    private final TowerGame game;
    private Map<String, Integer> shaders = new HashMap<>();
    private Stack<GLState> stack;
    public final Program basic;
    public final Texture white;
    private VAO vaoRect, vaoRectDynamicUV;

    public GLHandler(TowerGame game) {
        this.game = game;
        game.getLogger().info("Initializing GLHandler...");
        stack = new Stack<>();
        stack.push(new GLState());
        basic = createProgram("shaders/basic.vs", "shaders/basic.fs");
        white = createTexture(false).image(GL11.GL_RGBA, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, TowerUtil.toDirectBuffer(new byte[]{-1, -1, -1, -1}));
        vaoRect = createVAO().vboData(new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f})
                .vertexAttrib(0, 3, 20, 0).vertexAttrib(1, 2, 20, 12).eboData(new int[]{0, 1, 2, 0, 2, 3});
        vaoRectDynamicUV = createVAO().vboData(new float[]{0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f}, GL15.GL_DYNAMIC_DRAW)
                .vertexAttrib(0, 3, 0, 0).vertexAttrib(1, 2, 0, 48).eboData(new int[]{0, 1, 2, 0, 2, 3});
    }

    public GLState getState() {
        return stack.peek();
    }

    public void pushStack() {
        stack.push(new GLState(stack.peek()));
    }

    public void popStack() {
        GLState popState = stack.pop();
        popState.applyState(stack.peek());
        stack.peek().activeTexture = popState.activeTexture;
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

    public Texture createTexture(boolean mipmap) {
        return new Texture(mipmap);
    }

    public Texture loadTexture(String path, boolean mipmap) {
        PixelData data = PixelDatas.create(TowerUtil.readToBuffer(path));
        getState().unpackAlignment(4);
        return createTexture(mipmap).image(GL11.GL_RGBA, data.getWidth(), data.getHeight(), GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data.getPixelsRGBA());
    }

    public void drawRect2D(float x, float y, float width, float height, Vector4f color) {
        pushStack();
        WindowHandler window = game.getWindowHandler();
        getState().texture0(white);
        basic.uniform("uColor", color).uniform("uTexture", 0);
        getState().depthTest(false)
                .model(new Matrix4f().translate(x, y, 0.0f).scale(width, height, 1.0f)).view(new Matrix4f())
                .projection(new Matrix4f().ortho(0.0f, window.getWidth(), window.getHeight(), 0.0f, 0.0f, 1.0f)).applyMVP();
        vaoRectDynamicUV.drawElements();
        popStack();
    }

    public class Texture {
        private final int id;
        private int width, height;

        private Texture(boolean mipmap) {
            GLState state = getState();
            state.texture(state.activeTexture, id = GL11.glGenTextures());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        }

        public Texture image(int internalFormat, int width, int height, int format, int dataType, ByteBuffer data) {
            GLState state = getState();
            state.texture(state.activeTexture, id);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, dataType, data);
            this.width = width;
            this.height = height;
            return this;
        }

        public Texture subImage(int xOffset, int yOffset, int width, int height, int format, int dataType, ByteBuffer data) {
            GLState state = getState();
            state.texture(state.activeTexture, id);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, xOffset, yOffset, width, height, format, dataType, data);
            return this;
        }

        public Texture updateMipmap() {
            GLState state = getState();
            state.texture(state.activeTexture, id);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            return this;
        }

        public void drawRect2D(float x, float y, float width, float height, Vector4f color) {
            pushStack();
            WindowHandler window = game.getWindowHandler();
            getState().texture0(this);
            basic.uniform("uColor", color).uniform("uTexture", 0);
            getState().depthTest(false)
                    .model(new Matrix4f().translate(x, y, 0.0f).scale(width, height, 1.0f)).view(new Matrix4f())
                    .projection(new Matrix4f().ortho(0.0f, window.getWidth(), window.getHeight(), 0.0f, 0.0f, 1.0f)).applyMVP();
            vaoRect.drawElements();
            popStack();
        }

        public void drawRect2D(float x, float y) {
            drawRect2D(x, y, width, height, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        }

        public void drawRect2D(float x, float y, float width, float height, float u, float v, float uWidth, float vHeight, Vector4f color) {
            pushStack();
            WindowHandler window = game.getWindowHandler();
            getState().texture0(this);
            vaoRectDynamicUV.vboSubdata(48, new float[]{u / this.width, v / this.height, u / this.width, (v + vHeight) / this.height,
                    (u + uWidth) / this.width, (v + vHeight) / this.height, (u + uWidth) / this.width, v / this.height});
            basic.uniform("uColor", color).uniform("uTexture", 0);
            getState().depthTest(false)
                    .model(new Matrix4f().translate(x, y, 0.0f).scale(width, height, 1.0f)).view(new Matrix4f())
                    .projection(new Matrix4f().ortho(0.0f, window.getWidth(), window.getHeight(), 0.0f, 0.0f, 1.0f)).applyMVP();
            vaoRectDynamicUV.drawElements();
            popStack();
        }

        public void drawRect2D(float x, float y, float u, float v, float uWidth, float vHeight) {
            drawRect2D(x, y, uWidth, vHeight, u, v, uWidth, vHeight, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public class Program {
        private final int id;
        private Map<String, Integer> uniformLocations = new HashMap<>();
        private Map<String, Object> uniforms = new HashMap<>();

        //Warning: uniform operation (except getUniformLocation) will bind program!
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
                GL20.glShaderSource(shaderIDs[i], TowerUtil.readToString(path));
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
            if (!Arrays.equals(values, (float[]) o)) {
                GL20.glUniform1fv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform2f(String name, float... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!Arrays.equals(values, (float[]) o)) {
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
            if (!Arrays.equals(values, (float[]) o)) {
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
            if (!Arrays.equals(values, (float[]) o)) {
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
            if (!Arrays.equals(values, (float[]) o)) {
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
            if (!Arrays.equals(values, (float[]) o)) {
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
            if (!Arrays.equals(values, (float[]) o)) {
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
            if (!Arrays.equals(values, (int[]) o)) {
                GL20.glUniform1iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform2i(String name, int... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!Arrays.equals(values, (int[]) o)) {
                GL20.glUniform2iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform3i(String name, int... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!Arrays.equals(values, (int[]) o)) {
                GL20.glUniform3iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program uniform4i(String name, int... values) {
            Object o = uniforms.put(name, values);
            use();
            if (!Arrays.equals(values, (int[]) o)) {
                GL20.glUniform4iv(getUniformLocation(name), values);
            }
            return this;
        }

        public Program use() {
            getState().program(this);
            return this;
        }
    }

    public class VAO {
        private final int vao;
        private int vbo, vboDataCount, ebo, eboDataCount;
        private int[] attribSize = new int[16];

        //Warning: Any VAO operation will bind it!
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

        public VAO vboSubdata(long offset, float[] data) {
            getState().vao(vao, ebo).vbo(vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, data);
            return this;
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

    public class GLState {
        private boolean cullFront, cullFace, depthTest;
        private int depthFunc, vao, vbo, ebo, unpackAlignment, activeTexture;
        private int[] textures;
        private Vector4f clearColor = new Vector4f();
        private Vector4i viewport = new Vector4i();
        private Program program;
        public Matrix4f model, view, projection;

        private GLState() {
            model = new Matrix4f();
            view = new Matrix4f();
            projection = new Matrix4f();
            unpackAlignment = 4;
            textures = new int[16];
        }

        private GLState(GLState state) {
            cullFront = state.cullFront;
            cullFace = state.cullFace;
            depthTest = state.depthTest;
            depthFunc = state.depthFunc;
            clearColor = state.clearColor;
            unpackAlignment = state.unpackAlignment;
            viewport = state.viewport;
            model = new Matrix4f(state.model);
            view = new Matrix4f(state.view);
            projection = new Matrix4f(state.projection);
            program = state.program;
            vao = state.vao;
            vbo = state.vbo;
            ebo = state.ebo;
            textures = Arrays.copyOf(state.textures, 16);
            activeTexture = state.activeTexture;
        }

        public void applyState(GLState state) {
            cullFront(state.cullFront);
            cullFace(state.cullFace);
            depthTest(state.depthTest);
            depthFunc(state.depthFunc);
            clearColor(state.clearColor);
            unpackAlignment(state.unpackAlignment);
            viewport(state.viewport);
            program(state.program);
            model = new Matrix4f(state.model);
            view = new Matrix4f(state.view);
            projection = new Matrix4f(state.projection);
            if (program != null) {
                applyMVP();
            }
            vao(state.vao, state.ebo);
            vbo(state.vbo);
            for (int i = 0; i < 16; i++) {
                texture(i, state.textures[i]);
            }
            //activeTexture is not applied, because it should not be manually controlled.
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
                this.depthTest = enable;
            }
            return this;
        }

        public GLState depthFunc(int depthFunc) {
            if (this.depthFunc != depthFunc) {
                GL11.glDepthFunc(depthFunc);
                this.depthFunc = depthFunc;
            }
            return this;
        }

        public GLState clearColor(Vector4f clearColor) {
            if (!this.clearColor.equals(clearColor)) {
                GL11.glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
            }
            this.clearColor = clearColor;
            return this;
        }

        public GLState unpackAlignment(int value) {
            if (this.unpackAlignment != value) {
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, value);
                this.unpackAlignment = value;
            }
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

        public GLState program(Program program) {
            if (this.program != program) {
                GL20.glUseProgram(program == null ? 0 : program.id);
                this.program = program;
            }
            return this;
        }

        public GLState model(Matrix4f model) {
            this.model = model;
            return this;
        }

        public GLState view(Matrix4f view) {
            this.view = view;
            return this;
        }

        public GLState projection(Matrix4f projection) {
            this.projection = projection;
            return this;
        }

        public void applyMVP() {
            program.uniform("uModel", model).uniform("uView", view).uniform("uProjection", projection);
        }

        public GLState vao(int vao, int ebo) {
            if (this.vao != vao) {
                GL30.glBindVertexArray(vao);
                this.vao = vao;
            }
            this.ebo = ebo;
            return this;
        }

        public GLState vbo(int vbo) {
            if (this.vbo != vbo) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                this.vbo = vbo;
            }
            return this;
        }

        public GLState ebo(int ebo) {
            if (vao == 0 & ebo != 0) {
                GLHandler.this.game.getLogger().warn("Don't bind EBO when vao = 0, that will cause state bug!");
            }
            if (this.ebo != ebo) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
                this.ebo = ebo;
            }
            return this;
        }

        public GLState activeTexture(int index) {
            if (activeTexture != index) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + index);
                this.activeTexture = index;
            }
            return this;
        }

        public GLState texture(int index, int textureID) {
            if (textures[index] != textureID) {
                activeTexture(index);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
                this.textures[index] = textureID;
            }
            return this;
        }

        public GLState texture(int index, Texture texture) {
            return texture(index, texture.id);
        }

        public GLState texture0(int id) {
            return texture(0, id);
        }

        public GLState texture0(Texture texture) {
            return texture(0, texture == null ? 0 : texture.id);
        }

        public GLState texture1(int id) {
            return texture(1, id);
        }

        public GLState texture1(Texture texture) {
            return texture(1, texture == null ? 0 : texture.id);
        }

        public GLState texture2(int id) {
            return texture(2, id);
        }

        public GLState texture2(Texture texture) {
            return texture(2, texture == null ? 0 : texture.id);
        }

        public GLState texture3(int id) {
            return texture(3, id);
        }

        public GLState texture3(Texture texture) {
            return texture(3, texture == null ? 0 : texture.id);
        }

        public GLState texture4(int id) {
            return texture(4, id);
        }

        public GLState texture4(Texture texture) {
            return texture(4, texture == null ? 0 : texture.id);
        }

        public GLState texture5(int id) {
            return texture(5, id);
        }

        public GLState texture5(Texture texture) {
            return texture(5, texture == null ? 0 : texture.id);
        }

        public GLState texture6(int id) {
            return texture(6, id);
        }

        public GLState texture6(Texture texture) {
            return texture(6, texture == null ? 0 : texture.id);
        }

        public GLState texture7(int id) {
            return texture(7, id);
        }

        public GLState texture7(Texture texture) {
            return texture(7, texture == null ? 0 : texture.id);
        }

        public GLState texture8(int id) {
            return texture(8, id);
        }

        public GLState texture8(Texture texture) {
            return texture(8, texture == null ? 0 : texture.id);
        }

        public GLState texture9(int id) {
            return texture(9, id);
        }

        public GLState texture9(Texture texture) {
            return texture(9, texture == null ? 0 : texture.id);
        }

        public GLState texture10(int id) {
            return texture(10, id);
        }

        public GLState texture10(Texture texture) {
            return texture(10, texture == null ? 0 : texture.id);
        }

        public GLState texture11(int id) {
            return texture(11, id);
        }

        public GLState texture11(Texture texture) {
            return texture(11, texture == null ? 0 : texture.id);
        }

        public GLState texture12(int id) {
            return texture(12, id);
        }

        public GLState texture12(Texture texture) {
            return texture(12, texture == null ? 0 : texture.id);
        }

        public GLState texture13(int id) {
            return texture(13, id);
        }

        public GLState texture13(Texture texture) {
            return texture(13, texture == null ? 0 : texture.id);
        }

        public GLState texture14(int id) {
            return texture(14, id);
        }

        public GLState texture14(Texture texture) {
            return texture(14, texture == null ? 0 : texture.id);
        }

        public GLState texture15(int id) {
            return texture(15, id);
        }

        public GLState texture15(Texture texture) {
            return texture(15, texture == null ? 0 : texture.id);
        }
    }
}
