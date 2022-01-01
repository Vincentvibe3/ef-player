package io.github.vincentvibe3.webm

import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

class Main {

    fun checkIsEBML(input: InputStream):Boolean{
        val ebmlId = 0x1A45DFA3
        val firstbytes = input.readNBytes(4)
        val bytesInt = ByteBuffer.wrap(firstbytes).int
        return ebmlId == bytesInt
    }

    fun readEBMLHeader(input: InputStream){
        if (checkIsEBML(input)){
            val headerSize = readVINTData(input)
            println(headerSize)

        }
    }

    fun checkSegmentID(input: InputStream){
        input.readNBytes(4)
    }

    fun readVINTData(input: InputStream): Long {
        val firstByte = input.readNBytes(1)[0]
        val VINTWidth = firstByte.countLeadingZeroBits() + 1
        val remainingBytes = input.readNBytes(VINTWidth - 1)
        val VINT = ByteArray(remainingBytes.size + 1)
        val bitset = BitSet()
        bitset.set(0, Byte.SIZE_BITS - VINTWidth)
        val filter = bitset.toByteArray()
        if (filter.isEmpty()) {
            VINT[0] = 0
        } else {
            VINT[0] = firstByte.and(filter[0])
        }
        System.arraycopy(remainingBytes, 0, VINT, 1, remainingBytes.size)
        val longBytes = ByteArray(8)
        for (i in VINT.indices) {
            val finalIndex = (8 - VINT.size) + i
            longBytes[finalIndex] = VINT[i]
        }
        return ByteBuffer.wrap(longBytes).long
    }

}