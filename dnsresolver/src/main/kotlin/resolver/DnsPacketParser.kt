package resolver

interface DnsPacketParser {
    fun getRequestedUrlsFromPacket(packet: ByteArray): List<DnsQuestion>
}