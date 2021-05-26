package resolver

data class DnsQuestion(val labels: List<String>, val type: DnsType, val dnsClass: DnsClass)