#version 410 core

in vec2 vTexcoord;

out vec4 fragColor;

uniform vec4 uColor;
uniform sampler2D uTexture;

void main() {
    fragColor = texture(uTexture, vTexcoord) * uColor;
}