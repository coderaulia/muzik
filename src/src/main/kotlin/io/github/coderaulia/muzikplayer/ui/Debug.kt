package io.github.coderaulia.muzikplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.coderaulia.muzikplayer.APP_EXECUTION_START
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

@Composable
fun LogFirstDraw(tag: String) {
    var firstDraw by remember(tag) { mutableStateOf(true) }
    Canvas(Modifier) {
        if (firstDraw) {
            firstDraw = false
            logger.debug { "First draw of $tag took: ${Clock.System.now() - APP_EXECUTION_START}" }
        }
    }
}
