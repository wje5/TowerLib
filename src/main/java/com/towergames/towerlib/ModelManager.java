package com.towergames.towerlib;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.image.PixelData;
import de.javagl.jgltf.model.image.PixelDatas;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

public class ModelManager {
    private final TowerGame game;
    private Map<String, Model> models = new HashMap<>();
    private Map<TextureModel, GLHandler.Texture> textures = new HashMap<>();
    private GLHandler.UBO uboJointMatrices;

    public ModelManager(TowerGame game) {
        this.game = game;
        uboJointMatrices = game.getGlHandler().createUBO();
    }

    public Model loadModel(String path) {
        if (models.containsKey(path)) {
            return models.get(path);
        } else {
            game.getLogger().debug("Loading model: {}", path);
            try {
                GltfModel m = new GltfModelReader().read(TowerUtil.getResourcePath(path));
                Model model = new Model(m);
                models.put(path, model);
                return model;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load model: " + path, e);
            }
        }
    }

    public GLHandler.Texture loadTexture(TextureModel model, boolean srgb, boolean mipmap, GLHandler.Texture defaultTexture) {
        if (textures.containsKey(model)) {
            return textures.get(model);
        }
        if (model == null) {
            return defaultTexture;
        }
        PixelData data = PixelDatas.create(model.getImageModel().getImageData());
        GLHandler.Texture texture = game.getGlHandler().createTexture(mipmap).image(srgb ? GL21.GL_SRGB_ALPHA : GL11.GL_RGBA,
                data.getWidth(), data.getHeight(), GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data.getPixelsRGBA());
        textures.put(model, texture);
        return texture;
    }

    public GLHandler.Texture loadTexture(TextureModel model, boolean srgb, boolean mipmap) {
        return loadTexture(model, srgb, mipmap, null);
    }

    public class Model {
        private List<Primitive> primitives = new ArrayList<>();
        private Map<String, AnimationModel> animations = new HashMap<>();

        private Model(GltfModel model) {
            model.getSceneModels().forEach(scene -> {
                scene.getNodeModels().forEach(this::setupNode);
            });
            model.getAnimationModels().forEach(e -> {
                animations.put(e.getName(), e);
                game.getLogger().debug("load animation: {}", e.getName());
            });
        }

        private void setupNode(NodeModel node) {
            node.getMeshModels().forEach(mesh -> {
                mesh.getMeshPrimitiveModels().forEach(primitive -> {
                    game.getLogger().debug("mesh: {}", mesh.getName());
                    primitives.add(new Primitive(node, primitive));
                });
            });
            node.getChildren().forEach(this::setupNode);
        }

        public void doRender(boolean renderDepth, Map<String, Float> animationState) {
            GLHandler gl = game.getGlHandler();
            Map<NodeModel, float[][]> dirty = new HashMap<>();
            if (animationState != null && !animationState.isEmpty()) {
                animationState.forEach((name, time) -> {
                    AnimationModel animation = animations.get(name);
                    animation.getChannels().forEach(channel -> {
                        AnimationModel.Sampler sampler = channel.getSampler();
                        float[] data = interpolation(sampler, time);
                        NodeModel node = channel.getNodeModel();
                        float[][] a = dirty.get(node);
                        if (a == null) {
                            a = new float[][]{node.getTranslation(), node.getRotation(), node.getScale(), node.getWeights()};
                            dirty.put(node, a);
                        }
                        switch (channel.getPath()) {
                            case "translation":
                                node.setTranslation(data);
                                break;
                            case "rotation":
                                node.setRotation(data);
                                break;
                            case "scale":
                                node.setScale(data);
                                break;
                            case "weights":
                                node.setWeights(data);
                        }
                    });
                });
            }
            GLHandler.Program program = renderDepth ? null : gl.pbr;
            SkinModel last = null;
            for (Primitive primitive : primitives) {
                SkinModel skin = primitive.node.getSkinModel();
                if (skin == null) {
                    program.uniform("uEnableSkinning", false);
                    float[] f = primitive.node.computeGlobalTransform(new float[16]);
                    gl.getState().pushMVP();
                    gl.getState().model.mul(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14], f[15]);
                    gl.getState().applyMVP();
                    primitive.doRender(renderDepth);
                    gl.getState().popMVP();
                } else {
                    program.uniform("uEnableSkinning", true);
                    if (skin != last) {
                        List<NodeModel> joints = skin.getJoints();
                        float[] jointMatrices = new float[joints.size() * 16];
                        for (int i = 0; i < joints.size(); i++) {
                            float[] f = joints.get(i).computeGlobalTransform(new float[16]);
                            float[] ib = skin.getInverseBindMatrix(i, new float[16]);
                            float[] r = new Matrix4f(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14], f[15])
                                    .mul(ib[0], ib[1], ib[2], ib[3], ib[4], ib[5], ib[6], ib[7], ib[8], ib[9], ib[10], ib[11], ib[12], ib[13], ib[14], ib[15]).get(new float[16]);
                            System.arraycopy(r, 0, jointMatrices, i * 16, 16);
                        }
                        uboJointMatrices.uboData(jointMatrices, GL15.GL_DYNAMIC_DRAW);
                        uboJointMatrices.bind(0);
                        program.uniformBlock("JointMatircesBlock", 0);
                        gl.getState().applyMVP();
                        primitive.doRender(renderDepth);
                        last = skin;
                    }
                }
            }
        }

        private float[] interpolation(AnimationModel.Sampler sampler, float time) {
            ByteBuffer input = sampler.getInput().getBufferViewModel().getBufferViewData();
            ByteBuffer output = sampler.getOutput().getBufferViewModel().getBufferViewData();
            FloatBuffer fb = output.asFloatBuffer();
            int frameCount = input.remaining() / 4;
            int count = output.remaining() / 4 / frameCount;
            float f = 0, next = 0, t = 0;
            float[] a = new float[count], b = new float[count];
            int i = 0;
            for (; input.hasRemaining(); i++) {
                f = next;
                next = input.getFloat();
                if (f <= time && next >= time) {
                    t = (time - f) / (next - f);
                    fb.position(i * count);
                    fb.get(b);
                    if (i == 0) {
                        return b;
                    }
                    fb.position((i * count - count));
                    fb.get(a);
                    break;
                }
            }
            if (time >= next) {
                fb.position((i * count - count));
                output.asFloatBuffer().get(b);
                return b;
            }
            AnimationModel.Interpolation interpolation = sampler.getInterpolation();
            switch (interpolation) {
                case STEP:
                    return a;
                case LINEAR:
                    float[] r = new float[count];
                    for (int j = 0; j < count; j++) {
                        r[j] = (1 - t) * a[j] + t * b[j];
                    }
                    return r;
                case CUBICSPLINE:
                    throw new RuntimeException("TODO");
                    //TODO cubic spline interpolation
            }
            return null;
        }

        public class Primitive {
            private final NodeModel node;
            private final MeshPrimitiveModel primitive;
            private final GLHandler.VAO vao;

            private Primitive(NodeModel node, MeshPrimitiveModel primitive) {
                this.node = node;
                this.primitive = primitive;
                GLHandler gl = game.getGlHandler();
                vao = gl.createVAO();
                if (primitive.getAttributes().get("TANGENT") == null) {
                    throw new RuntimeException("Failed to create primitive: No tangent");
                }
                int count = primitive.getAttributes().get("POSITION").getCount();
                int size = primitive.getAttributes().values().stream().mapToInt(AccessorModel::getElementSizeInBytes).sum() * count;
                int morphSize = count * 36 * primitive.getTargets().size();
                ByteBuffer buffer = ByteBuffer.allocateDirect(size + morphSize);
                primitive.getAttributes().entrySet().stream()
                        .sorted(Comparator.comparingInt(a -> getAttributeIndex(a.getKey()))).forEach(e -> {
                            AccessorModel m = e.getValue();
                            game.getLogger().debug("key: {}, type: {}, size:{}/{}", e.getKey(), m.getComponentType(), m.getElementSizeInBytes(), m.getComponentSizeInBytes());
                            vao.vertexAttrib(getAttributeIndex(e.getKey()), m.getElementSizeInBytes() / m.getComponentSizeInBytes(), m.getComponentType(), 0, buffer.position());
                            buffer.put(m.getBufferViewModel().getBufferViewData());
                        });
                game.getLogger().debug("targets: {}", primitive.getTargets());
                int targetSize = primitive.getTargets().size();
                if (targetSize > 3) {
                    throw new RuntimeException("This model have " + targetSize + " targets, but max 3 supported.");
                }
                for (int i = 0; i < targetSize; i++) {
                    for (Map.Entry<String, AccessorModel> e : primitive.getTargets().get(i).entrySet()) {
                        AccessorModel m = e.getValue();
                        game.getLogger().debug("morph key: {}", e.getKey());
                        vao.vertexAttrib(7 + i * 3 + getAttributeIndex(e.getKey()), m.getElementSizeInBytes() / m.getComponentSizeInBytes(), m.getComponentType(), 0, buffer.position());
                        buffer.put(m.getBufferViewModel().getBufferViewData());
                    }
                }
                buffer.flip();

                AccessorModel indices = primitive.getIndices();
                vao.vboData(buffer).eboData(indices.getAccessorData().createByteBuffer(), GL15.GL_STATIC_DRAW, indices.getComponentType());
            }

            public void doRender(boolean renderDepth) {
                GLHandler gl = game.getGlHandler();
                float[] weights = node.getWeights();
                if (weights == null) {
                    weights = node.getMeshModels().get(0).getWeights();
                    if (weights == null) {
                        weights = new float[primitive.getTargets().size()];
                    }
                }
                MaterialModelV2 material = (MaterialModelV2) primitive.getMaterialModel();
                if (material == null) {
                    material = new MaterialModelV2();
                }
                gl.getState().texture0(loadTexture(material.getBaseColorTexture(), true, true, gl.white))
                        .texture1(loadTexture(material.getMetallicRoughnessTexture(), false, true, gl.white))
                        .texture2(loadTexture(material.getNormalTexture(), false, true, gl.defaultNormal))
                        .texture3(loadTexture(material.getOcclusionTexture(), false, true, gl.white))
                        .texture4(loadTexture(material.getEmissiveTexture(), false, true));
                gl.pbr.uniform("uBaseColorTexture", 0).uniform("uMetallicRoughnessTexture", 1).uniform("uNormalTexture", 2)
                        .uniform("uOcclusionTexture", 3).uniform("uEmissiveTexture", 4)
                        .uniform4f("uBaseColorFactor", material.getBaseColorFactor()).uniform("uMetallicFactor", material.getMetallicFactor())
                        .uniform("uRoughnessFactor", material.getRoughnessFactor()).uniform("uNormalScale", material.getNormalScale())
                        .uniform("uOcclusionStrength", material.getOcclusionStrength()).uniform3f("uEmissiveFactor", material.getEmissiveFactor())
                        .uniform3f("uMorphWeights", weights).lights(gl.createLight(TowerUtil.getDirection(-20f, 40), new Vector3f(1.0f, 1.0f, 1.0f)));
                vao.drawElements();
//                vao.drawArrays(GL11.GL_TRIANGLES);
            }

            private int getAttributeIndex(String attribute) {
                switch (attribute) {
                    case "POSITION":
                        return 0;
                    case "NORMAL":
                        return 1;
                    case "TANGENT":
                        return 2;
                    case "TEXCOORD_0":
                        return 3;
                    case "COLOR_0":
                        return 4;
                    case "JOINTS_0":
                        return 5;
                    case "WEIGHTS_0":
                        return 6;
                    default:
                        return -1;
                }
            }
        }
    }
}
