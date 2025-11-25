uniform mat4 g_WorldViewProjectionMatrix;
uniform mat3 g_WorldMatrixInverseTranspose;

// attributes
attribute vec3 inPosition;
attribute vec3 inNormal;

// output
varying vec4 vertColor;

void main() {
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);

    // DEBUG: Visualizing Normals
    // We convert the normal direction (-1 to 1) to color range (0 to 1)
    vec3 debugNormal = normalize(g_WorldMatrixInverseTranspose * inNormal);
    vertColor = vec4(debugNormal * 0.5 + 0.5, 1.0);
}