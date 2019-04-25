package com.nuvoton.thermalviewergl

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.util.Log
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

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
            value.toast(mDevice.deviceName)
            val permissionIntent = PendingIntent.getBroadcast(value, 0, Intent(ACTION_USB_PERMISSION), 0)
            manager.requestPermission(mDevice, permissionIntent)
        }
//        (value as MainActivity).isDataUpdated.observable.subscribeOn(AndroidSchedulers.mainThread()).observeOn(Schedulers.io())
//            .filter { it }
//            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(value)))
//            .subscribe({
//                triggerRead()
//            }, {
//                it.printStackTrace()
//            })
        field = value
    }
    lateinit var manager: UsbManager

    lateinit var mDevice: UsbDevice
    lateinit var mInputEndpoint: UsbEndpoint
    lateinit var mUsbInterface: UsbInterface
    var mUSBConnection: UsbDeviceConnection? = null
    var mRequest: UsbRequest? = null
    private val BUFFER_SIZE = 640*480*2

    var imageBuffer = ByteBuffer.allocate(BUFFER_SIZE)
    val imageByteArray = ByteArray(BUFFER_SIZE)
    var isRead = RxVar(false)
    var isStart = false
//    set(value) {
//        if (value) {
//            thread {
//                while (value) {
//                    if (mUSBConnection?.requestWait() == mRequest) {
//                        imageBuffer.put(byteBuffer)
//                        byteBuffer.clear()
//                    }
//                    if (imageBuffer.position() == imageBuffer.capacity()) {
//                        imageBuffer.flip()
//                        imageBuffer.get(imageByteArray)
//                        isRead.value = true
//                        imageBuffer.clear()
//                    }
//                    Thread.sleep(50)
//                }
//            }
//        }
//        field = value
//    }

    var isAppStarted = false

    private val buffer = ByteArray(512)
    private val byteBuffer = ByteBuffer.allocate(8192)
    private val testBuffer = ByteArray(8192)

    fun triggerReadBulk() {
        thread {
//              bulktransfer is too slow
                repeat((0 until 1200).count()) {
                    mUSBConnection?.bulkTransfer(mInputEndpoint, buffer, 512, 0)
                    imageBuffer.put(buffer)
                }
            imageBuffer.flip()
            imageBuffer.get(imageByteArray)
            isRead.value = true
        }
    }

    fun triggerReadUsbRequest() : ByteArray {
        repeat((0 until 75).count()) {
            try {
                if (mRequest?.queue(byteBuffer, 8192) == null) throw IOException("Error queueing request")
                mUSBConnection?.requestWait() ?: throw IOException("Null response")
                byteBuffer.flip()
                imageBuffer.put(byteBuffer)
                byteBuffer.clear()
            }catch (e: Exception) {
                e.printStackTrace()
                closeConnection()
            }
        }
        imageBuffer.position(0)
        imageBuffer.get(imageByteArray)
//        if (imageBuffer.limit() == imageByteArray.size) imageBuffer.get(imageByteArray) else imageBuffer.limit(imageBuffer.capacity())
        imageBuffer.position(0)
        return imageByteArray
    }

    private fun changeEndian32bits(byteArray: ByteArray) : ByteArray {
        val returnArray = ByteArray(byteArray.size)
        for (i in 0 until byteArray.size step 4) {
            returnArray[i+0] = byteArray[i+3]
            returnArray[i+1] = byteArray[i+2]
            returnArray[i+2] = byteArray[i+1]
            returnArray[i+3] = byteArray[i+0]
        }
        return returnArray
    }

    fun unregisterReceiver() {
        context?.unregisterReceiver(usbReceiver)
        closeConnection()
    }

    fun closeConnection() {
        mRequest?.close()
        mUSBConnection?.close()
        mUSBConnection?.releaseInterface(mUsbInterface)
        isStart = false
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
//            context?.runOnUiThread { toast(intent?.action?.toString() ?: "null") }
            if (ACTION_USB_PERMISSION  == intent?.action) {
                context?.runOnUiThread { toast(resources.getString(R.string.string_connected)) }
                synchronized(this) {
                    if (intent.getBooleanExtra((UsbManager.EXTRA_PERMISSION_GRANTED), false)) {
                        thread {
                            for (i in 0 until mDevice.interfaceCount) {
                                val inter = mDevice.getInterface(i)
                                for (j in 0 until mDevice.getInterface(i).endpointCount) {
                                    val endpoint = inter.getEndpoint(j)
                                    if (endpoint.direction == UsbConstants.USB_DIR_IN &&
                                        endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                                            endpoint.maxPacketSize == 512) {
                                        mUsbInterface = inter
                                        mInputEndpoint = endpoint
//                                        context?.runOnUiThread { toast("interface $i/endpoint $j setup") }
                                        manager.openDevice(mDevice).apply {
                                            mUSBConnection = this
                                            mRequest = UsbRequest()
                                            mRequest?.initialize(mUSBConnection, mInputEndpoint)
                                            claimInterface(mUsbInterface , true)
                                            setInterface(mUsbInterface)
                                            isStart = true
//                                            context?.runOnUiThread { toast("after set interface") }
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }else {
                        context?.runOnUiThread { toast(resources.getString(R.string.string_not_granted)) }
                        false
                    }
                }
            }else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent?.action) {
                context?.runOnUiThread { toast(resources.getString(R.string.string_disconnected)) }
                synchronized(this) {
                    isStart = false
                    mDevice.apply {
                        closeConnection()
                    }
                    true
                }
            }
        }
    }
}