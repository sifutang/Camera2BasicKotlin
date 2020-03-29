attribute vec4 a_Position;
attribute vec4 a_TextureCoordinate;

varying vec2 vTextureUnitCoordinate;

void main() {
    gl_Position = a_Position;
    vTextureUnitCoordinate = a_TextureCoordinate.xy;
}
