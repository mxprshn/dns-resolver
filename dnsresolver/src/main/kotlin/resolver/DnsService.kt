package resolver

interface DnsService {
    fun resolveAddress(input: Input): Output

    data class Input(val packet: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Input

            if (!packet.contentEquals(other.packet)) return false

            return true
        }

        override fun hashCode(): Int {
            return packet.contentHashCode()
        }
    }

    data class Output(val packet: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Input

            if (!packet.contentEquals(other.packet)) return false

            return true
        }

        override fun hashCode(): Int {
            return packet.contentHashCode()
        }
    }
}