package utils

object ByteUtils {
    fun twoBytesToInt(high: Byte, low: Byte): Int {
        return (high.toInt() shl 8) + low
    }

    fun intToTwoBytes(value: Int): List<Byte> {
        return listOf(
            (value shr 8).toByte(),
            (value and 0b11111111).toByte()
        )
    }
}