#version 410 core
#define MAX_LIGHTS 4
#define CSM_COUNT 2

in vec3 vPosition;
in vec2 vTexcoord;
in mat3 vTBN;
in vec4[CSM_COUNT] vCSMPosInLightSpace;

out vec4 fragColor;

uniform vec3 uCameraPosition;

uniform vec4 uBaseColorFactor;
uniform sampler2D uBaseColorTexture;
uniform float uMetallicFactor;
uniform float uRoughnessFactor;
uniform sampler2D uMetallicRoughnessTexture;
uniform sampler2D uNormalTexture;
uniform float uNormalScale;
uniform sampler2D uOcclusionTexture;
uniform float uOcclusionStrength;
uniform vec3 uEmissiveFactor;
uniform sampler2D uEmissiveTexture;

uniform int uLightCount;
uniform vec3 uLightPositions[MAX_LIGHTS + 1];
uniform vec3 uLightColors[MAX_LIGHTS + 1];
uniform sampler2D uCSMShadowMaps[CSM_COUNT];
uniform sampler2D uShadowMaps[MAX_LIGHTS];

const float PI = 3.14159265359;

float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    float nom = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
    return nom / max(denom, 0.0000001);
}

float GeometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    float nom = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    return nom / denom;
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(max(1.0 - cosTheta, 0.0), 5.0);
}

float calcCSMShadow() {
    for(int i = 0; i < CSM_COUNT; i++){
        vec4 posInLightSpace = vCSMPosInLightSpace[i];
        vec3 pos = posInLightSpace.xyz / posInLightSpace.w;
        pos = pos * 0.5 + 0.5;
//         vec3 lightDir = uLightPositions[0];
//         float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);
        float bias = 0.0000005;
        if(pos.z + bias < texture(uCSMShadowMaps[i], pos.xy).r){
            return 0.0;
        }
    }
    return 1.0;
}

void main() {
    vec4 baseColor = texture(uBaseColorTexture, vTexcoord) * uBaseColorFactor;
    vec2 metallicRoughness = texture(uMetallicRoughnessTexture, vTexcoord).bg;
    float metallic = metallicRoughness.x * uMetallicFactor;
    float roughness = metallicRoughness.y * uRoughnessFactor;
    vec3 normalMap = texture(uNormalTexture, vTexcoord).rgb * 2.0 - 1.0;
    normalMap.xy *= uNormalScale;
    vec3 N = normalize(vTBN * normalMap);
    float ao = texture(uOcclusionTexture, vTexcoord).r;
    ao = 1.0 + uOcclusionStrength * (ao - 1.0);

    vec3 emissive = texture(uEmissiveTexture, vTexcoord).rgb * uEmissiveFactor;
    vec3 V = normalize(uCameraPosition - vPosition);
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, baseColor.rgb, metallic);

    vec3 Lo = vec3(0.0);

    for (int i = 0; i < uLightCount; i++) {
        vec3 L;
        float attenuation = 1.0;

        if (i == 0) { // 平行光
            L = normalize(uLightPositions[i]);
        } else { // 点光源
            L = normalize(uLightPositions[i] - vPosition);
            float distance = length(uLightPositions[i] - vPosition);
            attenuation = 1.0 / (distance * distance);
        }

        vec3 H = normalize(V + L);
        vec3 radiance = uLightColors[i] * attenuation;
        float NDF = DistributionGGX(N, H, roughness);
        float G = GeometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

        vec3 nominator = NDF * G * F;
        float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0);
        vec3 specular = nominator / max(denominator, 0.001);

        vec3 kS = F;
        vec3 kD = vec3(1.0) - kS;
        kD *= 1.0 - metallic;

        float NdotL = max(dot(N, L), 0.0);

        float shadow = 1.0;
        for(int i = 0; i < CSM_COUNT; i++){
            vec4 posInLightSpace = vCSMPosInLightSpace[i];
            vec3 pos = posInLightSpace.xyz / posInLightSpace.w;
            pos = pos * 0.5 + 0.5;
            vec3 lightDir = uLightPositions[0];
            float bias = max(0.00005 * (1.0 - dot(vTBN[2], lightDir)), 0.0000005);
            if(pos.x >= 0.0 && pos.x <= 1.0 && pos.y >= 0.0 && pos.y <= 1.0 && pos.z > 0.0){
                if(pos.z > 0.0 && pos.z + bias < texture(uCSMShadowMaps[i], pos.xy).r){
                     shadow = 0.0;
                }
                break;
            }
        }

        Lo += (kD * baseColor.rgb / PI + specular) * radiance * NdotL * shadow;
    }

    vec3 ambient = vec3(0.01) * baseColor.rgb * ao;
    vec3 color = ambient + Lo + emissive;
    // HDR色调映射
    color = color / (color + vec3(1.0));

    fragColor = vec4(color, baseColor.a);
    fragColor = baseColor;
}