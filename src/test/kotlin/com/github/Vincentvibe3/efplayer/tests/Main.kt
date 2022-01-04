package com.github.Vincentvibe3.efplayer.tests

import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.Youtube
import com.github.Vincentvibe3.efplayer.formats.webm.EBMLHeader
import com.github.Vincentvibe3.efplayer.formats.webm.WebmDocument
import com.github.Vincentvibe3.efplayer.formats.webm.streaming.WebmReader
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingDeque
import kotlin.test.Test

class Main {

    @Test
    fun isEBML(){
        println("\nChecking EBML file...")
        val input = Files.newInputStream(Path.of("./src/test/resources/file.webm"))
        val isEBML = WebmDocument.checkIsEBML(input)
        println("Result: $isEBML")
        assert(isEBML)
    }

    @Test
    fun isNotEBML(){
        println("\nChecking non EBML file...")
        val input = Files.newInputStream(Path.of("./src/test/resources/nonEBML.txt"))
        val isEBML = WebmDocument.checkIsEBML(input)
        println("Result: $isEBML")
        assert(!isEBML)
    }

    @Test
    fun VINTtoLong(){
        println("\nChecking VINT parser...")
        val values = arrayOf(1L, 64L, 1345310L)
        val VINTs = ArrayList<ArrayList<Byte>>()
        val VINTasHex = arrayOf(
            arrayOf(0x81),
            arrayOf(0xC0),
            arrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x14, 0x87, 0x1e)
        )
        VINTasHex.forEach { vintHex ->
            val bytelist = ArrayList<Byte>()
            vintHex.forEach {
                bytelist.add(it.toByte())
            }
            VINTs.add(bytelist)
        }
        VINTs.forEachIndexed { i, it ->
            val input = ByteArrayInputStream(it.toByteArray())
            val long = WebmDocument.readVINTData(input)
            println("VINT values(decoded|true value): ${long.value} | ${values[i]}")
            assert(long.value==values[i])
        }
    }

    @Test
    fun findCluster(){
        val doc = WebmDocument()
        doc.header = EBMLHeader()
        val input = Files.newInputStream(Path.of("./src/test/resources/file.webm"))
        doc.header.parseHeader(input)
        input.readNBytes(4)
        doc.readSegment(input)
    }

    @Test
    fun readHeader(){
        println("\nReading Header")
        val input = Files.newInputStream(Path.of("./src/test/resources/file.webm"))
        val header = EBMLHeader()
        header.parseHeader(input)
        println("version: ${header.version}")
        println("maxIdLength: ${header.maxIdLength}")
        println("readVersion: ${header.readVersion}")
        println("maxSizeLength: ${header.maxSizeLength}")
        println("docType: ${header.docType}")
        println("docTypeVer: ${header.docTypeVer}")
        println("docTypeReadVer: ${header.docTypeReadVer}")
        assert(header.version==1)
        assert(header.readVersion==1)
        assert(header.maxIdLength==4L)
        assert(header.maxSizeLength==8L)
        assert(header.docType=="webm")
        assert(header.docTypeVer==4)
        assert(header.docTypeReadVer==2)
    }

    @Test
    fun streamingCheckCluster(){
        val data = LinkedBlockingDeque<Byte>()
        data.addAll(byteArrayOf(0x1f.toByte(), 0x43.toByte(), 0xb6.toByte(), 0x75.toByte(), 0x15.toByte(), 0x49.toByte(), 0xa9.toByte(), 0x66.toByte()).toList())
        val stream = WebmReader(Track("", Youtube, "", 1))
        stream.getID(data)
        assert(stream.checkAtCluster())
        stream.getID(data)
        assert(!stream.checkAtCluster())

    }

    @Test
    fun testVINTStreaming(){
        val stream = WebmReader(Track("", Youtube, "", 1))
        println("\nChecking VINT parser...")
        val values = arrayOf(1L, 64L, 1345310L)
        val VINTs = ArrayList<ArrayList<Byte>>()
        val VINTasHex = arrayOf(
            arrayOf(0x81),
            arrayOf(0xC0),
            arrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x14, 0x87, 0x1e)
        )
        VINTasHex.forEach { vintHex ->
            val bytelist = ArrayList<Byte>()
            vintHex.forEach {
                bytelist.add(it.toByte())
            }
            VINTs.add(bytelist)
        }
        VINTs.forEachIndexed { i, it ->
            runBlocking {
                val long = stream.readVINTData(LinkedBlockingDeque(it))
                println("VINT values(decoded|true value): ${long.value} | ${values[i]}")
                assert(long.value==values[i])
            }

        }
    }

}