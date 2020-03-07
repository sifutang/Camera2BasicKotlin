precision mediump float;

uniform sampler2D u_TextureUnit;

varying vec3 v_Color;
varying float v_ElapsedTime;

void main() {
    // 通过颜色除以粒子的运行时间，让年轻的粒子明亮，年老的粒子暗淡．
    // v_ElapsedTime可能为０，这会导致不明确的结果，但是着色器不会被终止
    gl_FragColor = vec4(v_Color / v_ElapsedTime, 1.0) * texture2D(u_TextureUnit, gl_PointCoord);
}
