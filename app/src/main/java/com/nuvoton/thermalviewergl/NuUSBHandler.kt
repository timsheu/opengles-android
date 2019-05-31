package com.nuvoton.thermalviewergl

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import androidx.core.view.isVisible
import com.nuvoton.thermalviewergl.utility.Constants
import com.nuvoton.thermalviewergl.utility.ExamineFrame
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.experimental.and

class NuUSBHandler {
    companion object {
        object Holder { val instance = NuUSBHandler() }
        val shared: NuUSBHandler by lazy { Holder.instance }
        val ACTION_USB_PERMISSION = "com.nuvoton.thermalviewer-gl.USB_PERMISSION"
    }

    var context: Context? = null
    set(value) {
        manager = value!!.getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
        value.registerReceiver(usbReceiver, filter)
        val devices = ArrayList(manager.deviceList.values)
        if (devices.size == 1) {
            mDevice = devices[0]
//            value.toast(mDevice.deviceName)
            val permissionIntent = PendingIntent.getBroadcast(value, 0, Intent(ACTION_USB_PERMISSION), 0)
            manager.requestPermission(mDevice, permissionIntent)
        }
        field = value
    }
    lateinit var manager: UsbManager

    lateinit var mDevice: UsbDevice
    lateinit var mInputEndpoint: UsbEndpoint
    lateinit var mUsbInterface: UsbInterface
    var mUSBConnection: UsbDeviceConnection? = null
    var mQueueReadRequest: UsbRequest? = null
    var rxTemp = RxVar(0)
    var isReady = RxVar(false)

    val wholeFrameBuffer = ByteBuffer.allocateDirect(Constants.frameDataSize)
    val thermalFrameBuffer = ByteBuffer.allocateDirect(Constants.thermalFrameSize)
    var isStart = false
    val cmosBuffers = RingBuffer(3, Constants.frameDataSize)
    val thermalBuffers = RingBuffer(3, Constants.thermalFrameSize)

    private val usbBuffer = ByteBuffer.allocateDirect(Constants.usbBufferSize)

    fun getOnce() {
        var checkHeaderResult = 0
        var test = ""
        test += (context as MainActivity).debug?.text
        thread {
            var t = Date().time
            repeat((0 until Constants.packetsCount).count()) {
                try {
                    if (mQueueReadRequest?.queue(usbBuffer, Constants.usbBufferSize) == null) throw IOException("Error queueing request")
                    mUSBConnection?.requestWait() ?: throw IOException("Null response")
                    when(it) {
                        0 -> {
                            checkHeaderResult += ExamineFrame.checkHeader(usbBuffer)
                            var temp = ""
                            (0 until 64).forEach {
                                temp += "${it.toString(16)} "
                            }
                            test += "header=$temp\n"
                        }
                        Constants.packetsCount - 1  -> {
                            checkHeaderResult += ExamineFrame.checkFooter(usbBuffer)
                            var temp = ""
                            (0 until 64).forEach {
                                temp += "${it.toString(16)} "
                            }
                            test += "tail=$temp\n"
                        }
                    }
                    context?.runOnUiThread { (context as MainActivity).debug?.text = test }
                    usbBuffer.position(0)
                    wholeFrameBuffer.put(usbBuffer)
                    if (checkHeaderResult != 0)  context?.runOnUiThread { toast("result=$checkHeaderResult") }
                    usbBuffer.position(0)
                    checkHeaderResult = 0
                }catch (e: Exception) {
                    e.printStackTrace()
                    closeConnection()
                }
            }
            t = Date().time - t
            test += "\n"
            context?.runOnUiThread { toast("$t ms to get one frame") }
            wholeFrameBuffer.position(0)
            cmosBuffers.writeToBuffer(wholeFrameBuffer)
            rxTemp.value = updateThermalData(wholeFrameBuffer)
            wholeFrameBuffer.position(0)
        }
    }

    fun triggerReadUsbRequest() {
        var checkHeaderResult = 0
        var test = ""
        test += (context as MainActivity).debug?.text
        thread {
            while(isStart) {
                var i=0
                while (i < Constants.packetsCount) {
                    try {
                        if (mQueueReadRequest?.queue(usbBuffer, Constants.usbBufferSize) == null) throw IOException("Error queueing request")
                        mUSBConnection?.requestWait() ?: throw IOException("Null response")
                        if (usbBuffer.position() != usbBuffer.capacity()) {
//                            context?.runOnUiThread { toast("${usbBuffer.position()}/${usbBuffer.limit()}/${usbBuffer.capacity()}")}
                            continue
                        }
                        when(i) {
                            0 -> {
                                checkHeaderResult += ExamineFrame.checkHeader(usbBuffer)
                                var temp = ""
                                (0 until 64).forEach {
                                    temp += "${usbBuffer[it].toInt().and(0xff).toString(16)} "
                                }
                                test += "header=$temp\n"
                                val ints = ExamineFrame.checkHeaderArray(usbBuffer)
                                var temp1 = ""
                                ints.forEach {
                                    temp1 += "${it.and(0xff).toString(16)} "
                                }
                                test += "header raw=$temp1\n"
                            }
                            Constants.packetsCount - 1  -> {
                                checkHeaderResult += ExamineFrame.checkFooter(usbBuffer)
                                var temp = ""
                                (usbBuffer.capacity() - 64 until usbBuffer.capacity()).forEach {
                                    temp += "${usbBuffer[it].toInt().and(0xff).toString(16)} "
                                }
                                test += "tail=$temp\n"
                                val ints = ExamineFrame.checkFooterArray(usbBuffer)
                                var temp1 = ""
                                ints.forEach {
                                    temp1 += "${it.and(0xff).toString(16)} "
                                }
                                test += "footer raw=$temp1\n"
                            }
                        }
                        usbBuffer.position(0)
                        if (checkHeaderResult == 0) wholeFrameBuffer.put(usbBuffer) else isStart = false
                        usbBuffer.position(0)
                        i++
                    }catch (e: Exception) {
                        e.printStackTrace()
                        context?.runOnUiThread { toast("e=${e.message}") }
                        isStart = false
                        break
                    }
                }
                wholeFrameBuffer.position(0)
                cmosBuffers.writeToBuffer(wholeFrameBuffer)
                rxTemp.value = updateThermalData(wholeFrameBuffer)
                wholeFrameBuffer.position(0)
                checkHeaderResult = 0
                Thread.sleep(20) //TODO: make it dynamic
            }
            clearBuffers()
        }
    }

    private fun updateThermalData(byteBuffer: ByteBuffer) : Int {
        val temperatureArray = IntArray(Constants.thermalFrameWidth * Constants.thermalFrameHeight)
        var temperature = 0
        var temp = 0
        var negative = false
        var offset = Constants.header.size * Byte.SIZE_BITS * 2

        byteBuffer.position(0)
        for (i in 0 until Constants.thermalFrameWidth * Constants.thermalFrameHeight) {
            temp = 0
            negative = byteBuffer.get(1+offset).toInt().and(0xff) and 0x01 == 1
            for (index in 3 + offset until Constants.thermalUnitLength * 2 + offset step 2) {
                val shiftIndex = ((Constants.thermalUnitLength * 2 + offset - index) / 2)
                temp = temp or ((byteBuffer.get(index).toInt().and(0xff) and 0x01) shl shiftIndex)
            }
            temperatureArray[i] = if (negative) -temp else temp
            offset += Constants.thermalBytePerBit * Constants.thermalUnitLength
        }
        byteBuffer.position(0)

        transformThermalData(temperatureArray)

        val firstPointOffset = (Constants.thermalFrameWidth)*(Constants.thermalFrameHeight/2-1) + Constants.thermalFrameWidth/2

        temperature += temperatureArray[firstPointOffset]
        temperature += temperatureArray[firstPointOffset+1]
        temperature += temperatureArray[firstPointOffset+(Constants.thermalFrameWidth)]
        temperature += temperatureArray[firstPointOffset+(Constants.thermalFrameWidth)+1]
        temperature /= 4
        return temperature
    }

    private fun transformThermalData(intArray: IntArray) {
        val zeroDegreeOffset = NuColor.COLOR_TABLE_LOWER_SIZE + 600
        intArray.forEachIndexed { index, it ->
            val rgb = NuColor.RGB_ColorTable[zeroDegreeOffset + it]
            thermalFrameBuffer.put(index*Constants.bytePerPixelRGB, rgb.R.toByte())
            thermalFrameBuffer.put(index*Constants.bytePerPixelRGB+1, rgb.G.toByte())
            thermalFrameBuffer.put(index*Constants.bytePerPixelRGB+2, rgb.B.toByte())
        }

        thermalFrameBuffer.position(0)
        thermalBuffers.writeToBuffer(thermalFrameBuffer)
        thermalFrameBuffer.position(0)
    }

    fun unregisterReceiver() {
        context?.unregisterReceiver(usbReceiver)
        closeConnection()
    }

    fun closeConnection() {
        mQueueReadRequest?.close()
        mUSBConnection?.close()
        mUSBConnection?.releaseInterface(mUsbInterface)
        isReady.value = false
    }

    fun clearBuffers() {
        usbBuffer.clear()
        wholeFrameBuffer.clear()
        thermalFrameBuffer.clear()
        thermalBuffers.initBuffers()
        cmosBuffers.initBuffers()
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_USB_PERMISSION == intent?.action) {
                context?.runOnUiThread { toast(resources.getString(R.string.string_connected)) }
                thread {
                    synchronized(this) {
                        if (intent.getBooleanExtra((UsbManager.EXTRA_PERMISSION_GRANTED), false)) {
                            setupDevice()
                        }else {
                            context?.runOnUiThread { toast(resources.getString(R.string.string_not_granted)) }
                        }
                    }
                }
            }else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent?.action) {
                context?.runOnUiThread { toast(resources.getString(R.string.string_disconnected)) }
                synchronized(this) {
                    mDevice?.apply {
                        closeConnection()
                    }
                }
            }
        }
    }

    fun setupDevice() {
        for (i in 0 until mDevice.interfaceCount) {
            val inter = mDevice.getInterface(i)
            for (j in 0 until mDevice.getInterface(i).endpointCount) {
                val endpoint = inter.getEndpoint(j)
                if (endpoint.direction == UsbConstants.USB_DIR_IN &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.maxPacketSize == 512) {
                    mUsbInterface = inter
                    mInputEndpoint = endpoint
//                    context?.runOnUiThread { toast("interface $i/endpoint $j setup") }
                    manager.openDevice(mDevice).apply {
                        mUSBConnection = this
                        mQueueReadRequest = UsbRequest()
                        mQueueReadRequest?.initialize(mUSBConnection, mInputEndpoint)
                        claimInterface(mUsbInterface , true)
                        setInterface(mUsbInterface)
                        isStart = true
                        isReady.value = true
//                        context?.runOnUiThread { toast("after set interface") }
                    }
                }
            }
        }
    }
}