package resolver

import Config
import utils.*

data class DnsPacket(
    val id: Int,
    val isQuery: Boolean,
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
        val labels: List<String>
    )

    abstract class ResourceRecord(
        val labels: List<String>,
        val dnsClass: DnsClass,
        val timeToLive: Int
    ) {
        abstract val dnsType: DnsType
        abstract val data: ByteArray
    }

    class ResourceRecordA(
        labels: List<String>,
        dnsClass: DnsClass,
        timeToLive: Int,
        val ipV4: String
    ) : ResourceRecord(labels, dnsClass, timeToLive) {

        override val dnsType: DnsType = DnsType.A
        override val data: ByteArray

        init {
            val ipBytes = ipV4.split(".")
            require(ipBytes.size == 4 && ipBytes.all { it.toInt() < 256 })
            data = byteArrayOf(*ipBytes.map { it.toInt().toByte() }.toByteArray())
        }
    }

    class ResourceRecordNS(
        labels: List<String>,
        dnsClass: DnsClass,
        timeToLive: Int,
        val nsLabels: List<String>
    ) : ResourceRecord(labels, dnsClass, timeToLive) {

        override val dnsType: DnsType = DnsType.NS
        override val data: ByteArray = ByteUtils.listOfLabelsToDnsNameBytes(nsLabels)
    }

    class ResourceRecordAAAA(
        labels: List<String>,
        dnsClass: DnsClass,
        timeToLive: Int,
        val ipV6: String
    ) : ResourceRecord(labels, dnsClass, timeToLive) {

        override val dnsType: DnsType = DnsType.AAAA
        override val data: ByteArray

        init {
            val ipBytes = ipV6.split(":").flatMap { it.chunked(2) }
            require(ipBytes.size == 16 && ipBytes.all { it.toLong(radix = 16) < 256 })
            data = byteArrayOf(*ipBytes.map { it.toLong(radix = 16).toByte() }.toByteArray())
        }
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

        byte2[0] = isQuery

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
            bytesList.addAll(ByteUtils.listOfLabelsToDnsNameBytes(it.labels).toList())
            bytesList.addAll(it.type.value.toTwoBytes())
            bytesList.addAll(it.dnsClass.value.toTwoBytes())
        }

        fun addResourceRecord(record: ResourceRecord) {
            bytesList.addAll(ByteUtils.listOfLabelsToDnsNameBytes(record.labels).toList())
            bytesList.addAll(record.dnsType.value.toTwoBytes())
            bytesList.addAll(record.dnsClass.value.toTwoBytes())
            bytesList.addAll(record.timeToLive.toFourBytes())
            bytesList.addAll(record.data.size.toTwoBytes())
            bytesList.addAll(record.data.toList())
        }

        answers.forEach { addResourceRecord(it) }
        authorities.forEach { addResourceRecord(it) }
        additionals.forEach { addResourceRecord(it) }

        bytesList.toByteArray()
    }

    companion object {
        fun fromByteArray(array: ByteArray): DnsPacket {
            return packet {
                id = ByteUtils.twoBytesToInt(array[0], array[1])

                val byte2 = array[2].toBooleanArray()
                val byte3 = array[3].toBooleanArray()
                isQuery = byte2[0]
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

                var pointer = 12

                fun parseName(): List<String> {
                    val labels = mutableListOf<String>()
                    var labelLength = array[pointer].toInt()
                    while (labelLength != 0) {
                        val labelStartIndex = pointer + 1
                        val labelEndIndex = pointer + labelLength
                        val labelBytes = array.copyOfRange(labelStartIndex, labelEndIndex + 1)
                        labels.add(labelBytes.decodeToString())
                        pointer += (labelLength + 1)
                        labelLength = array[pointer].toInt()
                    }
                    return labels
                }

                for (i in 0 until questionsCount) {
                    val labels = parseName()
                    val typeValue = ByteUtils.twoBytesToInt(array[pointer + 1], array[pointer + 2])
                    val classValue = ByteUtils.twoBytesToInt(array[pointer + 3], array[pointer + 4])

                    val type = DnsType.fromValue(typeValue)
                    if (type !in Config.supportedDnsTypes) {
                        throw UnsupportedDnsTypeException(type)
                    }

                    question(type, DnsClass.fromValue(classValue)) {
                        labels.forEach { - it }
                    }

                    pointer += 5
                }

                fun ResourceRecordDsl.parseResourceRecord() {
                    val labels = parseName()
                    val typeValue = ByteUtils.twoBytesToInt(array[pointer + 1], array[pointer + 2])
                    val classValue = ByteUtils.twoBytesToInt(array[pointer + 3], array[pointer + 4])

                    val type = DnsType.fromValue(typeValue)

                    val timeToLive = ByteUtils.fourBytesToInt(
                        array[pointer + 5],
                        array[pointer + 6],
                        array[pointer + 7],
                        array[pointer + 8]
                    )

                    val dataLength = ByteUtils.twoBytesToInt(array[pointer + 9], array[pointer + 10])
                    val data = array.sliceArray(pointer + 11 until pointer + 11 + dataLength)

                    when (type) {
                        DnsType.NS -> typeNS { parseName().forEach { - it } }
                        DnsType.A -> {
                            typeA(data.joinToString(".") { it.toInt().toString() })
                            pointer += 11 + dataLength
                        }
                        DnsType.AAAA -> {
                            typeAAAA(
                                data.toList().chunked(2).joinToString(":") {
                                    ByteUtils.twoBytesToInt(it[0], it[1]).toString()
                                }
                            )
                        }
                        else -> throw UnsupportedDnsTypeException(type)
                    }
                }

                for (i in 0 until answersCount) { answer { parseResourceRecord() } }
                for (i in 0 until authoritiesCount) { authority { parseResourceRecord() } }
                for (i in 0 until additionalsCount) { additional { parseResourceRecord() } }
            }
        }
    }
}