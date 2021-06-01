package resolver

import Config.DNS_PORT
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import utils.log
import java.net.InetSocketAddress

@ExperimentalUnsignedTypes
fun runDns(ip: String) {
    val service = DnsServiceImpl()

    runBlocking {
        val server = aSocket(ActorSelectorManager(Dispatchers.IO))
            .udp()
            .bind(InetSocketAddress(ip, DNS_PORT))

        log("Serving at port $DNS_PORT")

        while (true) {
            val receivedDatagram = server.receive()

            launch {
                val packet = receivedDatagram.packet.readBytes()
                runCatching {
                    service.resolve(packet)
                }.onFailure {
                    log(it, "Cannot resolve name")
                }.onSuccess {
                    server.send(Datagram(ByteReadPacket(it), receivedDatagram.address))
                }

            }
        }
    }
}