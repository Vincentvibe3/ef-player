package com.github.Vincentvibe3.efplayer.formats.webm.legacy

import com.github.Vincentvibe3.efplayer.formats.webm.legacy.WebmDocument.Companion.checkIsEBML
import com.github.Vincentvibe3.efplayer.formats.webm.legacy.WebmDocument.Companion.readVINTData
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.properties.Delegates

/**
 * @suppress
 *
 */
class EBMLHeader {

    companion object {
        const val EBMLVERSION = 0x4286
        const val EBMLREADVERSION = 0x42F7
        const val EBMLMAXIDLENGTH = 0x42F2
        const val EBMLMAXSIZELENGTH = 0x42F3
        const val DOCTYPE = 0x4282
        const val DOCTYPEVERSION = 0x4287
        const val DOCTYPEREADVERSION = 0x4285
        const val maxElementCount = 7
    }

    var version by Delegates.notNull<Int>()
    var readVersion by Delegates.notNull<Int>()
    var maxIdLength by Delegates.notNull<Long>()
    var maxSizeLength by Delegates.notNull<Long>()
    lateinit var docType: String
    var docTypeVer by Delegates.notNull<Int>()
    var docTypeReadVer by Delegates.notNull<Int>()

    private fun readEBMLReadver(input: InputStream): Long {
        val dataSize = readVINTData(input)
        val data = input.readNBytes(dataSize.value.toInt())
        val padding = Int.SIZE_BYTES-data.size
        val int = ByteArray(Int.SIZE_BYTES)
        System.arraycopy(data, 0, int, padding, data.size)
        readVersion = ByteBuffer.wrap(int).int
        return dataSize.bytesRead+dataSize.value
    }

    private fun readEBMLMaxIdLength(input: InputStream): Long {
        val dataSize = readVINTData(input)
        val data = input.readNBytes(dataSize.value.toInt())
        val padding = Long.SIZE_BYTES-data.size
        val long = ByteArray(Long.SIZE_BYTES)
        System.arraycopy(data, 0, long, padding, data.size)
        maxIdLength = ByteBuffer.wrap(long).long
        return dataSize.bytesRead+dataSize.value
    }

    private fun readEBMLver(input: InputStream): Long {
        val dataSize = readVINTData(input)
        val data = input.readNBytes(dataSize.value.toInt())
        val padding = Int.SIZE_BYTES-data.size
        val int = ByteArray(Int.SIZE_BYTES)
        System.arraycopy(data, 0, int, padding, data.size)
        version = ByteBuffer.wrap(int).int
        return dataSize.bytesRead+dataSize.value
    }

    private fun readEBMLMaxSizeLength(input: InputStream): Long {
        val dataSize = readVINTData(input)
        val data = input.readNBytes(dataSize.value.toInt())
        val padding = Long.SIZE_BYTES-data.size
        val long = ByteArray(Long.SIZE_BYTES)
        System.arraycopy(data, 0, long, padding, data.size)
        maxSizeLength = ByteBuffer.wrap(long).long
        return dataSize.bytesRead+dataSize.value
    }

    private fun readDocType(input: InputStream): Long {
        val dataSize = readVINTData(input)
        val data = input.readNBytes(dataSize.value.toInt())
        docType = String(data)
        return dataSize.bytesRead+dataSize.value
    }

    private fun readDocTypeVer(input: InputStream): Long {
        val dataSize = readVINTData(input)
        val data = input.readNBytes(dataSize.value.toInt())
        val padding = Int.SIZE_BYTES-data.size
        val int = ByteArray(Int.SIZE_BYTES)
        System.arraycopy(data, 0, int, padding, data.size)
        docTypeVer = ByteBuffer.wrap(int).int
        return dataSize.bytesRead+dataSize.value
    }

    private fun readDocTypeReadVer(input: InputStream): Long {
        val dataSize = readVINTData(input)
        val data = input.readNBytes(dataSize.value.toInt())
        val padding = Int.SIZE_BYTES-data.size
        val int = ByteArray(Int.SIZE_BYTES)
        System.arraycopy(data, 0, int, padding, data.size)
        docTypeReadVer = ByteBuffer.wrap(int).int
        return dataSize.bytesRead+dataSize.value
    }

    fun parseHeader(input: InputStream){
        if (checkIsEBML(input)){
            val headerSize = readVINTData(input)
            var leftToRead = headerSize.value
            for (_i in 0 until maxElementCount){
                val idBytes = input.readNBytes(2)
                leftToRead -= 2
                val id = ByteBuffer.wrap(byteArrayOf(0, 0, idBytes[0], idBytes[1])).int
                val bytesRead = when (id){
                    EBMLVERSION -> {
                        readEBMLver(input)
                    }
                    EBMLREADVERSION -> {
                        readEBMLReadver(input)
                    }
                    EBMLMAXIDLENGTH -> {
                        readEBMLMaxIdLength(input)
                    }
                    EBMLMAXSIZELENGTH -> {
                        readEBMLMaxSizeLength(input)
                    }
                    DOCTYPE -> {
                        readDocType(input)
                    }
                    DOCTYPEVERSION -> {
                        readDocTypeVer(input)
                    }
                    DOCTYPEREADVERSION -> {
                        readDocTypeReadVer(input)
                    }
                    else -> {throw InvalidIdException()
                    }
                }
                leftToRead-=bytesRead
                if (leftToRead==0L){
                    break
                }
            }
        }
    }

}