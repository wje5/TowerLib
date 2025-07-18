package com.towergames.towerlib;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL41;

import java.util.*;

public class ShaderManager {
    private final TowerGame game;
    private Map<String, Integer> shaders = new HashMap<>();
    private Map<List<String>, Integer> programs = new HashMap<>();
    public final int pos;

    public ShaderManager(TowerGame game) {
        this.game = game;
        pos = getProgram("shaders/pos.vs", "shaders/uColor.fs");
    }

    public int getProgram(String... shaderPaths) {
        List<String> shaderPathsList = Arrays.asList(shaderPaths);
        if (programs.containsKey(shaderPathsList)) {
            return programs.get(shaderPathsList);
        }
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
                game.getLogger().error("Unrecognized shader suffix: {}", path);
                return 0;
            }
            shaderIDs[i] = GL20.glCreateShader(type);
            GL20.glShaderSource(shaderIDs[i], TowerUtil.readAll(path));
            GL20.glCompileShader(shaderIDs[i]);
            if (GL20.glGetShaderi(shaderIDs[i], GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                game.getLogger().error("Failed to compile shader {}", path);
                game.getLogger().error(GL20.glGetShaderInfoLog(shaderIDs[i]));
                return 0;
            }
            shaders.put(path, shaderIDs[i]);
        }
        int program = GL20.glCreateProgram();
        for (int shader : shaderIDs) {
            GL20.glAttachShader(program, shader);
        }
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            game.getLogger().error("Failed to link program: {}", shaderPathsList);
            game.getLogger().error(GL20.glGetProgramInfoLog(program));
            return 0;
        }
        programs.put(shaderPathsList, program);
        game.getLogger().debug("Program linked: {}", shaderPathsList);
        return program;
    }
}
