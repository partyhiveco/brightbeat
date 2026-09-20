package com.example.vosclone.ui.calibration

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosclone.engine.TimingCalibrationStore
import com.example.vosclone.ui.theme.Brass
import com.example.vosclone.ui.theme.Ink
import com.example.vosclone.ui.theme.InkElevated
import com.example.vosclone.ui.theme.InkElevated2
import com.example.vosclone.ui.theme.Ivory
import com.example.vosclone.ui.theme.IvoryMuted
import com.example.vosclone.ui.theme.ScreenTitle
import com.example.vosclone.ui.theme.PopCyan
import com.example.vosclone.ui.theme.PopLavender
import com.example.vosclone.ui.theme.PopPink
import com.example.vosclone.ui.theme.PopYellow
import com.example.vosclone.ui.theme.SignalOrange
import com.example.vosclone.ui.theme.SignalTeal
import kotlinx.coroutines.delay

/**
 * A short tap-along metronome. Positive offsets move chart targets later,
 * compensating for the combined audio-output and touch pipeline delay.
 */
@Composable
fun CalibrationScreen(
    currentOffsetMs: Long,
    onSave: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val tone = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 88) }.getOrNull()
    }
    var runId by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var beatNumber by remember { mutableIntStateOf(0) }
    var awaitingTap by remember { mutableStateOf(false) }
    var lastBeatAtMs by remember { mutableLongStateOf(0L) }
    var pulseNonce by remember { mutableIntStateOf(0) }
    var chosenOffsetMs by remember {
        mutableLongStateOf(currentOffsetMs.coerceIn(TimingCalibrationStore.MIN_OFFSET_MS, TimingCalibrationStore.MAX_OFFSET_MS))
    }
    val samples = remember { mutableStateListOf<Long>() }
    val pulseScale = remember { Animatable(1f) }
    val measuredMedian = samples.sorted().medianOrNull()

    DisposableEffect(tone) {
        onDispose { tone?.release() }
    }

    LaunchedEffect(pulseNonce) {
        if (pulseNonce == 0) return@LaunchedEffect
        pulseScale.snapTo(1.14f)
        pulseScale.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        running = true
        beatNumber = 0
        awaitingTap = false
        lastBeatAtMs = 0L
        repeat(12) { index ->
            delay(if (index == 0) 850L else 600L)
            val beatAt = SystemClock.elapsedRealtime()
            beatNumber = index + 1
            lastBeatAtMs = beatAt
            awaitingTap = true
            pulseNonce += 1
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
        }
        awaitingTap = false
        running = false
    }

    LaunchedEffect(measuredMedian) {
        measuredMedian?.let { chosenOffsetMs = it.coerceIn(TimingCalibrationStore.MIN_OFFSET_MS, TimingCalibrationStore.MAX_OFFSET_MS) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF15265E), Color(0xFF0A1237), Ink),
                    startY = 0f,
                    endY = size.height
                )
            )
            drawCircle(
                color = PopCyan.copy(alpha = 0.12f),
                radius = size.width * 0.78f,
                center = Offset(size.width * 0.9f, size.height * 0.12f)
            )
            drawCircle(PopPink.copy(alpha = 0.09f), size.width * 0.6f, Offset(size.width * 0.02f, size.height * 0.7f))
            drawCircle(PopLavender.copy(alpha = 0.08f), size.width * 0.5f, Offset(size.width * 0.96f, size.height * 0.88f))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TIMING CALIBRATION", style = ScreenTitle, color = Ivory)
            Text(
                "Make the beep and your tap land together. We’ll use the average offset for every song on this device.",
                color = IvoryMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier.scale(pulseScale.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    drawCircle(
                        color = PopPink.copy(alpha = 0.2f),
                        radius = size.minDimension * 0.38f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    drawCircle(
                        color = PopYellow,
                        radius = size.minDimension * 0.22f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            Text(
                if (running) "BEAT ${beatNumber.coerceAtLeast(1)} / 12" else "READY WHEN YOU ARE",
                color = PopPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = {
                    if (awaitingTap && lastBeatAtMs > 0L) {
                        val delta = (SystemClock.elapsedRealtime() - lastBeatAtMs)
                            .coerceIn(TimingCalibrationStore.MIN_OFFSET_MS, TimingCalibrationStore.MAX_OFFSET_MS)
                        samples += delta
                        awaitingTap = false
                    }
                },
                enabled = awaitingTap,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PopPink, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                Text("TAP THE BEAT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "${samples.size} / 12 taps captured${if (samples.isNotEmpty()) "  •  latest ${formatOffset(samples.last())}" else ""}",
                color = IvoryMuted,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xE3233872), Color(0xEF0D1741))))
                    .border(1.dp, PopCyan.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DEVICE OFFSET", color = PopCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(formatOffset(chosenOffsetMs), color = Ivory, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                Text("Fine-tune after the tap test if needed.", color = IvoryMuted, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { chosenOffsetMs = (chosenOffsetMs - 5L).coerceAtLeast(TimingCalibrationStore.MIN_OFFSET_MS) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PopCyan.copy(alpha = 0.55f))
                    ) { Text("−5 MS", color = PopCyan, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { chosenOffsetMs = (chosenOffsetMs + 5L).coerceAtMost(TimingCalibrationStore.MAX_OFFSET_MS) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PopCyan.copy(alpha = 0.55f))
                    ) { Text("+5 MS", color = PopCyan, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { samples.clear(); runId += 1 },
                enabled = !running,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PopCyan, contentColor = Ink),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (samples.isEmpty()) "START TAP TEST" else "RUN AGAIN", fontWeight = FontWeight.ExtraBold) }
            OutlinedButton(
                onClick = { onSave(chosenOffsetMs) },
                enabled = !running && (samples.size >= 3 || chosenOffsetMs != currentOffsetMs),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PopYellow.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
            ) { Text("SAVE & RETURN", color = PopYellow, fontWeight = FontWeight.ExtraBold) }
            OutlinedButton(
                onClick = onCancel,
                enabled = !running,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            ) { Text("CANCEL", color = IvoryMuted, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun List<Long>.medianOrNull(): Long? {
    if (isEmpty()) return null
    val ordered = sorted()
    val middle = ordered.size / 2
    return if (ordered.size % 2 == 1) ordered[middle] else (ordered[middle - 1] + ordered[middle]) / 2L
}

private fun formatOffset(value: Long): String = when {
    value > 0L -> "+${value} ms"
    else -> "${value} ms"
}
