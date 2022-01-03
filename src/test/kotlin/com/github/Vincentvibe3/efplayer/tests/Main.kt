package com.github.Vincentvibe3.efplayer.tests

import com.github.Vincentvibe3.efplayer.core.Stream
import com.github.Vincentvibe3.efplayer.formats.webm.EBMLHeader
import com.github.Vincentvibe3.efplayer.formats.webm.WebmDocument
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
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

}