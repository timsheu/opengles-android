package com.nuvoton.opengl

import android.opengl.GLES10
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.Buffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {
    var bgColorValue = 0f
    private val vertexShader = """
        attribute vec4 position;
        uniform mat4 matrix;
        void main() {
            gl_Position = vec4(-1.0, 0.0, 0.0, 1.0);
            gl_PointSize = 5.0;
            }
            """.trimIndent()

    private val fragmentShader = """
        void main() {
        gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        }
    """.trimIndent()

    private val computeShader = """
        uniform float radius

struct Vector3f {
    float x;
    float y;
    float z;
    float w;
}

struct AttrData {
    Vector3f vertex;
    Vector3f color;
}

layout(std140, binding = 0) buffer destBuffer {
    AttrData data[];
} outBuffer;

layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;

void main() {
    ivec2 storePos = ivec2(gl_globalInvocationID.xy);

    uint gWidth = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint gHeight = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
    uint gXize = gWidth *gHeight;

    uint offset = storePos.y *gWidth + storePos.x;

    float alpha = 2.0 * 3.14159265359 * (float(offset) / float(gSize));

    outBuffer.data[offset].vertex.x = sin(alpha) * radius;
    outBuffer.data[offset].vertex.y = cos(alphs) * radius;
    outBuffer.data[offset].vertex.z = 0.0;
    outBuffer.data[offset].vertex.w = 1.0;

    outBuffer.data[offset].color.x = storePos.x / float(gWidth);
    outBuffer.data[offset].color.y = 0.0;
    outBuffer.data[offset].color.z = 1.0;
    outBuffer.data[offset].color.w = 1.0;

}

    """.trimIndent()

    private var mGLProgram : Int = -1
    private var frameNum = 0
    private var mInputBuffer: Buffer? = null
    private val mWidth = 32
    private val mHeight = 32
    private val mBytesPerPixel = 8
    private val mSize = mWidth * mHeight * mBytesPerPixel

    
    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
/*
        mInputBuffer = createInputBuffer()
        initGLSL()
        */
        GLES31.glClearColor(0f, 0f, 0f, 1f)
        val vsh = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER)
        GLES31.glShaderSource(vsh, vertexShader)
        GLES31.glCompileShader(vsh)

        val fsh = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER)
        GLES31.glShaderSource(fsh, fragmentShader)
        GLES31.glCompileShader(fsh)

        val csh = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER)
        GLES31.glShaderSource(csh, computeShader)
        GLES31.glCompileShader(csh)

        mGLProgram = GLES31.glCreateProgram()
        GLES31.glAttachShader(mGLProgram, vsh)
        GLES31.glAttachShader(mGLProgram, fsh)
//        GLES31.glAttachShader(mGLProgram, csh)
        GLES31.glLinkProgram(mGLProgram)

        GLES31.glValidateProgram(mGLProgram)

        val status = IntArray(1)

        GLES31.glGetProgramiv(mGLProgram, GLES31.GL_VALIDATE_STATUS, status, 0)
        Log.d(this.javaClass.simpleName, "validate shader program result=${GLES31.glGetProgramInfoLog(mGLProgram)}")
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES31.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        /*
        // FROM CSDN
        createEnvi()
        transferToTexture(mInputBuffer!!, fTextures[0])
        val a0 = FloatBuffer.allocate(mSize)
        val a1 = FloatBuffer.allocate(mSize)
        val a2 = FloatBuffer.allocate(mSize)

        val begin = System.currentTimeMillis()
        performCompute(fTextures[0], fTextures[1])
        performCompute(fTextures[1], fTextures[2])
        Log.w(this.javaClass.simpleName, "total compute spent: ${System.currentTimeMillis() - begin}")
        GLES31.glReadBuffer(GLES31.GL_COLOR_ATTACHMENT0)
        GLES31.glReadPixels(0, 0, mWidth, mHeight, GLES31.GL_RGBA, GLES31.GL_FLOAT, a0)

        GLES31.glReadBuffer(GLES31.GL_COLOR_ATTACHMENT1)
        GLES31.glReadPixels(0, 0, mWidth, mHeight, GLES31.GL_RGBA, GLES31.GL_FLOAT, a1)

        GLES31.glReadBuffer(GLES31.GL_COLOR_ATTACHMENT2)
        GLES31.glReadPixels(0, 0, mWidth, mHeight, GLES31.GL_RGBA, GLES31.GL_FLOAT, a2)

        val o1 = a0.array()
        val o2 = a1.array()
        val o3 = a2.array()
        o1.iterator().forEach { Log.d(this.javaClass.simpleName, "${it.toString()}") }
         */

        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glUseProgram(mGLProgram)
        val iLocRadius = GLES31.glGetUniformLocation(mGLProgram, "radius")
        GLES31.glUniform1f(iLocRadius, frameNum.toFloat())
        frameNum++
        GLES31.glDispatchCompute(2, 2, 1)
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, 0)
        GLES31.glMemoryBarrier(GLES31.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT)

        GLES31.glDrawArrays(GLES31.GL_POINTS, 0, 1)
    }

    /* FROM CSDN
    private fun createInputBuffer() : FloatBuffer {

        val floatBuffer = FloatBuffer.allocate(mSize)
        for ( i in 0 until mSize) {
            floatBuffer.put(i.toFloat())
        }
        floatBuffer.position(0)
        return floatBuffer
    }

    private val fFrame = IntArray(3)
    private val fTextures = IntArray(3)

    private fun createEnvi() {
        GLES31.glGenFramebuffers(1, fFrame, 0)
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fFrame[0])
        GLES31.glGenTextures(1, fTextures, 0)
        for (i in 0..2) {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, fTextures[i])
            GLES31.glTexStorage2D(GLES31.GL_TEXTURE_2D, 1, GLES31.GL_RGBA32F, mWidth, mHeight)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
        }
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, fTextures[0], 0)
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT1, GLES31.GL_TEXTURE_2D, fTextures[1], 0)
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT2, GLES31.GL_TEXTURE_2D, fTextures[2], 0)
    }

    private fun transferToTexture(data: Buffer, texId: Int) {
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texId)
        GLES31.glTexSubImage2D(GLES31.GL_TEXTURE_2D, 0, 0, 0, mWidth, mHeight, GLES31.GL_RGBA, GLES31.GL_FLOAT, data)
    }

    private val shader = """
        layout(local_size_x = 32, local_size_y = 32, local_size_z = 1) in;

        uniform float v[1000];
        layout(binding = 0, rgba32f) readonly uniform image2D input_image;
        layout(binding = 1, rgba32f) writeonly uniform image2D output_image;

        shared vec4 scanline[32][32];

        void main(void) {
        ivec2 pos = ivec2(gl_GlobnalInvocationID.xy);
        scanline[pos.x][pos.y] = imageLoad(input_image, pos);
        barrier();
        vec4 data = scanline[pos.x][pos.y];
        data.r = data.r + v[999];
        data.g = data.g;
        data.b = data.b;
        data.a = data.a;
        imageStore(output_image, pos.xy, data);
        }
    """.trimIndent()

    private fun initGLSL() {

        val csh = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER)
        GLES31.glShaderSource(csh, shader)
        GLES31.glCompileShader(csh)

        mGLProgram = GLES31.glCreateProgram()
        GLES31.glAttachShader(mGLProgram, csh)
        GLES31.glLinkProgram(mGLProgram)

        GLES31.glValidateProgram(mGLProgram)

        val status = IntArray(1)

        GLES31.glGetProgramiv(mGLProgram, GLES31.GL_VALIDATE_STATUS, status, 0)
        Log.d(this.javaClass.simpleName, "validate shader program result=${GLES31.glGetProgramInfoLog(mGLProgram)}")

    }

    private fun performCompute(inputTexture: Int, outputTexTure: Int) {
        GLES31.glUseProgram(mGLProgram)
        val v = FloatBuffer.allocate(1000)
        v.put(999, 512f)
        GLES31.glUniform1fv(GLES31.glGetUniformLocation(mGLProgram, "v"), 1000, v)

        GLES31.glBindImageTexture(0, inputTexture, 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_RGBA32F)
        GLES31.glBindImageTexture(1, outputTexTure, 0, false, 0, GLES31.GL_WRITE_ONLY, GLES31.GL_RGBA32F)

        GLES31.glDispatchCompute(1, 1, 1)
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
    }
    */
}