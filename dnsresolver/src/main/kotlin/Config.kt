import resolver.DnsType

object Config {
    val supportedDnsTypes = listOf(
        DnsType.A,
        DnsType.AAAA,
        DnsType.NS
    )
}