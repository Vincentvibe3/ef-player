package com.github.Vincentvibe3.efplayer.formats.webm.streaming

import com.github.Vincentvibe3.efplayer.formats.Result
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.formats.Format
import java.lang.RuntimeException

import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import kotlin.experimental.and

class WebmReader(private val track: Track):Format {

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
    private var missingData = false
    private val conversionByteBuffer: ByteBuffer = ByteBuffer.allocate(8)
    override val MINIMUM_BYTES_NEEDED = 8L

    fun readVINTData(source:LinkedBlockingDeque<Byte>): Result<Long> {
        val firstByte = source.removeFirst()
        val vintWidth = firstByte.countLeadingZeroBits() + 1
        val remainingBytes =  source.read(vintWidth - 1)
        val longPadding = Long.SIZE_BYTES-(remainingBytes.size + 1)
        val vint = ByteArray(Long.SIZE_BYTES)
        val bitset = BitSet()
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
        currentBlock = when (id){
            IDS.CLUSTER.id -> {
                STEPS.CLUSTER
            }
            IDS.SEGMENT.id -> {
                STEPS.CHECK_SEGMENT
            }
            else -> {
                STEPS.SKIP
            }
        }
        currentStep = STEPS.GET_SIZE
    }

    private fun getSize(data: LinkedBlockingDeque<Byte>){
        val result = readVINTData(data)
        currentBlockLeft = if (currentBlock == STEPS.CHECK_SEGMENT){
            -1
        } else {
            result.value
        }
        currentStep = currentBlock
    }

    private fun readCluster(data: LinkedBlockingDeque<Byte>){
        if (data.size<9){
            missingData = true
            return
        }
        while (currentBlockLeft>0 && data.size>=9){
            val idBytes = data.read(1)
            conversionByteBuffer.clear()
            val id = conversionByteBuffer.put(byteArrayOf(0, 0, 0, idBytes[0])).flip().int
            conversionByteBuffer.clear()
            val temp = data.read(8)
            temp.reverse()
            temp.forEach {
                data.putFirst(it)
            }
            val elementSize = readVINTData(data)
            if (elementSize.value>data.size){
                val discard = 8-elementSize.bytesRead
                for (index in discard until 8){
                    data.putFirst(temp[index.toInt()])
                }
                data.putFirst(idBytes[0])
                missingData = true
                break
            }
            currentBlockLeft -= 1
            val bytesRead = when (id){
                0xe7 -> {
                    val int = ByteArray(4)
                    val timeStampData = data.read(elementSize.value.toInt())
                    elementSize.value+elementSize.bytesRead
                }
                0xa0 -> {
                    data.skip(elementSize.value.toInt())
                    elementSize.value+elementSize.bytesRead
                }
                0xa3 -> {
                    val audioData = data.read(elementSize.value.toInt())
                    val tracknumber = audioData[0]
                    conversionByteBuffer.clear()
                    val dataTimestamp = conversionByteBuffer.put(byteArrayOf(audioData[1], audioData[2])).flip().short
                    conversionByteBuffer.clear()
                    val flags = audioData[3]
                    val keyframe = flags.and(0x80.toByte())
                    val lacing = flags.and(0x06).rotateRight(1)
                    val opus = ByteArray(audioData.size-4)
                    System.arraycopy(audioData, 4, opus, 0, opus.size)
                    track.trackChunks.put(opus)
                    elementSize.value+elementSize.bytesRead
                }
                else -> {
                    throw RuntimeException("Found invalid block in cluster")
                }
            }
            currentBlockLeft-=bytesRead
        }
        if (currentBlockLeft==0L){
            currentBlockLeft = -1
            currentStep = STEPS.GET_ID
        }
    }

    private fun skip(data: LinkedBlockingDeque<Byte>){
        if (currentBlockLeft>data.size){
            currentBlockLeft-=data.size
            data.skip(data.size)
            missingData = true
        } else {
            data.skip(currentBlockLeft.toInt())
            currentBlockLeft = -1
            currentStep = STEPS.GET_ID
        }
    }

    private fun checkSegment(){
        currentStep = STEPS.GET_ID
    }

    override suspend fun processNextBlock(data: LinkedBlockingDeque<Byte>) {
        while (data.size>MINIMUM_BYTES_NEEDED){
            when (currentStep){
                STEPS.GET_ID -> {
                    getID(data)
                }
                STEPS.GET_SIZE -> {
                    getSize(data)
                }
                STEPS.CHECK_SEGMENT -> {checkSegment()}
                STEPS.HEADER -> {}
                STEPS.SEEKHEAD -> {}
                STEPS.INFO -> {}
                STEPS.TRACKS -> {}
                STEPS.CHAPTERS -> {}
                STEPS.CLUSTER -> {
                    readCluster(data)
                }
                STEPS.CUES -> {}
                STEPS.ATTACHMENTS -> {}
                STEPS.TAGS -> {}
                STEPS.SKIP -> {
                    skip(data)
                }
                STEPS.CHECK_EBML -> {}
            }
            if (missingData){
                missingData = false
                break
            }
        }
    }


}