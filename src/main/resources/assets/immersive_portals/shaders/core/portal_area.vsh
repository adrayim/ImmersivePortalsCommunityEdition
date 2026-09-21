#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 IP_ModelViewMat;
uniform mat4 IP_ProjMat;

out vec4 vertexColor;

// even this shader code will be transformed to add clipping

void main() {
    gl_Position = IP_ProjMat * IP_ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
}
