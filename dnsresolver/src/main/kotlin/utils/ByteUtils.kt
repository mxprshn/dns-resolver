package utils

object ByteUtils {
    fun twoBytesToInt(high: Byte, low: Byte): Int {
        return (high.toInt() shl 8) + low
    }

    fun fourBytesToInt(vararg bytes: Byte): Int {
        require(bytes.size == 4)
        return (bytes[0].toInt() shl 32) +
            (bytes[1].toInt() shl 16) +
            (bytes[2].toInt() shl 8) +
            bytes[3]
    }

    fun listOfLabelsToDnsNameBytes(labels: List<String>): ByteArray {
        require(labels.all { it.length < 64 })
        val result = mutableListOf<Byte>()
        labels.forEach {
            result.add(it.length.toByte())
            result.addAll(it.toByteArray().toList())
        }
        result.add(0)
        require(result.size < 256)
        return result.toByteArray()
    }
}