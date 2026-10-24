package io.github.coderaulia.muzikplayer.data

import io.github.coderaulia.muzikplayer.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.ParseException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}
private val INLINE_TIMESTAMP_REGEX = Regex("""<\s*\d+:\d+(?:[.:]\d+)?\s*>""")

sealed interface Lyrics {
    val rawString: String

    data class Plain(val lyrics: String) : Lyrics {
        override val rawString: String get() = lyrics

        override fun toString(): String {
            return lyrics
        }
    }

    data class Synchronized(val lines: List<Line>) : Lyrics {
        override val rawString: String get() = toString()
        override fun toString(): String {
            return lines.joinToString(separator = "\n")
        }

        data class Line(
            val start: Duration,
            val content: String,
        ) {
            override fun toString(): String {
                val mm = start.inWholeMinutes.toString().padStart(2, '0')
                val ss = (start.inWholeSeconds - start.inWholeMinutes * 60).toString().padStart(2, '0')
                val xx = ((start.inWholeMilliseconds - start.inWholeSeconds * 1000) / 10).toString().padStart(2, '0')
                return "[$mm:$ss:$xx]$content"
            }
        }
    }

    companion object {
        fun of(lyrics: String?): Lyrics? {
            if (lyrics == null) return null

            try {
                return LrcFile.of(lyrics).toSynchronized()
            } catch (e: ParseException) {
                logger.trace { "Cannot parse Lrc: ${e.message}" }
            }

            val trimmed = lyrics.trimToNull() ?: return null
            return Plain(trimmed)
        }
    }
}

data class LrcFile(
    val rawString: String,
    val entries: List<Entry>,
) {
    data class Entry(
        val tagKey: String,
        val tagValue: String,
        val content: String?,
    ) {
        fun toSynchronizedLine(offsetMs: Int): Lyrics.Synchronized.Line? {
            val mm = if (tagKey.isEmpty()) 0 else tagKey.toPositiveIntOrMinusOne()
            return if (mm < 0 || content == null) {
                null
            } else {
                val ios = tagValue.indexOfFirst { it == '.' || it == ':' }
                val startMs = if (ios >= 0) {
                    val ss = tagValue.toPositiveIntOrMinusOne(end = ios).takeIfPositive { return null }
                    val fracStr = tagValue.substring(ios + 1)
                    val fractionMs = when {
                        fracStr.isEmpty() -> 0
                        fracStr.all { it in '0'..'9' } -> when (fracStr.length) {
                            1 -> (fracStr[0] - '0') * 100
                            2 -> (fracStr[0] - '0') * 100 + (fracStr[1] - '0') * 10
                            else -> (fracStr[0] - '0') * 100 + (fracStr[1] - '0') * 10 + (fracStr[2] - '0')
                        }
                        else -> 0
                    }
                    mm * 60 * 1000 + ss * 1000 + fractionMs - offsetMs
                } else {
                    val ss = tagValue.toPositiveIntOrMinusOne().takeIfPositive { return null }
                    mm * 60 * 1000 + ss * 1000 - offsetMs
                }
                val cleanContent = INLINE_TIMESTAMP_REGEX.replace(content, "").trim()
                Lyrics.Synchronized.Line(
                    startMs.milliseconds,
                    cleanContent,
                )
            }
        }
    }

    fun toSynchronized(): Lyrics.Synchronized {
        val offsetMs = entries
            .filter { it.tagKey.equals("offset", ignoreCase = true) }
            .mapNotNull { it.tagValue.toIntOrNull() }
            .sum()
        val lines = entries
            .mapNotNull { it.toSynchronizedLine(offsetMs) }
            .sortedBy { it.start }
        if (lines.isEmpty()) {
            throw ParseException("Empty lines in Lrc", 0)
        }
        return Lyrics.Synchronized(lines)
    }

    companion object {

        private const val BOM = '\uFEFF'

        private fun parseEntries(lyrics: String, start: Int, end: Int, destination: MutableList<Entry>) {
            var tagStart = start
            while (tagStart < end && (lyrics[tagStart].isWhitespace() || lyrics[tagStart] == BOM)) {
                tagStart++
            }
            if (tagStart == end) return

            if (lyrics[tagStart] != '[') {
                throw ParseException("Line '${lyrics.subSequence(start, end)}' doesn't match", 0)
            }

            data class Tag(val key: String, val value: String)
            val tags = mutableListOf<Tag>()
            var curr = tagStart
            var lastTagEnd = tagStart

            while (curr < end && lyrics[curr] == '[') {
                val nextBracket = lyrics.indexOf(']', curr + 1)
                if (nextBracket !in (curr + 1)..<end) break
                val colon = lyrics.indexOf(':', curr + 1)
                if (colon !in (curr + 1)..<nextBracket) break

                val tagKey = lyrics.substringTrimmed(curr + 1, colon)
                val tagValue = lyrics.substringTrimmed(colon + 1, nextBracket)
                tags.add(Tag(tagKey, tagValue))
                lastTagEnd = nextBracket

                var nextTagStart = nextBracket + 1
                while (nextTagStart < end && (lyrics[nextTagStart] == ' ' || lyrics[nextTagStart] == '\t')) {
                    nextTagStart++
                }
                if (nextTagStart < end && lyrics[nextTagStart] == '[') {
                    val nb = lyrics.indexOf(']', nextTagStart + 1)
                    val nc = lyrics.indexOf(':', nextTagStart + 1)
                    if (nb in (nextTagStart + 1)..<end && nc in (nextTagStart + 1)..<nb) {
                        curr = nextTagStart
                        continue
                    }
                }
                break
            }

            if (tags.isEmpty()) {
                throw ParseException("Line '${lyrics.subSequence(start, end)}' doesn't match", 0)
            }

            val content = lyrics.substringTrimmed(lastTagEnd + 1, end).ifEmpty { null }
            for (tag in tags) {
                destination.add(Entry(tag.key, tag.value, content))
            }
        }

        private fun parseEntry(lyrics: String, start: Int, end: Int): Entry? {
            val list = mutableListOf<Entry>()
            parseEntries(lyrics, start, end, list)
            return list.firstOrNull()
        }

        fun of(lyrics: String): LrcFile {
            val entries = ArrayList<Entry>(lyrics.countAllLines())
            lyrics.forAllLines { start, end ->
                parseEntries(lyrics, start, end, entries)
            }
            if (entries.isEmpty()) {
                throw ParseException("Empty Lrc", 0)
            }
            return LrcFile(lyrics, entries)
        }
    }
}
