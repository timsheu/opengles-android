package com.nuvoton.thermalviewergl

import java.nio.ByteBuffer

class RingBuffer(val bufferSize: Int, val dataSize: Int) {
    private var arr = arrayListOf<ByteBuffer>()
    private var read = 0
    private var write = 0

    init {
        initBuffers()
    }

    fun getReadBuffer() : ByteBuffer? {
        synchronized(this) {
            return if (read == write) null
            else {
                val temp = read
                read = (read + 1) % bufferSize
                arr[temp]
            }
        }
    }

    fun writeToBuffer(input: ByteBuffer) {
        synchronized(this) {
            val buf = arr[write]
            buf.position(0)
            buf.put(input)
            buf.position(0)
            write = (write + 1) % bufferSize
        }
    }

    fun initBuffers() {
        arr.clear()
        repeat((0 until bufferSize).count()) {
            val temp = ByteBuffer.allocateDirect(dataSize)
            arr.add(temp)
        }
    }
}