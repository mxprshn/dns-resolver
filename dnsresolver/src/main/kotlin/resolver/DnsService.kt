package resolver

interface DnsService {
    suspend fun resolve(domainName: String): Result
    suspend fun resolve(dnsQuery: ByteArray): ByteArray

    data class Result(val ips: List<String>)
}