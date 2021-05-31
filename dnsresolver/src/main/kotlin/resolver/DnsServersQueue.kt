package resolver

import java.util.*

class DnsServersQueue(val targetHost: String, initial: List<String>) {

    private data class Entry(val host: String, val priority: Int)
    private val entries = PriorityQueue { e1: Entry, e2: Entry -> e2.priority - e1.priority }
    private val askedHosts = mutableSetOf<String>()
    private val splitTargetHost = targetHost.split(".")

    init {
        initial.forEach {
            entries.add(Entry(it, -1))
            askedHosts.add(it)
        }
    }

    fun enqueue(host: String) {
        if (askedHosts.contains(host)) return
        val priority = commonTailLength(splitTargetHost, host.split("."))
        entries.add(Entry(host, priority))
        askedHosts.add(host)
    }

    fun dequeue(): String = entries.remove().host

    fun isNotEmpty() = entries.isNotEmpty()

    private fun commonTailLength(first: List<String>, second: List<String>): Int {
        var count = 0
        for (i in 0 until minOf(first.size, second.size)) {
            if (first[first.size - 1 - i] != second[second.size - 1 - i]) {
                return count
            }
            ++count
        }

        return count
    }
}