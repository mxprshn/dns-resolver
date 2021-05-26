package resolver

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class ExternalDnsService(private val ip: String) : DnsService {
    private val socket = aSocket(ActorSelectorManager(Dispatchers.IO))
            .udp()
            .connect(InetSocketAddress(ip, 53))

    override suspend fun resolveAddress(input: ByteArray): ByteArray {
        socket.send(Datagram(ByteReadPacket(input), NetworkAddress(ip, 53)))
        return socket.receive().packet.readBytes()
    }
}