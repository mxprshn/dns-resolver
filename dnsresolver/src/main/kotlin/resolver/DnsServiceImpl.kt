package resolver

import Config
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.whileSelect
import utils.log
import java.net.InetSocketAddress

class DnsServiceImpl : DnsService {
    private abstract class DnsResult
    private class NameServer(hostname: String) : DnsResult()
    private class ResolvedAddress(ip: String)  : DnsResult()

    @ExperimentalCoroutinesApi
    override suspend fun resolve(domainName: String, type: DnsType): DnsService.Result {
        resolveZone("com", DnsType.A, Config.rootServers.map { it.ipV4 })
        return DnsService.Result(emptyList())
    }

    @ExperimentalCoroutinesApi
    private suspend fun resolveZone(zoneName: String, type:DnsType, serverAddresses: List<String>): DnsResult {
        val packet = packet {
            id = 73
            isResponse = false
            opcode = 0
            authoritativeAnswer = false
            truncation = false
            recursionDesired = false
            recursionAvailable = false
            responseCode = 0

            question(type, DnsClass.IN) {
                - zoneName
            }
        }.bytes

        coroutineScope {
            whileSelect {
                serverAddresses.map {
                    async {
                        val socket = aSocket(ActorSelectorManager(Dispatchers.IO))
                            .udp()
                            .connect(InetSocketAddress(it, 53))
                        socket.send(Datagram(ByteReadPacket(packet), NetworkAddress(it, 53)))
                        socket.receive().packet.readBytes()
                    }
                }
                .forEach {
                    it.onAwait { array ->
                        log("Received: ${array.joinToString("") { "%02x".format(it) }}")
                        val receivedPacket = DnsPacket.fromByteArray(array)
                        val resolvedIps = receivedPacket.answers.filter { it.dnsType == DnsType.A || it.dnsType == DnsType.AAAA }
                        val nameServers = receivedPacket.answers.filter { it.dnsType == DnsType.NS }

                        true
                    }
                }
            }
        }

        return NameServer("")
    }
}