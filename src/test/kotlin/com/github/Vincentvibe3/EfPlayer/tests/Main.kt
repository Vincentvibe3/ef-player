package com.github.Vincentvibe3.EfPlayer.tests

import com.github.Vincentvibe3.EfPlayer.core.Stream
import com.github.Vincentvibe3.EfPlayer.formats.webm.EBMLHeader
import com.github.Vincentvibe3.EfPlayer.formats.webm.Main
import com.github.Vincentvibe3.EfPlayer.formats.webm.WebmDocument
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class Main {

    @Test
    fun isEBML(){
        println("\nChecking EBML file...")
        val input = Files.newInputStream(Path.of("./src/test/resources/file.webm"))
        val isEBML = Main().checkIsEBML(input)
        println("Result: $isEBML")
        assert(isEBML)
    }

    @Test
    fun isNotEBML(){
        println("\nChecking non EBML file...")
        val input = Files.newInputStream(Path.of("./src/test/resources/nonEBML.txt"))
        val isEBML = Main().checkIsEBML(input)
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
            val long = Main().readVINTData(input)
            println("VINT values(decoded|true value): $long | ${values[i]}")
            assert(long==values[i])
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

//    @Test
//    fun streamData(){
//        runBlocking {
//            Stream().startStreaming("https://rr3---sn-ux3n588t-mjh6.googlevideo.com/videoplayback?expire=1641166075&ei=m-DRYbqGN7fnhwb6lq2YDQ&ip=192.0.226.2&id=o-AFyGp-3q5EuvEpXZTDvXY6n0Z6fPp3SOOZyLNMleUx2s&itag=249&source=youtube&requiressl=yes&mh=8m&mm=31%2C29&mn=sn-ux3n588t-mjh6%2Csn-t0a7sn7d&ms=au%2Crdu&mv=m&mvi=3&pcm2cms=yes&pl=21&initcwndbps=770000&vprv=1&mime=audio%2Fwebm&gir=yes&clen=1345358&dur=202.221&lmt=1639320067127142&mt=1641144065&fvip=3&keepalive=yes&fexp=24001373%2C24007246&c=ANDROID&txp=5531432&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cvprv%2Cmime%2Cgir%2Cclen%2Cdur%2Clmt&sig=AOq0QJ8wRQIgXDNo51Jaap3IO5mdFgrDfh8Osk42Uy_xSRWLTnTrzBACIQDWeIT_IV6jEdggDHGArdyXZ48p7fPXGipmjTln5ATvhw%3D%3D&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpcm2cms%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRQIhAOQGeUZ4QGCteMpTwEQQQZlMSIvU51OVKSZ7dIW0iBDpAiAeia971iHKVZxRph2Ytpz5OpplALwjgniN0W-vIGrRbw%3D%3D")
//        }
//
//    }

}