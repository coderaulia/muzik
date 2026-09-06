package io.github.coderaulia.muzikplayer.utils

private const val INDEX_MULTIPLIER = 999983
private const val ITERATIONS = 100

fun ByteArray.fastHashCode(): Int {
    var byteIndex = INDEX_MULTIPLIER % size
    var ret: Int = 0
    repeat(ITERATIONS) {
        ret = ret * 31 + this[byteIndex]
        byteIndex = (byteIndex + INDEX_MULTIPLIER) % size
    }
    return ret
}