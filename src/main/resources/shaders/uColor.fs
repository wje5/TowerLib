#version 410 core

out vec4 fragColor;

uniform vec4 uColor;

void main() {
    fragColor = uColor;
}