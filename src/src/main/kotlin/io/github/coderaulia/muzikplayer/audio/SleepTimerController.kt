package io.github.coderaulia.muzikplayer.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

object SleepTimerController {
    private val _remainingDuration = MutableStateFlow<Duration?>(null)
    val remainingDuration: StateFlow<Duration?> = _remainingDuration.asStateFlow()

    private val _volumeMultiplier = MutableStateFlow(1.0f)
    val volumeMultiplier: StateFlow<Float> = _volumeMultiplier.asStateFlow()

    private var timerJob: Job? = null

    fun computeFadeMultiplier(remaining: Duration, fadeWindow: Duration = 30.seconds): Float {
        if (remaining <= Duration.ZERO) return 0.0f
        if (remaining >= fadeWindow || fadeWindow <= Duration.ZERO) return 1.0f
        val fraction = (remaining / fadeWindow).toFloat()
        return fraction.coerceIn(0.0f, 1.0f)
    }

    @Synchronized
    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingDuration.value = null
        _volumeMultiplier.value = 1.0f
    }

    @Synchronized
    fun start(
        scope: CoroutineScope,
        duration: Duration,
        onFadeTick: suspend (Float) -> Unit = {},
        onExpired: suspend () -> Unit,
    ) {
        timerJob?.cancel()
        if (duration <= Duration.ZERO) {
            cancel()
            scope.launch { onExpired() }
            return
        }

        _remainingDuration.value = duration
        _volumeMultiplier.value = computeFadeMultiplier(duration)

        timerJob = scope.launch {
            val startTime = TimeSource.Monotonic.markNow()
            val totalDuration = duration
            val tickInterval = 250.milliseconds
            while (isActive) {
                val elapsed = startTime.elapsedNow()
                val remaining = (totalDuration - elapsed).coerceAtLeast(Duration.ZERO)
                _remainingDuration.value = remaining
                val mult = computeFadeMultiplier(remaining)
                if (_volumeMultiplier.value != mult) {
                    _volumeMultiplier.value = mult
                    onFadeTick(mult)
                }
                if (remaining <= Duration.ZERO) {
                    break
                }
                delay(tickInterval.coerceAtMost(remaining))
            }
            try {
                onExpired()
            } finally {
                _remainingDuration.value = null
                _volumeMultiplier.value = 1.0f
            }
        }
    }
}
