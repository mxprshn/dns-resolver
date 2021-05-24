package resolver

import utils.ByteUtils

class DnsPacketParserImpl : DnsPacketParser {

    override fun getRequestedUrlsFromPacket(packet: ByteArray): List<DnsQuestion> {
        val questionCount = ByteUtils.twoBytesToInt(packet[4], packet[5])
        val result = mutableListOf<DnsQuestion>()
        var pointer = DNS_HEADER_LENGTH_BYTES
        for (i in 0 until questionCount) {
            val labels = mutableListOf<String>()

            var labelLength = packet[pointer].toInt()
            while (labelLength != 0) {
                val labelStartIndex = pointer + 1
                val labelEndIndex = pointer + labelLength
                val labelBytes = packet.copyOfRange(labelStartIndex, labelEndIndex + 1)
                labels.add(labelBytes.toString())
                pointer += (labelLength + 1)
                labelLength = packet[pointer].toInt()
            }

            val typeValue = ByteUtils.twoBytesToInt(packet[pointer + 1], packet[pointer + 2])
            val classValue = ByteUtils.twoBytesToInt(packet[pointer + 3], packet[pointer + 4])

            result.add(
                DnsQuestion(
                    names = labels,
                    type = DnsType.fromValue(typeValue),
                    dnsClass = DnsClass.fromValue(classValue)
                )
            )

            pointer += 5
        }

        return result
    }

    companion object {
        private const val DNS_HEADER_LENGTH_BYTES = 12
    }
}