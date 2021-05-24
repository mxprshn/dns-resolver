package utils

object ByteUtils {
    fun twoBytesToInt(high: Byte, low: Byte): Int {
        return (high.toInt() shl 8) + low
    }
}