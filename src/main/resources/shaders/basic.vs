#version 410 core

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexcoord;

out vec2 vTexcoord;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

void main() {
    vTexcoord = aTexcoord;
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
}