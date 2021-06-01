package resolver

import Config
import utils.*

data class DnsPacket(
    val id: Int,
    val isResponse: Boolean,
    val opcode: Int,
    val authoritativeAnswer: Boolean,
    val truncation: Boolean,
    val recursionDesired: Boolean,
    val recursionAvailable: Boolean,
    val responseCode: Int,
    val questions: List<Question>,
    val answers: List<ResourceRecord>,
    val authorities: List<ResourceRecord>,
    val additionals: List<ResourceRecord>
) {
    data class Question(
        val type: DnsType,
        val dnsClass: DnsClass,
        val name: DnsName
    )

    data class ResourceRecord(
        val name: DnsName,
        val dnsClass: DnsClass,
        val timeToLive: Int,
        val data: RRData
    ) {
        val dnsType = data.type
        val dataBytes = data.bytes
    }


    init {
        require(id <= 0xFFFF)
        require(opcode <= 0xF)
        require(responseCode <= 0xF)
        require(questions.isNotEmpty())
    }

    val bytes: ByteArray by lazy {
        val bytesList = mutableListOf<Byte>()
        bytesList.addAll(id.toTwoBytes())
        val byte2 = BooleanArray(8)
        val byte3 = BooleanArray(8)

        byte2[0] = isResponse

        opcode.toBooleanArray(4)
            .let {
                byte2[1] = it[0]
                byte2[2] = it[1]
                byte2[3] = it[2]
                byte3[4] = it[3]
            }

        byte2[5] = authoritativeAnswer
        byte2[6] = truncation
        byte2[7] = recursionDesired

        byte3[0] = recursionAvailable
        byte3[1] = false
        byte3[2] = false
        byte3[3] = false

        responseCode.toBooleanArray(4)
            .let {
                byte3[4] = it[0]
                byte3[5] = it[1]
                byte3[6] = it[2]
                byte3[7] = it[3]
            }

        bytesList.add(byte2.toByte())
        bytesList.add(byte3.toByte())
        bytesList.addAll(questions.size.toTwoBytes())
        bytesList.addAll(answers.size.toTwoBytes())
        bytesList.addAll(authorities.size.toTwoBytes())
        bytesList.addAll(additionals.size.toTwoBytes())

        questions.forEach {
            bytesList.addAll(ByteUtils.listOfLabelsToDnsNameBytes(it.name.labels).toList())
            bytesList.addAll(it.type.value.toTwoBytes())
            bytesList.addAll(it.dnsClass.value.toTwoBytes())
        }

        fun addResourceRecord(record: ResourceRecord) {
            bytesList.addAll(ByteUtils.listOfLabelsToDnsNameBytes(record.name.labels).toList())
            bytesList.addAll(record.dnsType.value.toTwoBytes())
            bytesList.addAll(record.dnsClass.value.toTwoBytes())
            bytesList.addAll(record.timeToLive.toFourBytes())
            bytesList.addAll(record.dataBytes.size.toTwoBytes())
            bytesList.addAll(record.dataBytes.toList())
        }

        answers.forEach { addResourceRecord(it) }
        authorities.forEach { addResourceRecord(it) }
        additionals.forEach { addResourceRecord(it) }

        bytesList.toByteArray()
    }

    companion object {
        @ExperimentalUnsignedTypes
        fun fromByteArray(array: ByteArray): DnsPacket {
            return packet {
                id = ByteUtils.twoBytesToInt(array[0], array[1])

                val byte2 = array[2].toBooleanArray()
                val byte3 = array[3].toBooleanArray()
                isResponse = byte2[0]
                opcode = byte2.sliceArray(1..4).toInt()
                authoritativeAnswer = byte2[5]
                truncation = byte2[6]
                recursionDesired = byte2[7]
                recursionAvailable = byte3[0]
                responseCode = byte3.sliceArray(4..7).toInt()

                val questionsCount = ByteUtils.twoBytesToInt(array[4], array[5])
                val answersCount = ByteUtils.twoBytesToInt(array[6], array[7])
                val authoritiesCount = ByteUtils.twoBytesToInt(array[8], array[9])
                val additionalsCount = ByteUtils.twoBytesToInt(array[10], array[11])

                var currentByteIndex = 12

                fun parseName(): List<String> {
                    val labels = mutableListOf<String>()
                    var labelLength = array[currentByteIndex].toInt()
                    var isPointer = false

                    while (labelLength != 0 && !isPointer) {
                        val labelLengthBits = labelLength.toBooleanArray(8)
                        isPointer = labelLengthBits[0] && labelLengthBits[1]

                        if (isPointer) {
                            val firstOffsetByte = array[currentByteIndex].toUInt().and(63u).toByte()
                            val offset = ByteUtils.twoBytesToInt(firstOffsetByte, array[currentByteIndex+1])
                            val oldPointer = currentByteIndex
                            currentByteIndex = offset
                            val pointedName = parseName()
                            currentByteIndex = oldPointer
                            labels.addAll(pointedName)
                            currentByteIndex += 1
                        } else {
                            val labelStartIndex = currentByteIndex + 1
                            val labelEndIndex = currentByteIndex + labelLength
                            val labelBytes = array.copyOfRange(labelStartIndex, labelEndIndex + 1)
                            labels.add(labelBytes.decodeToString())
                            currentByteIndex += (labelLength + 1)
                            labelLength = array[currentByteIndex].toInt()
                        }
                    }
                    return labels
                }

                for (i in 0 until questionsCount) {
                    val labels = parseName()
                    val typeValue = ByteUtils.twoBytesToInt(array[currentByteIndex + 1], array[currentByteIndex + 2])
                    val classValue = ByteUtils.twoBytesToInt(array[currentByteIndex + 3], array[currentByteIndex + 4])

                    val type = DnsType.fromValue(typeValue)
                    if (type !in Config.supportedDnsTypes) {
                        throw UnsupportedDnsTypeException(type)
                    }

                    question(DnsName(labels), type, DnsClass.fromValue(classValue))

                    currentByteIndex += 5
                }

                fun parseResourceRecord(): ResourceRecord {
                    val name = DnsName(parseName())

                    val typeValue = ByteUtils.twoBytesToInt(array[currentByteIndex + 1], array[currentByteIndex + 2])
                    val classValue = ByteUtils.twoBytesToInt(array[currentByteIndex + 3], array[currentByteIndex + 4])

                    val type = DnsType.fromValue(typeValue)

                    val dnsClass = DnsClass.fromValue(classValue)
                    val timeToLive = ByteUtils.fourBytesToInt(
                        array[currentByteIndex + 5],
                        array[currentByteIndex + 6],
                        array[currentByteIndex + 7],
                        array[currentByteIndex + 8]
                    )

                    val dataLength = ByteUtils.twoBytesToInt(array[currentByteIndex + 9], array[currentByteIndex + 10])
                    val dataBytes = array.sliceArray(currentByteIndex + 11 until currentByteIndex + 11 + dataLength)
                    currentByteIndex += 11

                    val data = when (type) {
                        DnsType.NS -> RRData.NS(DnsName(parseName())).also { currentByteIndex += 1  }
                        DnsType.A -> RRData.A(dataBytes.joinToString(".") { "%02x".format(it).toInt(16).toString() })
                            .also { currentByteIndex += dataLength  }
                        else -> RRData.NULL(dataBytes).also { currentByteIndex += dataLength }
                    }

                    return ResourceRecord(name, dnsClass, timeToLive, data)
                }

                for (i in 0 until answersCount) { answer(parseResourceRecord()) }
                for (i in 0 until authoritiesCount) { authority(parseResourceRecord()) }
                for (i in 0 until additionalsCount) { additional(parseResourceRecord()) }
            }
        }
    }
}