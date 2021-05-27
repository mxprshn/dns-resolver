package resolver

interface DnsService {
    //suspend fun resolveAddress(input: ByteArray): ByteArray

    suspend fun resolve(domainName: String, type: DnsType): Result

    data class Result(val ips: List<String>)
}