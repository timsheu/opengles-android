package com.nuvoton.thermalviewergl.utility

import java.nio.ByteBuffer

class ExamineFrame {
    companion object {
        fun checkHeader(byteBuffer: ByteBuffer) : Int {
            var result = 0
            (0 until Constants.header.size).forEach { i ->
                val extract = IntArray(4)
                (0 until Byte.SIZE_BITS).forEach { j ->
                    extract[i] = extract[i] or ((byteBuffer.get((i*8+j)*2 + 1)).toInt().and(0x01).shl(7-j))
                }
                if (Constants.header[i] != extract[i]) {
                    result = 1
                }
            }
            return result
        }

        fun checkFooter(byteBuffer: ByteBuffer) : Int {
            val offset = Constants.usbBufferSize - Constants.footer.size * Byte.SIZE_BITS * 2
            var result = 0
            (0 until Constants.footer.size).forEach { i ->
                val extract = IntArray(4)
                (0 until Byte.SIZE_BITS).forEach { j ->
                    extract[i] = extract[i] or ((byteBuffer.get(offset+(i*8+j)*2 + 1)).toInt().and(0x01).shl(7-j))
                }
                if (Constants.footer[i] != extract[i]) {
                    result = 2
                }
            }
            return result
        }
    }
}