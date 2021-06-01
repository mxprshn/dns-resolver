package resolver

fun packet(
    id: Int = 0,
    isResponse: Boolean = false,
    opcode: Int = 0,
    authoritativeAnswer: Boolean = false,
    truncation: Boolean = false,
    recursionDesired: Boolean = false,
    recursionAvailable: Boolean = false,
    responseCode: Int = 0,
    builder: PacketDsl.() -> Unit
): DnsPacket {
    val packetDsl = PacketDslImpl(
        id,
        isResponse,
        opcode,
        authoritativeAnswer,
        truncation,
        recursionDesired,
        recursionAvailable,
        responseCode
    )
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
        name: DnsName,
        type: DnsType,
        dnsClass: DnsClass = DnsClass.IN,
    )

    fun question(question: DnsPacket.Question)

    fun answer(
        name: DnsName,
        timeToLive: Int = 0,
        dnsClass: DnsClass = DnsClass.IN,
        data: () -> RRData
    )

    fun answer(rr: DnsPacket.ResourceRecord)

    fun authority(
        name: DnsName,
        timeToLive: Int = 0,
        dnsClass: DnsClass = DnsClass.IN,
        data: () -> RRData
    )

    fun authority(rr: DnsPacket.ResourceRecord)

    fun additional(
        name: DnsName,
        timeToLive: Int = 0,
        dnsClass: DnsClass = DnsClass.IN,
        data: () -> RRData
    )

    fun additional(rr: DnsPacket.ResourceRecord)
}

class PacketDslImpl(override var id: Int,
                    override var isResponse: Boolean,
                    override var opcode: Int,
                    override var authoritativeAnswer: Boolean,
                    override var truncation: Boolean,
                    override var recursionDesired: Boolean,
                    override var recursionAvailable: Boolean,
                    override var responseCode: Int) : PacketDsl {

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

    override fun additional(rr: DnsPacket.ResourceRecord) {
        additionals.add(rr)
    }

    override fun additional(name: DnsName, timeToLive: Int, dnsClass: DnsClass, data: () -> RRData) {
        additionals.add(DnsPacket.ResourceRecord(name, dnsClass, timeToLive, data()))
    }

    override fun answer(rr: DnsPacket.ResourceRecord) {
        answers.add(rr)
    }

    override fun answer(name: DnsName, timeToLive: Int, dnsClass: DnsClass, data: () -> RRData) {
        answers.add(DnsPacket.ResourceRecord(name, dnsClass, timeToLive, data()))
    }

    override fun authority(rr: DnsPacket.ResourceRecord) {
        authorities.add(rr)
    }

    override fun authority(name: DnsName, timeToLive: Int, dnsClass: DnsClass, data: () -> RRData) {
        authorities.add(DnsPacket.ResourceRecord(name, dnsClass, timeToLive, data()))
    }

    override fun question(question: DnsPacket.Question) {
        questions.add(question)
    }

    override fun question(name: DnsName, type: DnsType, dnsClass: DnsClass) {
        questions.add(DnsPacket.Question(type, dnsClass, name))
    }
}