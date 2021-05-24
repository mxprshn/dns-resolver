package resolver

enum class DnsClass(val value: Int) {
    IN(1);

    companion object {
        fun fromValue(value: Int): DnsClass {
            return values().firstOrNull { it.value == value } ?: throw IllegalArgumentException("Unsupported DNS class: $value")
        }
    }
}