#version 120

uniform sampler2D textureIn;
uniform vec2 texelSize;
uniform float offset;

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec2 stepOffset = texelSize * offset;
    vec2 halfOffset = stepOffset * 0.5;

    vec4 sum = texture2D(textureIn, uv);
    sum += texture2D(textureIn, uv + vec2( stepOffset.x,  stepOffset.y));
    sum += texture2D(textureIn, uv + vec2(-stepOffset.x,  stepOffset.y));
    sum += texture2D(textureIn, uv + vec2( stepOffset.x, -stepOffset.y));
    sum += texture2D(textureIn, uv + vec2(-stepOffset.x, -stepOffset.y));
    sum += texture2D(textureIn, uv + vec2( halfOffset.x, -halfOffset.y));
    sum += texture2D(textureIn, uv + vec2(-halfOffset.x,  halfOffset.y));
    sum += texture2D(textureIn, uv + vec2( halfOffset.x,  halfOffset.y));
    sum += texture2D(textureIn, uv + vec2(-halfOffset.x, -halfOffset.y));

    gl_FragColor = vec4(sum.rgb / 9.0, 1.0);
}


