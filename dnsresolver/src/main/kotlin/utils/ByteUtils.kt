package utils

object ByteUtils {
    @ExperimentalUnsignedTypes
    fun twoBytesToInt(high: Byte, low: Byte): Int {
        val string = "${"%02x".format(high)}${"%02x".format(low)}"
        return string.toInt(16)
    }

    @ExperimentalUnsignedTypes
    fun fourBytesToInt(vararg bytes: Byte): Int {
        require(bytes.size == 4)
        val result = ((bytes[0].toUInt() shl 32) +
            (bytes[1].toUInt() shl 16) +
            (bytes[2].toUInt() shl 8) +
             bytes[3].toUInt())
        return result.toInt()
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