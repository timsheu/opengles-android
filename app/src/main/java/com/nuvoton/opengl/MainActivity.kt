package com.nuvoton.opengl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewManager
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.constraintLayout
import org.jetbrains.anko.custom.ankoView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TestActivityUI().setContentView(this)
    }
}

class TestActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>): View {
        return with(ui) {
            constraintLayout {

                glView {

                }.lparams(width = matchParent, height = matchParent) {
                    margin = 20
                }
            }
        }
    }
}

inline fun ViewManager.glView(init: MyGLSurfaceView.() -> Unit = {}) : MyGLSurfaceView {
    return ankoView({ MyGLSurfaceView(it) }, theme = 0, init = init)
}