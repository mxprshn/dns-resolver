package resolver

data class DnsName(val value: String) {
    val labels = value.split(".")

    constructor(labels: List<String>) : this(labels.joinToString("."))
}