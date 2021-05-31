package resolver

import utils.log

fun packet(builder: PacketDsl.() -> Unit): DnsPacket {
    val packetDsl = PacketDslImpl()
    builder(packetDsl)
    return packetDsl.packet
}

interface PacketDsl {

    var id: Int
    var isResponse: Boolean
    var opcode: Int
    var authoritativeAnswer: Boolean
    var truncation: Boolean
    var recursionDesired: Boolean
    var recursionAvailable: Boolean
    var responseCode: Int

    fun question(
        type: DnsType,
        dnsClass: DnsClass,
        nameBuilder: NameDsl.() -> Unit
    )

    fun answer(builder: ResourceRecordDsl.() -> Unit)

    fun authority(builder: ResourceRecordDsl.() -> Unit)

    fun additional(builder: ResourceRecordDsl.() -> Unit)
}

interface NameDsl {
    operator fun String.unaryMinus()
}

interface ResourceRecordDsl {
    var dnsClass: DnsClass
    var timeToLive: Int

    fun name(nameBuilder: NameDsl.() -> Unit)
    fun typeA(ipV4: String)
    fun typeNS(nameBuilder: NameDsl.() -> Unit)
    fun typeAAAA(ipV6: String)
}

class PacketDslImpl : PacketDsl {

    override var id: Int = 0
    override var isResponse: Boolean = false
    override var opcode: Int = 0
    override var authoritativeAnswer: Boolean = false
    override var recursionAvailable: Boolean = false
    override var recursionDesired: Boolean = false
    override var responseCode: Int = 0
    override var truncation: Boolean = false

    private val questions = mutableListOf<DnsPacket.Question>()
    private val answers = mutableListOf<DnsPacket.ResourceRecord>()
    private val authorities = mutableListOf<DnsPacket.ResourceRecord>()
    private val additionals = mutableListOf<DnsPacket.ResourceRecord>()

    val packet get() = DnsPacket(
        id = id,
        isResponse = isResponse,
        opcode = opcode,
        authoritativeAnswer = authoritativeAnswer,
        recursionAvailable = recursionAvailable,
        recursionDesired = recursionDesired,
        responseCode = responseCode,
        truncation = truncation,
        questions = questions,
        answers = answers,
        authorities = authorities,
        additionals = additionals
    )

    override fun question(type: DnsType, dnsClass: DnsClass, nameBuilder: NameDsl.() -> Unit) {
        val nameDsl = NameDslImpl()
        nameBuilder(nameDsl)
        questions.add(
            DnsPacket.Question(
                type,
                dnsClass,
                nameDsl.labels
            )
        )
    }

    override fun answer(builder: ResourceRecordDsl.() -> Unit) {
        val recordDsl = ResourceRecordDslImpl()
        builder(recordDsl)
        answers.add(recordDsl.resourceRecord)
        log("Parsed answers:${answers.size} auth:${authorities.size} add: ${additionals.size}")
    }

    override fun authority(builder: ResourceRecordDsl.() -> Unit) {
        val recordDsl = ResourceRecordDslImpl()
        builder(recordDsl)
        authorities.add(recordDsl.resourceRecord)
        log("Parsed answers:${answers.size} auth:${authorities.size} add: ${additionals.size}")
    }

    override fun additional(builder: ResourceRecordDsl.() -> Unit) {
        val recordDsl = ResourceRecordDslImpl()
        builder(recordDsl)
        additionals.add(recordDsl.resourceRecord)
        log("Parsed answers:${answers.size} auth:${authorities.size} add: ${additionals.size}")
    }
}

class NameDslImpl : NameDsl {

    private val mLabels = mutableListOf<String>()
    val labels: List<String> = mLabels

    override fun String.unaryMinus() {
        mLabels.add(this)
    }
}

class ResourceRecordDslImpl : ResourceRecordDsl {

    override var dnsClass: DnsClass = DnsClass.IN
    override var timeToLive: Int = 0

    private val nameDsl = NameDslImpl()

    private var mResourceRecord: DnsPacket.ResourceRecord? = null
    val resourceRecord get() = mResourceRecord ?: throw IllegalStateException("Resource record not configured")

    override fun name(nameBuilder: NameDsl.() -> Unit) {
        nameBuilder(nameDsl)
    }

    override fun typeA(ipV4: String) {
        mResourceRecord = DnsPacket.ResourceRecordA(nameDsl.labels, dnsClass, timeToLive, ipV4)
    }

    override fun typeAAAA(ipV6: String) {
        mResourceRecord = DnsPacket.ResourceRecordAAAA(nameDsl.labels, dnsClass, timeToLive, ipV6)
    }

    override fun typeNS(nameBuilder: NameDsl.() -> Unit) {
        val nameServerDsl = NameDslImpl()
        nameBuilder(nameServerDsl)
        mResourceRecord = DnsPacket.ResourceRecordNS(nameDsl.labels, dnsClass, timeToLive, nameServerDsl.labels)
    }
}