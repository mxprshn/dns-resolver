package resolver

import Config
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import utils.log
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
        val queue = DnsServersQueue(domainName, Config.rootServers.map { it.hostname })

        while (queue.isNotEmpty()) {
            val nsHost = queue.dequeue()
            if (!cache.containsKey(nsHost)) {
                cache[nsHost] = resolve(nsHost).ips.toMutableList()
            }
            val nsIp = cache[nsHost]?.first() ?: continue

            val questionPacket = packet {
                id = 73
                isResponse = false
                opcode = 0
                authoritativeAnswer = false
                truncation = false
                recursionDesired = false
                recursionAvailable = false
                responseCode = 0

                question(DnsType.A, DnsClass.IN) {
                    domainName.split(".").forEach { - it }
                }
            }.bytes

            val socket = aSocket(ActorSelectorManager(Dispatchers.IO))
                .udp()
                .connect(InetSocketAddress(nsIp, 53))
            socket.send(Datagram(ByteReadPacket(questionPacket), NetworkAddress(nsIp, 53)))
            val receivedPacketBytes = socket.receive().packet.readBytes()
            log("Received: ${receivedPacketBytes.joinToString("") { "%02x".format(it) }}")

            val receivedPacket = DnsPacket.fromByteArray(receivedPacketBytes)
            val resolvedIpV4s = receivedPacket.answers.filterIsInstance<DnsPacket.ResourceRecordA>().map { it.ipV4 }

            if (resolvedIpV4s.isNotEmpty()) {
                return DnsService.Result(resolvedIpV4s)
            }

            val nsAnswers = receivedPacket.authorities.filterIsInstance<DnsPacket.ResourceRecordNS>()

            if (nsAnswers.isEmpty()) continue

            val additionals = receivedPacket.additionals
                .filterIsInstance<DnsPacket.ResourceRecordA>()

            nsAnswers.forEach { nsRR ->
                val ip = additionals.firstOrNull { it.labels == nsRR.nsLabels }?.ipV4
                val joinedName = nsRR.nsLabels.joinToString(".")
                if (ip != null && !cache.containsKey(joinedName)) {
                    cache[joinedName] = mutableListOf(ip)
                }
                queue.enqueue(joinedName)
            }
        }

        return DnsService.Result(emptyList())
    }
}