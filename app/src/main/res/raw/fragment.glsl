precision highp float;

varying vec2 textureCoordinate;

uniform sampler2D inputImageTexture;
uniform sampler2D inputThermalTexture;

uniform float cWidth;
uniform float cHeight;
uniform float tWidth;
uniform float tHeight;

uniform float cmosOnOff;
uniform float thermalOnOff;

void main() {
    mat3 counting = mat3(1.164, 1.164, 1.164,
    0.000, -0.392, 2.017,
    1.596, -0.813, 0.000);
    vec3 offset = vec3(-(16.0/255.0), -0.5, -0.5);

    vec4 yuyv = vec4(texture2D(inputImageTexture, textureCoordinate));
    int x = int(textureCoordinate.x*cWidth);
    float mod = float(x/2)*2.0;
    mod = float(x) - mod;
    vec3 cmos;

    if (mod == 1.0) { // yuyv = rgba
        cmos = yuyv.bga;
    } else {
        cmos = yuyv.rga;
    }

    cmos = counting * (cmos + offset);

    vec3 thermalrgb = vec3(texture2D(inputThermalTexture, textureCoordinate));
    int thermalx = int(textureCoordinate.x*tWidth);

    if (cmosOnOff == 1.0 && thermalOnOff == 1.0) {
        gl_FragColor = vec4((cmos*vec3(0.5)+thermalrgb*vec3(0.5)), 1.0);
    } else if (cmosOnOff == 1.0 && thermalOnOff == 0.0) {
        gl_FragColor = vec4(cmos, 1.0);
    } else if (cmosOnOff == 0.0 && thermalOnOff == 1.0) {
        gl_FragColor = vec4(thermalrgb, 1.0);
    } else {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}