package com.nuvoton.opengl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewManager
import android.widget.TextView
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.constraintLayout

import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    var myGLSurfaceView: MyGLSurfaceView? = null
    var temperatureText : TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewUI().setContentView(this)
        Observable.interval(34, TimeUnit.MILLISECONDS)
            .observeOn(Schedulers.computation())
            .map {
                myGLSurfaceView?.updateTestDataContent()
            }
            .subscribeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe({
                myGLSurfaceView?.requestRender()
            }, {
                it.printStackTrace()
            })
    }
}

class ViewUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>): View {
        val test = with(ui) {
            constraintLayout {
                id = View.generateViewId()
                val view = glView {
                    id = View.generateViewId()
                    owner.myGLSurfaceView = this
                }.lparams(width = matchParent, height = matchParent) {
                    margin = 20
                }
                val cmos = button("CMOS") {
                    id = View.generateViewId()
                    onClick { owner.myGLSurfaceView?.renderer?.imageOnOff?.first?.not() }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToStart = view.id
                    bottomToBottom = view.id
                    margin = 8
                }

                val thermal = button("Thermal") {
                    id = View.generateViewId()
                    onClick { owner.myGLSurfaceView?.renderer?.imageOnOff?.second?.not() }
                }.lparams(width = wrapContent, height = wrapContent) {
                    endToEnd = view.id
                    bottomToBottom = view.id
                    margin = 8
                }

                val temperature = textView("TEMPERATURE") {
                    id = View.generateViewId()
                }.lparams(width = wrapContent, height = wrapContent) {
                    endToEnd = view.id
                    topToTop = view.id
                    margin = 8
                }
                owner.temperatureText = temperature
            }
        }
        return test
    }
}

inline fun ViewManager.glView(init: MyGLSurfaceView.() -> Unit = {}) : MyGLSurfaceView {
    return ankoView({ MyGLSurfaceView(it) }, theme = 0, init = init)
}