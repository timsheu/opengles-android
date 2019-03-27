package com.nuvoton.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.ViewManager
import org.jetbrains.anko.custom.ankoView

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer : MyGLRenderer
    init {
        setEGLContextClientVersion(3)
        renderer = MyGLRenderer()
        setRenderer(renderer)
        // prevents the GLSurfaceView frame from being redrawn until you call requestRender(), which is more efficient for this sample app.
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
//        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}