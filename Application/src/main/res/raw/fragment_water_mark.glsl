precision mediump float;

uniform sampler2D sWaterMarkSampler;

varying vec2 vWaterMarkTextureCoord;

void main() {
    vec4 color = texture2D(sWaterMarkSampler, vWaterMarkTextureCoord);
//    if (color.a < 0.5) {
//        discard;
//    }
    gl_FragColor = color;
}