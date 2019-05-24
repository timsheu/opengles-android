package com.nuvoton.thermalviewergl

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewManager
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.nuvoton.thermalviewergl.utility.StrokedTextView
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.constraintLayout

import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk27.coroutines.onClick
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    var myGLSurfaceView: MyGLSurfaceView? = null
    var temperatureText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        ViewUI().setContentView(this)
        if (defaultSharedPreferences.getBoolean("first", true)) {
            longToast(resources.getString(R.string.string_first_start))
            defaultSharedPreferences.edit().putBoolean("first", false).apply()
        }
    }

    override fun onStart() {
        super.onStart()
        NuUSBHandler.shared.context = this
        NuUSBHandler.shared.rxTemp.observable.observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this))).subscribe({
            var value = it.toFloat()
            value /= 10
            temperatureText?.text = resources.getString(R.string.string_temperature_degree, value.toString())
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
                toggleButton {
                    id = View.generateViewId()
                    isChecked = owner.myGLSurfaceView?.renderer?.cmosOnOff != 0
                    text = resources.getString(R.string.string_cmos)
                    onClick {
                        text = resources.getString(R.string.string_cmos)
                        owner.myGLSurfaceView?.renderer?.cmosOnOff = if (owner.myGLSurfaceView?.renderer?.cmosOnOff == 0 ) 1 else 0
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToStart = view.id
                    bottomToBottom = view.id
                    margin = 8
                }

                toggleButton {
                    id = View.generateViewId()
                    isChecked = owner.myGLSurfaceView?.renderer?.thermalOnOff != 0
                    text = resources.getString(R.string.string_thermal)
                    onClick {
                        text = resources.getString(R.string.string_thermal)
                        owner.myGLSurfaceView?.renderer?.thermalOnOff = if (owner.myGLSurfaceView?.renderer?.thermalOnOff == 0 ) 1 else 0
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    endToEnd = view.id
                    bottomToBottom = view.id
                    margin = 8
                }

                val start = toggleButton {
                    id = View.generateViewId()
                    text = resources.getString((R.string.string_get_image))
                    onClick {
                        text = resources.getString((R.string.string_get_image))
                        if (!isChecked) {
                            NuUSBHandler.shared.triggerReadUsbRequest()
                            thread {
                                while(NuUSBHandler.shared.isStart.value) {
//                                view.renderer.cmosImageBuffer = NuUSBHandler.shared.cmosBuffers.getReadBuffer()
                                    view.requestRender()
                                    Thread.sleep(10)
                                }
                            }
                        }else {
                            NuUSBHandler.shared.isStart.value = false
                        }
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToStart = view.id
                    topToTop = view.id
                    margin = 8
                }

                val once = button("GetOnce") {
                    id = View.generateViewId()
                    onClick {
                        thread {
                            NuUSBHandler.shared.getOnce()
                            Thread.sleep(200)
                            view.requestRender()
                        }
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToEnd = start.id
                    topToTop = view.id
                    margin = 8
                }

                val temperature = strokedTextView {
                    text = resources.getString(R.string.string_temperature_default)
                    id = View.generateViewId()
                }.lparams(width = wrapContent, height = wrapContent) {
                    endToEnd = view.id
                    topToTop = view.id
                    margin = 8
                    marginEnd = 16
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
