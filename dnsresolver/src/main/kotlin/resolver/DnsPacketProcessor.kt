package resolver

interface DnsPacketProcessor {
    fun getRequestedUrlsFromPacket(packet: ByteArray): List<DnsQuestion>
    fun generateAnswerPacket(questionPacket: ByteArray, answers: List<Pair<DnsQuestion, String>>): ByteArray
}