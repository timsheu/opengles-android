package com.nuvoton.thermalviewergl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewManager
import android.view.WindowManager
import android.widget.TextView
import com.nuvoton.thermalviewergl.utility.StrokedTextView
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
    var temperatureText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ViewUI().setContentView(this)
    }

    override fun onStart() {
        super.onStart()
        NuUSBHandler.shared.context = this
        myGLSurfaceView?.rxTemp?.observable?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))?.subscribe({
            var value = it.toFloat()
            value /= 10
            temperatureText?.text = resources.getString(R.string.string_temperature, value.toString())
        }, {
            it.printStackTrace()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        NuUSBHandler.shared.unregisterReceiver()
        myGLSurfaceView?.renderer?.cmosImageBuffer?.clear()
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
                    onClick {
                        val cmosOnOff = owner.myGLSurfaceView?.renderer?.cmosOnOff
                        owner.myGLSurfaceView?.renderer?.cmosOnOff = if(cmosOnOff == 0) 1 else 0
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToStart = view.id
                    bottomToBottom = view.id
                    margin = 8
                }

                val thermal = button("Thermal") {
                    id = View.generateViewId()
                    onClick {
                        val thermalOnOff = owner.myGLSurfaceView?.renderer?.thermalOnOff
                        owner.myGLSurfaceView?.renderer?.thermalOnOff = if(thermalOnOff == 0) 1 else 0
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    endToEnd = view.id
                    bottomToBottom = view.id
                    margin = 8
                }

                val bulk = button("Bulk") {
                    id = View.generateViewId()
                    onClick {
                        NuUSBHandler.shared.isStart = true
//                        thread {
                            NuUSBHandler.shared.triggerReadBulk()
//                        }
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToStart = view.id
                    topToTop = view.id
                    margin = 8
                }

                val queue = button("UsbRequest") {
                    id = View.generateViewId()
                    onClick {
                        Observable.interval(0, 100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io())
                            .takeWhile {
                                NuUSBHandler.shared.isStart
                            }
                            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(owner))).subscribe {
                                val result = NuUSBHandler.shared.triggerReadUsbRequest().copyOf()
                                owner.myGLSurfaceView?.imageUpdateSubject?.onNext(result)
                            }
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToEnd = bulk.id
                    topToTop = view.id
                    margin = 8
                }

                val temperature = strokedTextView {
                    text = "Temperature"
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

inline fun ViewManager.strokedTextView(init: StrokedTextView.() -> Unit = {}) : StrokedTextView {
    return ankoView({ StrokedTextView(it) }, theme = 0, init = init)
}
