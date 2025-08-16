#version 410 core
#define MAX_LIGHTS 4
#define CSM_COUNT 2

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec4 aTangent;
layout (location = 3) in vec2 aTexcoord;
layout (location = 4) in vec4 aColor;
layout (location = 5) in vec4 aJoints;
layout (location = 6) in vec4 aWeights;
layout (location = 7) in vec3 aMorphPosition;
layout (location = 8) in vec3 aMorphNormal;
layout (location = 9) in vec3 aMorphTangent;
layout (location = 10) in vec3 aMorphPosition1;
layout (location = 11) in vec3 aMorphNormal1;
layout (location = 12) in vec3 aMorphTangent1;
layout (location = 13) in vec3 aMorphPosition2;
layout (location = 14) in vec3 aMorphNormal2;
layout (location = 15) in vec3 aMorphTangent2;

out vec3 vPosition;
out vec2 vTexcoord;
out mat3 vTBN;
out vec4 vCSMPosInLightSpace[CSM_COUNT];

uniform vec3 uMorphWeights;
uniform JointMatircesBlock {
    mat4 jointMatrices[1024];
};
uniform mat4 uCSMLightSpaces[CSM_COUNT];

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;
uniform bool uEnableSkinning;

void main() {
    vec3 morphedPosition = aPosition;
    vec3 morphedNormal = aNormal;
    vec3 morphedTangent = aTangent.xyz;
    if(uMorphWeights.x > 0.0) {
        morphedPosition += uMorphWeights.x * aMorphPosition;
        morphedNormal += uMorphWeights.x * aMorphNormal;
        morphedTangent += uMorphWeights.x * aMorphTangent;
    }
    if(uMorphWeights.y > 0.0) {
        morphedPosition += uMorphWeights.y * aMorphPosition1;
        morphedNormal += uMorphWeights.y * aMorphNormal1;
        morphedTangent += uMorphWeights.y * aMorphTangent1;
    }
    if(uMorphWeights.z > 0.0) {
        morphedPosition += uMorphWeights.z * aMorphPosition2;
        morphedNormal += uMorphWeights.z * aMorphNormal2;
        morphedTangent += uMorphWeights.z * aMorphTangent2;
    }
    if (uEnableSkinning) {
        mat4 skinMatrix = aWeights.x * jointMatrices[int(aJoints.x)] +
        aWeights.y * jointMatrices[int(aJoints.y)] +
        aWeights.z * jointMatrices[int(aJoints.z)] +
        aWeights.w * jointMatrices[int(aJoints.w)];

        vec4 skinnedPosition = skinMatrix * vec4(morphedPosition, 1.0);
        vec3 skinnedNormal = mat3(skinMatrix) * morphedNormal;
        vec3 skinnedTangent = mat3(skinMatrix) * morphedTangent;

        vec3 N = normalize(mat3(transpose(inverse(uModel))) * skinnedNormal);
        vec3 T = normalize(mat3(uModel) * skinnedTangent);
        vec3 B = cross(N, T) * aTangent.w;
        vTBN = mat3(T, B, N);
        vPosition = vec3(uModel * skinnedPosition);
    } else {
        vec3 N = normalize(mat3(transpose(inverse(uModel))) * morphedNormal);
        vec3 T = normalize(mat3(uModel) * morphedTangent);
        vec3 B = cross(N, T) * aTangent.w;
        vTBN = mat3(T, B, N);
        vPosition = vec3(uModel * vec4(morphedPosition, 1.0));
    }
    vTexcoord = aTexcoord;
    for(int i = 0; i < CSM_COUNT; i++){
        vCSMPosInLightSpace[i] = uCSMLightSpaces[i] * vec4(vPosition, 1.0);
    }
    gl_Position = uProjection * uView * vec4(vPosition, 1.0);
}