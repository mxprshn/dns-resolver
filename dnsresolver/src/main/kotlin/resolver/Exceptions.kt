package resolver

class UnsupportedDnsTypeException(type: DnsType) : Throwable("Unsupported DNS type: $type")