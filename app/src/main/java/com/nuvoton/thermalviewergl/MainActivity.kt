package com.nuvoton.thermalviewergl

import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.ViewManager
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
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
    var once: Button? = null
    var debug: TextView? = null
    var startButton: Button? = null

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
        val dev : UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        if (dev != null) {
            NuUSBHandler.shared.mDevice = dev
            NuUSBHandler.shared.setupDevice()
        }
        NuUSBHandler.shared.rxTemp.observable.observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this))).subscribe({
            var value = it.toFloat()
            value /= 10
            temperatureText?.text = resources.getString(R.string.string_temperature_degree, value.toString())
        }, {
                it.printStackTrace()
                toast("once e=${it.message}")
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        NuUSBHandler.shared.unregisterReceiver()
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
                    textOn = resources.getString(R.string.string_cmos)
                    textOff = resources.getString(R.string.string_cmos)
                    onClick {
                        owner.myGLSurfaceView?.renderer?.cmosOnOff = if(isChecked) 1
                        else {
                            owner.runOnUiThread { text = resources.getString(R.string.string_cmos) }
                            0
                        }
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
                    textOn = resources.getString(R.string.string_thermal)
                    textOff = resources.getString(R.string.string_thermal)
                    onClick {
                        owner.myGLSurfaceView?.renderer?.thermalOnOff = if(isChecked) 1
                        else {
                            owner.runOnUiThread { text = resources.getString(R.string.string_thermal) }
                            0
                        }
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    endToEnd = view.id
                    bottomToBottom = view.id
                    margin = 8
                }

                val start = toggleButton {
                    isEnabled = false
                    owner.startButton = this
                    id = View.generateViewId()
                    text = resources.getString((R.string.string_get_image))
                    textOn = resources.getString((R.string.string_get_image))
                    textOff = resources.getString((R.string.string_get_image))
                    NuUSBHandler.shared.isReady.observable.observeOn(AndroidSchedulers.mainThread())
                        .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(owner)))
                        .subscribe({
                            this.isEnabled = it
                        }, {
                            it.printStackTrace()
                            toast("once e=${it.message}")
                        })
                    onClick {
                        if (isChecked) {
                            owner.once?.isEnabled = false
                            NuUSBHandler.shared.isStart = true
                            NuUSBHandler.shared.triggerReadUsbRequest()
                            Thread.sleep(100)
                            thread {
                                while(NuUSBHandler.shared.isStart) {
                                    Thread.sleep(20)
                                    view.requestRender()
                                }
//                                owner.runOnUiThread { isChecked = false }
                            }
                        }else {
                            NuUSBHandler.shared.isStart = false
                            owner.once?.isEnabled = true
                        }
                    }
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToStart = view.id
                    topToTop = view.id
                    margin = 8
                }

                val once = button("GetOnce") {
                    isVisible = false
                    isEnabled = false
                    owner.once = this
                    NuUSBHandler.shared.isReady.observable.observeOn(AndroidSchedulers.mainThread())
                        .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(owner)))
                        .subscribe({
                            this.isEnabled = it
                        }, {
                            it.printStackTrace()
                            toast("once e=${it.message}")
                        })
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

                textView {
                    id = View.generateViewId()
                    owner.debug = this
                    backgroundColor = Color.WHITE
                    movementMethod = ScrollingMovementMethod()
                }.lparams(width = wrapContent, height = wrapContent) {
                    startToStart = view.id
                    endToEnd = view.id
                    topToTop = view.id
                    bottomToBottom = view.id
                    marginStart = 50
                    marginEnd = 50
                    elevation = 1000f
                }
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
