package resolver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking

private const val DNS_PORT = 53

@ExperimentalCoroutinesApi
fun runDns(ip: String) {

    runBlocking {
        val service = DnsServiceImpl()
        service.resolve("hwproj.me")
    }
}