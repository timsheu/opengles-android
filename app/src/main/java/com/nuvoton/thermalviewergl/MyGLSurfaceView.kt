package com.nuvoton.thermalviewergl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer : MyGLRenderer
    var value = 0

    val header = intArrayOf(0x4E, 0x56, 0x54, 0x20)
    val footer = intArrayOf(0x57, 0x42, 0x5A, 0xA5)

    val THERMAL_UNIT_LENGTH = 13
    val THERMAL_BYTES_PER_BIT = 2
    val THERMAL_WIDTH = 32
    val THERMAL_HEIGHT = 32
    val THERMAL_DATA_LENGTH = THERMAL_WIDTH * THERMAL_HEIGHT
    val THERMAL_HEADER_LENGTH = 4

    val CMOS_DATA_LENGTH = 640 * 480 * 2 // VGA in YUYV
    val imageByteData = ByteArray(CMOS_DATA_LENGTH)
    val imageUpdateSubject = PublishSubject.create<ByteArray>().toSerialized()
    val rxTemp = RxVar(0)

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context)

        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        imageUpdateSubject.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .map {
                if(checkHeaderAndFooter(it)) {
                    updateDataContent(it)
                    updateThermalContent(it)
                } else -9999
            }
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from((context as MainActivity))))
            .subscribe({
                requestRender()

                rxTemp.value = it
//                Log.d(this.javaClass.simpleName, "rxTemp updated")
        }, {
                it.printStackTrace()
        })
    }

    fun checkHeaderAndFooter(byteArray: ByteArray) : Boolean {
        var result = true
        (0 until header.size).forEach { i ->
            val extract = IntArray(4)
            (0 until Byte.SIZE_BITS).forEach { j ->
                extract[i] = extract[i] or ((byteArray[(i*8+j)*2 + 1]).toInt().and(0x01).shl(7-j))
            }
            if (header[i] != extract[i]) {
                result = false
            }
        }

        val offset = CMOS_DATA_LENGTH - THERMAL_HEADER_LENGTH * Byte.SIZE_BITS * 2
        (0 until footer.size).forEach { i ->
            val extract = IntArray(4)
            (0 until Byte.SIZE_BITS).forEach { j ->
                extract[i] = extract[i] or ((byteArray[offset+(i*8+j)*2 + 1]).toInt().and(0x01).shl(7-j))
            }
            if (footer[i] != extract[i]) {
                result = false
            }
        }

        return result
    }

    fun updateDataContent(byteArray: ByteArray) {
        renderer.cmosImageBuffer?.clear()
        renderer.cmosImageBuffer?.put(byteArray)
        renderer.cmosImageBuffer?.flip()
    }

    fun updateThermalContent(byteArray: ByteArray) : Int {
        val temperatureArray = IntArray(THERMAL_WIDTH * THERMAL_HEIGHT)
        var temperature = 0
        var temp = 0
        var negative = false
        var offset = header.size * Byte.SIZE_BITS * 2

        for (i in 0 until THERMAL_WIDTH * THERMAL_HEIGHT) {
            temp = 0
            negative = byteArray[1+offset].toInt().and(0xff) and 0x01 == 1
            for (index in 3 + offset until THERMAL_UNIT_LENGTH * 2 + offset step 2) {
                val shiftIndex = ((THERMAL_UNIT_LENGTH * 2 + offset - index) / 2)
                temp = temp or ((byteArray[index].toInt().and(0xff) and 0x01) shl shiftIndex)
            }
            temperatureArray[i] = if (negative) -temp else temp
            offset += THERMAL_BYTES_PER_BIT * THERMAL_UNIT_LENGTH
        }

        renderer.transformThermalData(temperatureArray)

        val firstPointOffset = (THERMAL_WIDTH)*(THERMAL_HEIGHT/2-1) + THERMAL_WIDTH/2

        temperature += temperatureArray[firstPointOffset]
        temperature += temperatureArray[firstPointOffset+1]
        temperature += temperatureArray[firstPointOffset+(THERMAL_WIDTH)]
        temperature += temperatureArray[firstPointOffset+(THERMAL_WIDTH)+1]
        temperature /= 4
        return temperature
    }
}