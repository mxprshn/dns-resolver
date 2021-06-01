package resolver

import utils.ByteUtils

interface RRData {
    val bytes: ByteArray
    val type: DnsType

    class A(val ipV4: String) : RRData {
        override val bytes: ByteArray
        init {
            val ipBytes = ipV4.split(".")
            require(ipBytes.size == 4 && ipBytes.all { it.toInt() < 256 })
            bytes = byteArrayOf(*ipBytes.map { it.toInt().toByte() }.toByteArray())
        }

        override val type = DnsType.A
    }

    class NS(val serverName: DnsName) : RRData {
        override val bytes: ByteArray = ByteUtils.listOfLabelsToDnsNameBytes(serverName.labels)
        override val type = DnsType.NS
    }

    class CNAME(val canonicalName: DnsName) : RRData {
        override val bytes: ByteArray = ByteUtils.listOfLabelsToDnsNameBytes(canonicalName.labels)
        override val type = DnsType.CNAME
    }

    class PTR(val destinationName: DnsName) : RRData {
        override val bytes: ByteArray = ByteUtils.listOfLabelsToDnsNameBytes(destinationName.labels)
        override val type = DnsType.PTR
    }

    class NULL(anything: ByteArray) : RRData {
        override val bytes: ByteArray = anything
        override val type = DnsType.NULL
    }
}