package resolver

interface DnsService {
    suspend fun resolveAddress(input: ByteArray): ByteArray
}