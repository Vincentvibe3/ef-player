package io.github.vincentvibe3.EfPlayer.formats.webm

import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashMap
import kotlin.experimental.and

class WebmDocument {

    lateinit var header: EBMLHeader;
    val audioData: ByteArray? = null
    var currentChunk:Long = 0

    val trackopuschunks = HashMap<Long, ByteArray?>()

    fun canProvide():Boolean {
        val bytes = trackopuschunks[currentChunk]
        return bytes?.isNotEmpty() ?: false
    }

    fun getAudio(): ByteBuffer? {
        val bytes = trackopuschunks[currentChunk]
        currentChunk++
        return ByteBuffer.wrap(bytes)
    }

    fun genChunks(){
        val length = 222000L/20
        for (i in 0 until length){
            trackopuschunks[i] = null
        }
    }

    fun readSegment(input:InputStream){
        genChunks()
        val segmentSize = readVINTData(input)
        var leftToRead = segmentSize.value
        println(leftToRead)
        while (leftToRead>0L){
            val idBytes = input.readNBytes(4)
            leftToRead -= 4
            val id = ByteBuffer.wrap(idBytes).int
            val bytesRead = when (id){
                IDS.CLUSTER.id -> {
                    println("at cluster")
                    getCluster(input)
                }
                else -> {
                    println("found other")
                    val size = readVINTData(input)
                    input.readNBytes(size.value.toInt())
                    size.value+size.bytesRead
//                    throw InvalidIdException()
                }
            }
            leftToRead-=bytesRead
        }
    }

    fun getCluster(input:InputStream): Long{
        val size = readVINTData(input)
//        println(size.value)
//        input.readNBytes(size.value.toInt())
        var timestamp: Int? = null
        var leftToRead = size.value
        while (leftToRead>0L){
            val idBytes = input.readNBytes(1)
            leftToRead -= 1
            val id = ByteBuffer.wrap(byteArrayOf(0, 0, 0, idBytes[0])).int
            val bytesRead = when (id){
                0xe7 -> {
                    println("at timestamp")
                    val elementSize = readVINTData(input)
                    val int = ByteArray(4)
                    val timeStampData = input.readNBytes(elementSize.value.toInt())
                    val padding = Int.SIZE_BYTES-timeStampData.size
                    System.arraycopy(timeStampData, 0, int, padding, timeStampData.size)
                    timestamp = ByteBuffer.wrap(int).int
                    println("${ByteBuffer.wrap(int).int} cluster ts")
                    elementSize.value+elementSize.bytesRead
                }
                0xa0 -> {
                    val elementSize = readVINTData(input)
                    input.readNBytes(elementSize.value.toInt())
                    elementSize.value+elementSize.bytesRead
                }
                else -> {
                    println("found data")
                    val elementSize = readVINTData(input)
                    val audioData = input.readNBytes(elementSize.value.toInt())
                    val tracknumber = audioData[0]
                    val dataTimestamp = ByteBuffer.wrap(byteArrayOf(audioData[1], audioData[2])).short
                    val flags = audioData[3]
                    val keyframe = flags.and(0x80.toByte())
                    val lacing = flags.and(0x06).rotateRight(1)
                    println("${lacing.toUInt()} lacing")
                    println(dataTimestamp)
                    val opus = ByteArray(audioData.size-4)
                    System.arraycopy(audioData, 4, opus, 0, opus.size)
                    if (timestamp != null) {
                        val chunk = (timestamp+dataTimestamp)/20
                        trackopuschunks[chunk.toLong()] = opus
                    }
                    elementSize.value+elementSize.bytesRead
//                    throw InvalidIdException()
                }
            }
            leftToRead-=bytesRead
        }
        return size.value+size.bytesRead
    }

    fun readSeekHead(input:InputStream){
        val dataSize = readVINTData(input)
        val remainingBytes = dataSize.value
    }

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

        fun checkIsEBML(input: InputStream):Boolean{
            val ebmlId = 0x1A45DFA3
            val firstbytes = input.readNBytes(4)
            val bytesInt = ByteBuffer.wrap(firstbytes).int
            return ebmlId == bytesInt
        }

        fun readVINTData(input: InputStream): VINTData {
            val firstByte = input.readNBytes(1)[0]
            val VINTWidth = firstByte.countLeadingZeroBits() + 1
            val remainingBytes = input.readNBytes(VINTWidth - 1)
            val longPadding = Long.SIZE_BYTES-(remainingBytes.size + 1)
            val VINT = ByteArray(Long.SIZE_BYTES)
            val bitset = BitSet()
            bitset.set(0, Byte.SIZE_BITS - VINTWidth)
            val filter = bitset.toByteArray()
            if (filter.isEmpty()) {
                VINT[longPadding] = 0
            } else {
                VINT[longPadding] = firstByte.and(filter[0])
            }
            System.arraycopy(remainingBytes, 0, VINT, longPadding+1, remainingBytes.size)
            return VINTData(ByteBuffer.wrap(VINT).long, VINTWidth.toLong())
        }

    }

}