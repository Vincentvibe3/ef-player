package com.github.Vincentvibe3.efplayer.formats.webm.streaming

import com.github.Vincentvibe3.efplayer.core.MissingDataException
import com.github.Vincentvibe3.efplayer.core.Result
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.formats.Format

import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import kotlin.experimental.and

class WebmReader(val track: Track):Format {

    companion object {

        enum class IDS(val id: Int) {
            SEGMENT(0x18538067),
            SEEKHEAD(0x114D9B74),
            INFO( 0x1549a966),
            TRACKS( 0x1654ae6b),
            CHAPTERS(0x1043A770),
            CLUSTER( 0x1f43b675),
            CUES( 0x1c53bb6b),
            ATTACHMENTS( 0x1941A469),
            TAGS( 0x1254C367)
        }

        fun checkIsEBML(bytes:ByteArray):Boolean{
            val ebmlId = 0x1A45DFA3
            val bytesInt = ByteBuffer.wrap(bytes).int
            return ebmlId == bytesInt
        }

    }

    private enum class STEPS {
        CHECK_EBML, GET_ID, GET_SIZE, CHECK_SEGMENT, HEADER, SEEKHEAD, INFO, TRACKS, CHAPTERS, CLUSTER, CUES, ATTACHMENTS, TAGS, SKIP
    }

    // For testing only
    fun checkAtCluster():Boolean{
        return currentBlock == STEPS.CLUSTER
    }

    private var currentBlock = STEPS.SKIP
    private var currentStep = STEPS.GET_SIZE
    private var currentBlockLeft = -1L
    private var headerParsed = false
    private val conversionByteBuffer: ByteBuffer = ByteBuffer.allocate(8)

    override val MINIMUM_BYTES_NEEDED = 8L

    suspend fun readVINTData(source:LinkedBlockingDeque<Byte>): Result<Long> {
        val firstByte = source.removeFirst()
        val vintWidth = firstByte.countLeadingZeroBits() + 1
        val remainingBytes =  source.read(vintWidth - 1)
        val longPadding = Long.SIZE_BYTES-(remainingBytes.size + 1)
        val vint = ByteArray(Long.SIZE_BYTES)
        val bitset = BitSet()
        if (vintWidth>8){
            println("problem")
        }
        bitset.set(0, Byte.SIZE_BITS - vintWidth)
        val filter = bitset.toByteArray()
        if (filter.isEmpty()) {
            vint[longPadding] = 0
        } else {
            vint[longPadding] = firstByte.and(filter[0])
        }
        System.arraycopy(remainingBytes, 0, vint, longPadding+1, remainingBytes.size)
        conversionByteBuffer.clear()
        val result = Result(vintWidth.toLong(), conversionByteBuffer.put(vint).flip().long)
        conversionByteBuffer.clear()
        return result
    }

    fun getID(data: LinkedBlockingDeque<Byte>){
        val bytes = data.read(4)
        conversionByteBuffer.clear()
        val id = conversionByteBuffer.put(bytes).flip().int
        conversionByteBuffer.clear()
        println("$id id")
        when (id){
            IDS.CLUSTER.id -> {
                currentBlock = STEPS.CLUSTER
            }
            IDS.SEGMENT.id -> {
                currentBlock = STEPS.CHECK_SEGMENT
            }
            else -> {
                currentBlock = STEPS.SKIP
            }
        }
        currentStep = STEPS.GET_SIZE
    }

    private suspend fun getSize(data: LinkedBlockingDeque<Byte>){
        val result = readVINTData(data)
        if (currentBlock == STEPS.CHECK_SEGMENT){
            currentBlockLeft = -1
        } else {
            currentBlockLeft = result.value
        }
        currentStep = currentBlock
    }

    suspend fun readCluster(data: LinkedBlockingDeque<Byte>){
        var timestamp: Int? = null
        if (data.size<9){
            throw MissingDataException()
        }
        while (currentBlockLeft>0 && data.size>=9){
//            println("data $data")
//            println(data.size>9)
            val idBytes = data.read(1)
            conversionByteBuffer.clear()
            val id = conversionByteBuffer.put(byteArrayOf(0, 0, 0, idBytes[0])).flip().int
            conversionByteBuffer.clear()
            val temp = data.read(8)
            temp.reverse()
            temp.forEach {
                data.putFirst(it)
            }
//            println(data)
            val elementSize = readVINTData(data)
            if (elementSize.value>data.size){
                val discard = 8-elementSize.bytesRead
                for (index in discard until 8){
                    data.putFirst(temp[index.toInt()])
                }
                data.putFirst(idBytes[0])
//                println(data)
                throw MissingDataException()
            }
            currentBlockLeft -= 1
            val bytesRead = when (id){
                0xe7 -> {
                    println("at timestamp")
                    val int = ByteArray(4)
                    val timeStampData = data.read(elementSize.value.toInt())
                    val padding = Int.SIZE_BYTES-timeStampData.size
                    System.arraycopy(timeStampData, 0, int, padding, timeStampData.size)
                    conversionByteBuffer.clear()
                    timestamp = conversionByteBuffer.put(int).flip().int
                    conversionByteBuffer.clear()
                    println("$timestamp cluster ts")
                    elementSize.value+elementSize.bytesRead
                }
                0xa0 -> {
                    data.skip(elementSize.value.toInt())
                    elementSize.value+elementSize.bytesRead
                }
                0xa3 -> {
                    println("found data")
                    val audioData = data.read(elementSize.value.toInt())
                    val tracknumber = audioData[0]
                    conversionByteBuffer.clear()
                    val dataTimestamp = conversionByteBuffer.put(byteArrayOf(audioData[1], audioData[2])).flip().short
                    conversionByteBuffer.clear()
                    val flags = audioData[3]
                    val keyframe = flags.and(0x80.toByte())
                    val lacing = flags.and(0x06).rotateRight(1)
                    println("${lacing.toUInt()} lacing")
                    println(dataTimestamp)
                    val opus = ByteArray(audioData.size-4)
                    System.arraycopy(audioData, 4, opus, 0, opus.size)
                    if (timestamp != null) {
                        val chunk = (timestamp+dataTimestamp)/20
                        track.trackChunks.put(opus)
                    }
                    elementSize.value+elementSize.bytesRead
//                    throw InvalidIdException()
                }
                else -> {
                    println("error")
                    println(data)
                    elementSize.value+elementSize.bytesRead
                }
            }
            currentBlockLeft-=bytesRead
//            if(currentBlockLeft==0L){
//                println("cluster done")
//            }
        }
        if (currentBlockLeft==0L){
            currentBlockLeft = -1
            currentStep = STEPS.GET_ID
        }
    }

    fun skip(data: LinkedBlockingDeque<Byte>){
        println("skipping")
        data.skip(currentBlockLeft.toInt())
        currentStep = STEPS.GET_ID
    }

    suspend fun checkSegment(data: LinkedBlockingDeque<Byte>){
        println("found segment")
        currentStep = STEPS.GET_ID
    }

    override suspend fun processNextBlock(data: LinkedBlockingDeque<Byte>) {
        println("processing")

        while (data.size>MINIMUM_BYTES_NEEDED){
            println(currentBlockLeft)
            println("${data.size} size")
            println(currentStep)
            when (currentStep){
                STEPS.GET_ID -> {
                    println("getting ID")
                    getID(data)
                }
                STEPS.GET_SIZE -> {
                    println("getting size")
                    getSize(data)
                }
                STEPS.CHECK_SEGMENT -> {checkSegment(data)}
                STEPS.HEADER -> {}
                STEPS.SEEKHEAD -> {}
                STEPS.INFO -> {}
                STEPS.TRACKS -> {}
                STEPS.CHAPTERS -> {}
                STEPS.CLUSTER -> {
                    try{
                        readCluster(data)
                    } catch (e:MissingDataException) {
                        break
                    }
                }
                STEPS.CUES -> {}
                STEPS.ATTACHMENTS -> {}
                STEPS.TAGS -> {}
                STEPS.SKIP -> {
                    skip(data)
                }
            }
        }
    }


}