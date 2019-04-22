precision highp float;

varying vec2 textureCoordinate;

uniform sampler2D inputImageTexture;

uniform float width;
uniform float height;

void main() {
    mat3 counting = mat3(1.164, 1.164, 1.164,
                        0.000, -0.392, 2.017,
                        1.596, -0.813, 0.000);
    vec3 offset = vec3(-(16.0/255.0), -0.5, -0.5);

    vec4 yuyv = vec4(texture2D(inputImageTexture, textureCoordinate));
    int x = int(textureCoordinate.x*width);
    float mod = float(x/2)*2.0;
    mod = float(x) - mod;
    vec3 result;

    if(mod == 1.0) { // yuyv = rgba
        result = yuyv.bga;
    }else {
        result = yuyv.rga;
    }

    result = counting * (result + offset);

    gl_FragColor = vec4(result, 1.0);
}