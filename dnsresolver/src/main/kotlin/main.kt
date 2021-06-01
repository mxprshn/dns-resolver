import resolver.runDns

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    runDns(Config.HOST_IP)
}