package resolver

class DnsServiceImpl : DnsService {
    override suspend fun resolve(domainName: String, type: DnsType): DnsService.Result {

        packet {
            id = 44
            isQuery = false
            truncation = false

            question(DnsType.A, DnsClass.IN) {
                - "www"
                - "google"
                - "com"
            }

            answer {
                name {
                    - "kek"
                    - "lol"
                    - "kekdns"
                }
                typeA("198.2.2.1")
            }


        }

        TODO()
    }
}