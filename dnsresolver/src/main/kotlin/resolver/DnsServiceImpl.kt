package resolver

import Config
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import utils.log
import utils.ofType
import java.net.InetSocketAddress

class DnsServiceImpl : DnsService {

    private val cache = mutableMapOf<String, MutableList<String>>()

    init {
        Config.rootServers.forEach {
            cache[it.hostname] = mutableListOf(it.ipV4)
        }
    }

    @ExperimentalUnsignedTypes
    override suspend fun resolve(domainName: String): DnsService.Result {
        val cached = cache[domainName]
        if (cached != null) {
            log("Found cached ips for $domainName")
            return DnsService.Result(cached)
        }

        val queue = DnsServersQueue(domainName, Config.rootServers.map { it.hostname })
        val name = DnsName(domainName)

        while (queue.isNotEmpty()) {
            val nsHost = queue.dequeue()
            if (!cache.containsKey(nsHost)) {

                cache[nsHost] = resolve(nsHost).ips.toMutableList()
            }
            val nsIp = cache[nsHost]?.first() ?: continue
            log("Asking $nsHost on $nsIp for $domainName")

            val questionPacket = packet(
                    id = (0..65535).random(),
                    isResponse = false,
                    opcode = 0,
                    authoritativeAnswer = false,
                    truncation = false,
                    recursionDesired = false,
                    recursionAvailable = false,
                    responseCode = 0
            ) {
                question(name, DnsType.A)
            }.bytes

            val socket = aSocket(ActorSelectorManager(Dispatchers.IO))
                .udp()
                .connect(InetSocketAddress(nsIp, 53))
            socket.send(Datagram(ByteReadPacket(questionPacket), NetworkAddress(nsIp, 53)))
            val receivedPacketBytes = socket.receive().packet.readBytes()

            val receivedPacket = DnsPacket.fromByteArray(receivedPacketBytes)
            val resolvedIpV4s = receivedPacket.answers.ofType<RRData.A>()


            if (resolvedIpV4s.isNotEmpty()) {
                val ips = resolvedIpV4s.map { it.second.ipV4 }
                cache[domainName] = ips.toMutableList()
                return DnsService.Result(resolvedIpV4s.map { it.second.ipV4 })
            }

            val nsAnswers = receivedPacket.authorities.ofType<RRData.NS>()

            if (nsAnswers.isEmpty()) continue

            val additionals = receivedPacket.additionals.ofType<RRData.A>()

            nsAnswers.forEach { (_, nsData) ->
                val ip = additionals.firstOrNull { it.first.name == nsData.serverName }?.second?.ipV4
                val joinedName = nsData.serverName.value
                if (ip != null && !cache.containsKey(joinedName)) {
                    cache[joinedName] = mutableListOf(ip)
                }
                queue.enqueue(joinedName)
            }
        }

        return DnsService.Result(emptyList())
    }

    @ExperimentalUnsignedTypes
    override suspend fun resolve(dnsQuery: ByteArray): ByteArray {
        val queryPacket = DnsPacket.fromByteArray(dnsQuery)
        if (queryPacket.questions.any { it.type !in Config.supportedDnsTypes }) {
            return packet(
                id = queryPacket.id,
                isResponse = true,
                opcode = 0,
                authoritativeAnswer = false,
                truncation = false,
                recursionDesired = false,
                recursionAvailable = false,
                responseCode = 4
            ) {}.bytes
        }

        val asked = queryPacket.questions.first().name
        val resolvedIps = resolve(asked.value).ips

        return packet(
            id = queryPacket.id,
            isResponse = true,
            opcode = 0,
            authoritativeAnswer = false,
            truncation = false,
            recursionDesired = false,
            recursionAvailable = false,
            responseCode = 0
        ) {
            queryPacket.questions.forEach { question(it) }
            resolvedIps.forEach { ip -> answer(asked, 0) { RRData.A(ip) } }
        }.bytes
    }
}