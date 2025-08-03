#version 410 core

layout (location = 0) in vec4 aPosTex;

out vec2 vTexcoord;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

void main() {
    vTexcoord = aPosTex.zw;
    gl_Position = uProjection * uView * uModel * vec4(aPosTex.xy, 0.0, 1.0);
}