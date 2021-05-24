package resolver

data class DnsQuestion(val names: List<String>, val type: DnsType, val dnsClass: DnsClass)