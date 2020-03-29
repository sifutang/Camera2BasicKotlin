attribute vec4 aPosition;
attribute vec4 aWaterMarkTextureCoordinate;

uniform mat4 uMVPMatrix;

varying vec2 vWaterMarkTextureCoord;

void main() {
    vWaterMarkTextureCoord = aWaterMarkTextureCoordinate.xy;
    gl_Position = uMVPMatrix * aPosition;
}
