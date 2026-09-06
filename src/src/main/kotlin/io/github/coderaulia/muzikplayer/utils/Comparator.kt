package io.github.coderaulia.muzikplayer.utils

fun <T> noopComparator(): Comparator<T> = compareBy { 0 }
fun <T> Comparator<T>?.orNoop(): Comparator<T> = this ?: noopComparator()
