package com.nuvoton.opengl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.charset.Charset
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class MyGLRenderer(context: Context) : GLSurfaceView.Renderer {
    companion object {
        val TEXTURE_NO_ROTATION = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f)
        val TEXTURE_ROTATED_90 = floatArrayOf(
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f
        )

        val TEXTURE_ROTATED_180 = floatArrayOf(
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        )

        val TEXTURE_ROTATED_270 = floatArrayOf(
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        )
    }

    var mWidth = 640
    var mHeight = 480
    var frameWidth = -1
    var frameHeight = -1

    var mThermalWidth = 32
    var mThermalHeight = 32

    val mBytesPerPixelInYUV = 2
    val mBytesPerPixelInRGB = 3
    val dataSize = (mWidth * mHeight * mBytesPerPixelInYUV).toInt()
    var cmosImageBuffer: ByteBuffer? = null
    var thermalDataBuffer: IntBuffer? = null
    var thermalImageBuffer: ByteBuffer? = null

    var imageOnOff = Pair(true, true)

    private val vertices = floatArrayOf(
        -1f, 1f, 0f,
        -1f, -1f, 0f,
        1f, 1f, 0f,
        1f, -1f, 0f
    )


    private val textureCoordinate = TEXTURE_ROTATED_270

    private var mVerticesBuffer: FloatBuffer? = null
    private var mTextureCoordinateBuffer: FloatBuffer? = null

    private var mPositionHandler = -1
    private var mTextureCoordinateHandler = -1
    private var mGLCMOSUniformTexture = -1
    private var mGLThermalUniformUvTexture = -1
    private var mGLUniformWidthHandler = -1
    private var mGLUniformHeightHandler = -1

    private var mProgramHandler = -1

    private var mTextureId = -1
//    private var mUvTextureId = -1

    private var context: Context? = context

    init {
        mVerticesBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        mVerticesBuffer!!.put(vertices).position(0)
        mTextureCoordinateBuffer = ByteBuffer.allocateDirect(textureCoordinate.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTextureCoordinateBuffer!!.put(textureCoordinate).position(0)
        cmosImageBuffer = ByteBuffer.allocate(dataSize)
        for (i in 0 until dataSize) {
            when(i) {
                in 0 until dataSize*1/2 + 640 -> {
                    when(i%4) {
                        0 -> {
                            cmosImageBuffer!!.put(255.toByte())
                        }
                        1 -> {
                            cmosImageBuffer!!.put(255.toByte())
                        }
                        2 -> {
                            cmosImageBuffer!!.put(0.toByte())
                        }
                        3 -> {
                            cmosImageBuffer!!.put(0.toByte())
                        }
                    }
                }
                in (dataSize*1/2 + 640) until (dataSize*1/2 + 640*2) -> {
                    when(i%4) {
                        0 -> {
                            cmosImageBuffer!!.put(149.toByte())
                        }
                        1 -> {
                            cmosImageBuffer!!.put(43.toByte())
                        }
                        2 -> {
                            cmosImageBuffer!!.put(0.toByte())
                        }
                        3 -> {
                            cmosImageBuffer!!.put(21.toByte())
                        }
                    }
                }
                in (dataSize*1/2 + 640*2) until (dataSize) -> {
                    when(i%4) {
                        0 -> {
                            cmosImageBuffer!!.put(76.toByte())
                        }
                        1 -> {
                            cmosImageBuffer!!.put(84.toByte())
                        }
                        2 -> {
                            cmosImageBuffer!!.put(255.toByte())
                        }
                        3 -> {
                            cmosImageBuffer!!.put(255.toByte())
                        }
                    }
                }
            }
        }
        cmosImageBuffer!!.position(0)

        thermalDataBuffer = IntBuffer.allocate(mThermalWidth * mThermalHeight)
        for (i in 0 until thermalDataBuffer!!.capacity()) {
            thermalDataBuffer!!.put(((NuColor.COLOR_TABLE_LOWER_SIZE+600)..(NuColor.COLOR_TABLE_LOWER_SIZE+1600)).random())
        }
        thermalDataBuffer!!.position(0)

        thermalImageBuffer = ByteBuffer.allocate(mThermalWidth * mThermalHeight * mBytesPerPixelInRGB)

        (0 until thermalDataBuffer!!.capacity()).forEach {
            val value = thermalDataBuffer!![it]
            val rgb = NuColor.RGB_ColorTable[value]
            thermalImageBuffer!!.put(it*mBytesPerPixelInRGB, rgb.R.toByte())
            thermalImageBuffer!!.put(it*mBytesPerPixelInRGB+1, rgb.G.toByte())
            thermalImageBuffer!!.put(it*mBytesPerPixelInRGB+2, rgb.B.toByte())
        }

        thermalImageBuffer!!.position(0)
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        
        val textures = intArrayOf(0)
        GLES20.glGenTextures(1, textures, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth/2, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, cmosImageBuffer)
        mTextureId = textures[0]

//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1])
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat());
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, cmosImageBuffer)
//        mUvTextureId = textures[1]


        val compileStatus = intArrayOf(-1)

        var vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        if (vertexShaderHandle != 0) {
            GLES20.glShaderSource(vertexShaderHandle, loadRawString(R.raw.vertex))
            GLES20.glCompileShader(vertexShaderHandle)

            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) { // compiled failed
                Log.d(this.javaClass.simpleName, "Vertex shader compile status = ${GLES20.glGetShaderInfoLog(vertexShaderHandle)}")
                GLES20.glDeleteShader(vertexShaderHandle)
                vertexShaderHandle = 0
            }

        }

        if (vertexShaderHandle == 0) throw RuntimeException("Error creating vertex shader")

        var fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        if (fragmentShaderHandle != 0) {
            GLES20.glShaderSource(fragmentShaderHandle, loadRawString(R.raw.fragment))

            GLES20.glCompileShader(fragmentShaderHandle)

            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) { // compiled failed
                Log.d(this.javaClass.simpleName, "Fragment shader compile status = ${GLES20.glGetShaderInfoLog(fragmentShaderHandle)}")
                GLES20.glDeleteShader(fragmentShaderHandle)
                fragmentShaderHandle = 0
            }

        }

        if (fragmentShaderHandle == 0) throw RuntimeException("Error creating fragment shader")

        var programHandler = GLES20.glCreateProgram()
        if (programHandler != 0) {
            GLES20.glAttachShader(programHandler, vertexShaderHandle)
            GLES20.glAttachShader(programHandler, fragmentShaderHandle)
            GLES20.glBindAttribLocation(programHandler, 0, "position")
            GLES20.glBindAttribLocation(programHandler, 1, "inputTextureCoordinate")
            GLES20.glLinkProgram(programHandler)

            val linkStatus = intArrayOf(0)
            GLES20.glGetProgramiv(programHandler, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                GLES20.glDeleteShader(programHandler)
                programHandler = 0
            }
        }

        if (programHandler == 0) throw java.lang.RuntimeException("Error creating program")

        mPositionHandler = GLES20.glGetAttribLocation(programHandler, "position")
        mTextureCoordinateHandler = GLES20.glGetAttribLocation(programHandler, "inputTextureCoordinate")
        mGLCMOSUniformTexture = GLES20.glGetUniformLocation(programHandler, "inputImageTexture")
//        mGLUniformUvTexture = GLES20.glGetUniformLocation(programHandler, "uvTexture")
        mGLUniformWidthHandler = GLES20.glGetUniformLocation(programHandler, "width")
        mGLUniformHeightHandler = GLES20.glGetUniformLocation(programHandler, "height")

        GLES20.glUseProgram(programHandler)
        mProgramHandler = programHandler

        Log.d(this.javaClass.simpleName, "OpenGLES20 init done")
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        frameWidth = width
        frameHeight = height
//        val frameRatio = frameWidth.toFloat() / frameHeight.toFloat()
//        val ratio = mWidth.toFloat() / mHeight.toFloat()
//        if (frameRatio > ratio) {
//            frameHeight = (frameWidth/ratio).toInt()
//        }else {
//            frameWidth = (frameHeight*ratio).toInt()
//        }
        GLES20.glViewport(0, 0, frameWidth, frameHeight)
    }

    override fun onDrawFrame(unused: GL10?) {
//        Log.d(this.javaClass.simpleName, "onDraw")
        GLES20.glUseProgram(mProgramHandler)
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glVertexAttribPointer(mPositionHandler, 3, GLES20.GL_FLOAT, false, 0, mVerticesBuffer)
        GLES20.glEnableVertexAttribArray(mPositionHandler)
        GLES20.glVertexAttribPointer(mTextureCoordinateHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordinateBuffer)
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandler)
        GLES20.glUniform1f(mGLUniformWidthHandler, (mWidth).toFloat())
        GLES20.glUniform1f(mGLUniformHeightHandler, (mHeight).toFloat())

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId)
        GLES20.glUniform1i(mGLCMOSUniformTexture, 0)

//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mUvTextureId)
//        GLES20.glUniform1i(mGLUniformUvTexture, 1)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth/2, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, cmosImageBuffer)
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 0, frameWidth, frameHeight, 0, GLES20.GL_UNSIGNED_BYTE, cmosImageBuffer)

        GLES20.glDisableVertexAttribArray(mPositionHandler)
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 1)

//        Log.d(this.javaClass.simpleName, "${cmosImageBuffer?.get(0)}")
    }

    private fun loadRawString(rawId : Int) : String {
        val inputStream = context?.resources?.openRawResource(rawId)
        return inputStream!!.bufferedReader(Charset.defaultCharset()).use { it.readText() }
    }
}