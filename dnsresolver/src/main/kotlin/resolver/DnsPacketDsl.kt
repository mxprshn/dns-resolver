package resolver

fun packet(
    /**
        A 16 bit identifier assigned by the program that
        generates any kind of query.  This identifier is copied
        the corresponding reply and can be used by the requester
        to match up replies to outstanding queries.
     */
    id: Int = 0,
    /**
        A one bit field that specifies whether this message is a
        query (0), or a response (1).
     */
    isResponse: Boolean = false,
    /**
        A four bit field that specifies kind of query in this
        message.  This value is set by the originator of a query
        and copied into the response.  The values are:

        0               a standard query (QUERY)

        1               an inverse query (IQUERY)

        2               a server status request (STATUS)

        3-15            reserved for future use
     */
    opcode: Int = 0,
    /**
        Authoritative Answer - this bit is valid in responses,
        and specifies that the responding name server is an
        authority for the domain name in question section.

        Note that the contents of the answer section may have
        multiple owner names because of aliases.  The AA bit
        corresponds to the name which matches the query name, or
        the first owner name in the answer section.
     */
    authoritativeAnswer: Boolean = false,
    /**
        TrunCation - specifies that this message was truncated
        due to length greater than that permitted on the
        transmission channel.
     */
    truncation: Boolean = false,
    /**
        Recursion Desired - this bit may be set in a query and
        is copied into the response.  If RD is set, it directs
        the name server to pursue the query recursively.
        Recursive query support is optional.
     */
    recursionDesired: Boolean = false,
    /**
        Recursion Available - this be is set or cleared in a
        response, and denotes whether recursive query support is
        available in the name server.
     */
    recursionAvailable: Boolean = false,
    /**
        Response code - this 4 bit field is set as part of
        responses.  The values have the following
        interpretation:

        0               No error condition

        1               Format error - The name server was
        unable to interpret the query.

        2               Server failure - The name server was
        unable to process this query due to a
        problem with the name server.

        3               Name Error - Meaningful only for
        responses from an authoritative name
        server, this code signifies that the
        domain name referenced in the query does
        not exist.

        4               Not Implemented - The name server does
        not support the requested kind of query.

        5               Refused - The name server refuses to
        perform the specified operation for
        policy reasons.  For example, a name
        server may not wish to provide the
        information to the particular requester,
        or a name server may not wish to perform
        a particular operation (e.g., zone
        transfer) for particular data.

        6-15            Reserved for future use.
     */
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
    }

    override fun authority(builder: ResourceRecordDsl.() -> Unit) {
        val recordDsl = ResourceRecordDslImpl()
        builder(recordDsl)
        authorities.add(recordDsl.resourceRecord)
    }

    override fun additional(builder: ResourceRecordDsl.() -> Unit) {
        val recordDsl = ResourceRecordDslImpl()
        builder(recordDsl)
        additionals.add(recordDsl.resourceRecord)
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