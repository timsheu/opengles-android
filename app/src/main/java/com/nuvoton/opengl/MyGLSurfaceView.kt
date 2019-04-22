package com.nuvoton.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.ViewManager
import org.jetbrains.anko.custom.ankoView

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer : MyGLRenderer
    var value = 0
    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context)

        setRenderer(renderer)
        // prevents the GLSurfaceView frame from being redrawn until you call requestRender(), which is more efficient for this sample app.
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
//        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    fun updateTestDataContent() : Boolean {
//        val byteArray = ByteArray(renderer.dataSize)
//        var ori = renderer.cmosImageBuffer?.get(0)!!.toInt()
//        ori += 100
//        ori %= 256
//        (0 until renderer.dataSize).forEach { i ->
//            when(i) {
//                in 0 until renderer.dataSize -> {
//                    when(i%4) {
//                        0 -> {
//                            byteArray[i] = ori.toByte()
//                        }
//                        1 -> {
//                            byteArray[i] = 255.toByte()
//                        }
//                        2 -> {
////                            byteArray[i] = 128.toByte()
//                        }
//                        3 -> {
//                            byteArray[i] = 255.toByte()
//                        }
//                    }
//                }
////                in renderer.dataSize/4 until renderer.dataSize/2 -> {
////                    if(i%3 == 1) byteArray[i] = ori.toByte()
////                }
////                else -> {
////                    if(i%3 == 2) byteArray[i] = ori.toByte()
////                }
//            }
//        }
//        renderer.cmosImageBuffer?.clear()
//        renderer.cmosImageBuffer?.put(byteArray)
//        renderer.cmosImageBuffer?.position(0)
//        renderer.cmosImageBuffer?.position(0)
//        for (i in 0 until renderer.dataSize) {
//            var v = renderer.cmosImageBuffer?.get(i)?.toInt()!!
//            when(i%3) {
//                else -> {
//                    v += 100
//                    v %= 256
//                }
//            }
//            renderer.cmosImageBuffer?.put(v.toByte())
//        }
//        renderer.cmosImageBuffer?.position(0)
//        Log.d(this.javaClass.simpleName, "" +
//                "\n${renderer.cmosImageBuffer?.get(0)}/${renderer.cmosImageBuffer?.get(1)}/${renderer.cmosImageBuffer?.get(2)}" +
//                "\n${renderer.cmosImageBuffer?.get(3)}/${renderer.cmosImageBuffer?.get(4)}/${renderer.cmosImageBuffer?.get(5)}" +
//                "\n${renderer.cmosImageBuffer?.get(6)}/${renderer.cmosImageBuffer?.get(7)}/${renderer.cmosImageBuffer?.get(8)}")
        return true
    }
}