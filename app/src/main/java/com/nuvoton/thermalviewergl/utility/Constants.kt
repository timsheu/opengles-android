package com.nuvoton.thermalviewergl.utility

class Constants {
    companion object {
        val header = intArrayOf(0x4E, 0x56, 0x54, 0x20)
        val footer = intArrayOf(0x57, 0x42, 0x5A, 0xA5)
        const val frameWidth = 640
        const val frameHeight = 480
        const val bytePerPixelYUV = 2
        const val frameDataSize= frameWidth * frameHeight * bytePerPixelYUV
        const val usbBufferSize = 8192
        const val packetsCount = frameDataSize / usbBufferSize

        const val thermalFrameWidth = 32
        const val thermalFrameHeight = 32
        const val bytePerPixelRGB = 3
        const val bitsPerThermalData = 26
        const val thermalFrameSize = thermalFrameWidth * thermalFrameHeight * bytePerPixelRGB
        const val thermalDataSize = thermalFrameWidth * thermalFrameHeight * bitsPerThermalData
        const val thermalUnitLength = 13
        const val thermalBytePerBit = 2
    }
}