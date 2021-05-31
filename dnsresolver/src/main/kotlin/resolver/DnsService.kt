package resolver

interface DnsService {
    suspend fun resolve(domainName: String, type: DnsType): Result

    data class Result(val ips: List<String>)
}