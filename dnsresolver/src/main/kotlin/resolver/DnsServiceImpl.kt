package resolver

import utils.log

class DnsServiceImpl(private val processor: DnsPacketProcessor,
                     private val externalDnsService: DnsService,
                     private val labelsToBlock: Set<String>,
                     private val stubPageAddress: String
) : DnsService {
    override suspend fun resolveAddress(input: ByteArray): ByteArray {
        val questions = processor.getRequestedUrlsFromPacket(input)
        questions.forEach {
            log("Question: ${it.type} | ${it.labels.joinToString(".")}")
        }
        val originalAnswer = externalDnsService.resolveAddress(input)
        val fakeAnswer = processor.generateAnswerPacket(input, questions.map { it to stubPageAddress })
        return if(questions.any { q -> labelsToBlock.any { q.labels.joinToString(".").contains(it) } }) fakeAnswer else originalAnswer
    }
}