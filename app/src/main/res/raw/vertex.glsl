attribute vec4 position;
attribute vec4 inputTextureCoordinate;

varying vec2 textureCoordinate;

void main() {
    gl_Position = position;
//    textureCoordinate = vec2(inputTextureCoordinate.x/float(2), inputTextureCoordinate.y);
    textureCoordinate = inputTextureCoordinate.xy;
}