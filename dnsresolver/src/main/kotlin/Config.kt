import resolver.DnsType

object Config {
    const val HOST_IP = "192.168.1.40"
    const val DNS_PORT = 53

    val supportedDnsTypes = listOf(
        DnsType.A,
        DnsType.NS,
        DnsType.PTR,
        DnsType.CNAME,
        DnsType.NULL
    )

    /**
     * List of root DNS servers
     *
     * @see <a href="https://www.iana.org/domains/root/servers">IANA</a>
     */
    val rootServers = listOf(
        RootServer("a.root-servers.net", "198.41.0.4", "Verisign, Inc."),
        RootServer("b.root-servers.net", "199.9.14.201", "University of Southern California, Information Sciences Institute"),
        RootServer("c.root-servers.net", "192.33.4.12", "Cogent Communications"),
        RootServer("d.root-servers.net", "199.7.91.13", "University of Maryland"),
        RootServer("e.root-servers.net", "192.203.230.10", "NASA (Ames Research Center)"),
        RootServer("f.root-servers.net", "192.5.5.241", "Internet Systems Consortium, Inc."),
        RootServer("g.root-servers.net", "192.112.36.4", "US Department of Defense (NIC)"),
        RootServer("h.root-servers.net", "198.97.190.53", "US Army (Research Lab)"),
        RootServer("i.root-servers.net", "192.36.148.17", "Netnod"),
        RootServer("j.root-servers.net", "192.58.128.30", "Verisign, Inc."),
        RootServer("k.root-servers.net", "193.0.14.129", "RIPE NCC"),
        RootServer("l.root-servers.net", "199.7.83.42", "ICANN"),
        RootServer("m.root-servers.net", "202.12.27.33", "WIDE Project"),
    )
}

data class RootServer(
    val hostname: String,
    val ipV4: String,
    val operator: String
)