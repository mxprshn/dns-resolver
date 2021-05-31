package resolver

interface DnsService {
    suspend fun resolve(domainName: String): Result

    data class Result(val ips: List<String>)
}