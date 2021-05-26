package resolver

import utils.ByteUtils

class DnsPacketProcessorImpl : DnsPacketProcessor {

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
                labels.add(labelBytes.decodeToString())
                pointer += (labelLength + 1)
                labelLength = packet[pointer].toInt()
            }

            val typeValue = ByteUtils.twoBytesToInt(packet[pointer + 1], packet[pointer + 2])
            val classValue = ByteUtils.twoBytesToInt(packet[pointer + 3], packet[pointer + 4])

            result.add(
                DnsQuestion(
                    labels = labels,
                    type = DnsType.fromValue(typeValue),
                    dnsClass = DnsClass.fromValue(classValue)
                )
            )

            pointer += 5
        }

        return result
    }

    override fun generateAnswerPacket(questionPacket: ByteArray, answers: List<Pair<DnsQuestion, String>>): ByteArray {
        val answerPacket = mutableListOf<Byte>()
        answerPacket.addAll(questionPacket.slice(0..1))
        answerPacket.add(0b10000000.toByte())
        answerPacket.add(0)
        answerPacket.addAll(questionPacket.slice(4..5))
        answerPacket.addAll(questionPacket.slice(4..5))
        answerPacket.addAll(listOf(0, 0, 0, 0))

        answers.forEach { (question, _) ->
            question.labels.forEach { label ->
                answerPacket.add(label.length.toByte())
                answerPacket.addAll(label.toByteArray().toList())
            }
            answerPacket.add(0)
            answerPacket.addAll(ByteUtils.intToTwoBytes(question.type.value))
            answerPacket.addAll(ByteUtils.intToTwoBytes(question.dnsClass.value))
        }

        answers.forEach { (question, answerIp) ->
            question.labels.forEach { label ->
                answerPacket.add(label.length.toByte())
                answerPacket.addAll(label.toByteArray().toList())
            }
            answerPacket.add(0)
            answerPacket.addAll(ByteUtils.intToTwoBytes(question.type.value))
            answerPacket.addAll(ByteUtils.intToTwoBytes(question.dnsClass.value))
            answerPacket.addAll(listOf(0, 0, 0, 0))
            answerPacket.addAll(listOf(0, 4))
            val ipOctets = answerIp.split(".").map { it.toInt().toByte() }
            answerPacket.addAll(ipOctets)
        }

        return answerPacket.toByteArray()
    }

    companion object {
        private const val DNS_HEADER_LENGTH_BYTES = 12
    }
}