package com.example.vosclone.ui.gameplay

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosclone.audio.AudioEngine
import com.example.vosclone.R
import com.example.vosclone.chart.Chart
import com.example.vosclone.engine.GameSession
import com.example.vosclone.engine.FeedbackEvent
import com.example.vosclone.engine.FeedbackKind
import com.example.vosclone.engine.Judgement
import com.example.vosclone.engine.LOOKAHEAD_MS
import com.example.vosclone.engine.NoteState
import com.example.vosclone.ui.theme.Brass
import com.example.vosclone.ui.theme.Ink
import com.example.vosclone.ui.theme.InkElevated2
import com.example.vosclone.ui.theme.Ivory
import com.example.vosclone.ui.theme.IvoryMuted
import com.example.vosclone.ui.theme.JudgeGood
import com.example.vosclone.ui.theme.JudgeMiss
import com.example.vosclone.ui.theme.JudgePerfect
import com.example.vosclone.ui.theme.LaneColors
import com.example.vosclone.ui.theme.LaneNames
import com.example.vosclone.ui.theme.PopCyan
import com.example.vosclone.ui.theme.PopLavender
import com.example.vosclone.ui.theme.PopPink
import com.example.vosclone.ui.theme.PopYellow
import com.example.vosclone.ui.theme.SignalOrange
import com.example.vosclone.ui.theme.SignalRed
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/** Current relaxed pacing while the game is being learned. */
private const val BEGINNER_PLAYBACK_SPEED = 0.82f
private const val SUSTAIN_FLOW_PERIOD_MS = 680L
private const val SUSTAIN_FLOW_STREAK_COUNT = 4
private const val SUSTAIN_EMBER_COUNT = 14
private const val SUSTAIN_GLINT_PERIOD_MS = 320L
private const val TWO_PI = 6.2831855f

// Keep the stage name consistent with the branded home card while preserving
// the chart's real title for loading, timing, and scoring.
private fun displayChartTitle(chart: Chart): String =
    if (chart.title.equals("Demo Track 1", ignoreCase = true)) "STARLIGHT AGAIN" else chart.title.uppercase()

/** Renders the falling-note play field and follows the real audio clock. */
@Composable
fun GameplayScreen(
    chart: Chart,
    assetAudioPath: String,
    timingOffsetMs: Long = 0L,
    playbackSpeed: Float = BEGINNER_PLAYBACK_SPEED,
    onFinished: (GameSession) -> Unit,
    onQuit: () -> Unit
) {
    val context = LocalContext.current
    val session = remember(chart, timingOffsetMs) { GameSession(chart, timingOffsetMs) }
    val audioEngine = remember { AudioEngine(context) }

    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var audioDurationMs by remember { mutableLongStateOf(0L) }
    var generatedFallbackNotes by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var showQuitConfirmation by remember { mutableStateOf(false) }
    var feedbackJudgement by remember { mutableStateOf<Judgement?>(null) }
    var feedbackNonce by remember { mutableIntStateOf(0) }
    var heldLanes by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val heldPointers = remember { mutableMapOf<PointerId, Int>() }
    val feedbackAlpha = remember { Animatable(0f) }
    val feedbackLift = remember { Animatable(0f) }
    val lanePulseAlphas = remember(chart.lanes) { List(chart.lanes) { Animatable(0f) } }

    LaunchedEffect(chart, playbackSpeed) {
        audioEngine.loadAssetTrack(assetAudioPath)
        audioEngine.setPlaybackSpeed(playbackSpeed)
        audioEngine.play()
        started = true
    }

    // A confirmation keeps the audio clock and scoring state stable while the
    // player decides. The existing frame loop remains the sole owner of note
    // timing, so dismissing the popup resumes the same run without retiming it.
    LaunchedEffect(showQuitConfirmation) {
        if (showQuitConfirmation) {
            audioEngine.pause()
        } else if (started && !finished) {
            audioEngine.play()
        }
    }

    BackHandler {
        showQuitConfirmation = !showQuitConfirmation
    }

    // Completion is tied to ExoPlayer's STATE_ENDED, not the last chart note.
    // This prevents short charts from cutting off a much longer song.
    LaunchedEffect(started) {
        if (!started) return@LaunchedEffect
        while (!finished) {
            withFrameNanos {
                val positionMs = audioEngine.currentPositionMs()
                currentTimeMs = positionMs

                // Media3 exposes the real duration after preparation. If the
                // bundled chart stops before that duration, extend it on a BPM
                // grid so the play field never silently goes blank mid-song.
                if (audioDurationMs == 0L) {
                    val duration = audioEngine.durationMs()
                    if (duration > 0L) {
                        audioDurationMs = duration
                        generatedFallbackNotes = session.scheduler.extendToAudioDuration(duration)
                    }
                }

                session.tickHolds(positionMs, heldLanes)
                session.tickAutoMiss(positionMs)
                if (!finished && audioEngine.isEnded() && session.scheduler.isChartComplete(positionMs, session.totalTimingOffsetMs)) {
                    finished = true
                    audioEngine.pause()
                    onFinished(session)
                }
            }
        }
    }

    // Drain every event, rather than only the latest lane, so chords get one
    // pulse per lane and hold ticks can provide continuous tactile feedback.
    LaunchedEffect(session.feedbackSerial) {
        val events = session.drainFeedbackEvents()
        if (events.isEmpty()) return@LaunchedEffect
        kotlinx.coroutines.coroutineScope {
            events.forEach { event ->
                if (event.kind == FeedbackKind.JUDGEMENT && event.judgement != null) {
                    feedbackJudgement = event.judgement
                    feedbackNonce += 1
                }
                if (event.lane in lanePulseAlphas.indices) {
                    launch {
                        lanePulseAlphas[event.lane].snapTo(1f)
                        lanePulseAlphas[event.lane].animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                    }
                }
                if (event.haptic) {
                    playGameplayHaptic(context, event)
                }
            }
        }
    }

    LaunchedEffect(feedbackNonce) {
        if (feedbackNonce == 0) return@LaunchedEffect
        kotlinx.coroutines.coroutineScope {
            launch {
                feedbackAlpha.snapTo(1f)
                feedbackAlpha.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
            }
            launch {
                feedbackLift.snapTo(0f)
                feedbackLift.animateTo(-68f, tween(520, easing = FastOutSlowInEasing))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { audioEngine.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Image(
            // Keep gameplay on the same neon concert rig as the home surface,
            // but use its clean center composition so the note path stays clear.
            painter = painterResource(R.drawable.vos_concert_backdrop_v2),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize().alpha(0.98f)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    // A very light veil keeps copy and notes readable while
                    // allowing the beams, truss, banners, and crowd to stay
                    // visible behind the transparent playfield.
                    colors = listOf(Color(0x18071343), Color(0x0807102F), Color(0x24070D29)),
                    startY = 0f,
                    endY = size.height
                )
            )
            // Small color blooms tie the glass HUD into the stage lights.
            drawCircle(PopPink.copy(alpha = 0.07f), size.width * 0.44f, Offset(size.width * 0.04f, size.height * 0.12f))
            drawCircle(PopCyan.copy(alpha = 0.07f), size.width * 0.52f, Offset(size.width * 0.98f, size.height * 0.38f))

            // Sparse angular confetti catches the light around the stage,
            // without adding a fixed centerpiece behind the falling notes.
            repeat(10) { index ->
                val angle = index * 0.93f
                val radiusX = size.width * (0.24f + (index % 4) * 0.07f)
                val radiusY = size.height * (0.16f + (index % 3) * 0.055f)
                val x = size.width * 0.5f + cos(angle) * radiusX
                val y = size.height * 0.46f + sin(angle) * radiusY
                val accent = when (index % 3) {
                    0 -> PopPink
                    1 -> PopCyan
                    else -> PopYellow
                }
                drawRoundRect(
                    color = accent.copy(alpha = 0.42f),
                    topLeft = Offset(x, y),
                    size = Size(6f + (index % 2) * 2f, 13f + (index % 3) * 3f),
                    cornerRadius = CornerRadius(2f)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Keep the transparent status bar while keeping the HUD below
                // the device's live top inset. Keep the bottom lane controls
                // above the transparent navigation bar as well.
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Give the transparent system status row a little breathing room;
            // the target HUD floats below the chrome instead of touching it.
            Spacer(modifier = Modifier.height(9.dp))
            HudBar(
                chart = chart,
                session = session,
                generatedFallbackNotes = generatedFallbackNotes,
                playbackSpeed = playbackSpeed
            )
            PlayField(
                chart = chart,
                session = session,
                currentTimeMs = currentTimeMs,
                lanePulseAlphas = lanePulseAlphas.map { it.value },
                heldLanes = heldLanes,
                onPointerDown = { _, lane ->
                    val judgement = session.onTap(lane, audioEngine.currentPositionMs())
                    if (judgement != null) {
                        // Fire at pointer-down so the tap never waits for a frame.
                        playGameplayHaptic(context, FeedbackEvent(FeedbackKind.JUDGEMENT, lane, judgement))
                    }
                    val isHold = judgement != null && judgement != Judgement.MISS && session.hasActiveHold(lane)
                    isHold
                },
                onPointerUp = { _, lane ->
                    session.releaseHold(lane, audioEngine.currentPositionMs())
                },
                onHoldPointerChanged = { pointerId, lane, isDown ->
                    if (isDown) {
                        heldPointers[pointerId] = lane
                    } else {
                        heldPointers.remove(pointerId)
                    }
                    heldLanes = heldPointers.values.toSet()
                },
                onHoldPointerMoved = { pointerId, lane, isWithinTolerance ->
                    if (isWithinTolerance) {
                        heldPointers[pointerId] = lane
                    } else {
                        heldPointers.remove(pointerId)
                    }
                    heldLanes = heldPointers.values.toSet()
                }
            )
        }

        feedbackJudgement?.let { judgement ->
            Text(
                text = judgement.label().uppercase(),
                color = judgement.color(),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = feedbackLift.value.dp)
                    .alpha(feedbackAlpha.value)
            )
        }

        if (showQuitConfirmation) {
            QuitConfirmationOverlay(
                onStay = { showQuitConfirmation = false },
                onQuit = onQuit
            )
        }
    }
}

/** Branded, modal quit confirmation shown above the live concert playfield. */
@Composable
private fun QuitConfirmationOverlay(
    onStay: () -> Unit,
    onQuit: () -> Unit
) {
    val overlayInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Keep the stage and system-bar composition visible through a
            // deep veil, matching the glass treatment used by the HUD.
            .background(Color(0xB9070B2B))
            .clickable(
                interactionSource = overlayInteraction,
                indication = null,
                onClick = { /* Modal surface: tapping outside keeps it open. */ }
            )
            .semantics { contentDescription = "Quit game confirmation" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .widthIn(max = 690.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xF20B1745), Color(0xF2171A53), Color(0xF20B1745))
                    )
                )
                .border(
                    width = 2.5.dp,
                    brush = Brush.horizontalGradient(listOf(PopCyan, PopLavender, PopPink)),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 24.dp, vertical = 25.dp)
                .semantics { contentDescription = "Quit game" }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("✦", color = PopCyan, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "QUIT GAME?",
                        color = Ivory,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 9.dp)
                    )
                    Text("✦", color = PopPink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(0.78f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.weight(1f).height(2.dp).background(PopCyan.copy(alpha = 0.78f)))
                    Text("✦", color = PopLavender, fontSize = 17.sp)
                    Box(Modifier.weight(1f).height(2.dp).background(PopPink.copy(alpha = 0.68f)))
                }
                Text(
                    text = "Leave this song? Your current\nrun will be lost.",
                    color = Ivory,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    QuitActionButton(
                        text = "STAY",
                        accent = PopCyan,
                        fill = Brush.linearGradient(
                            listOf(Color(0xAA102B5C), Color(0xCC102B5C))
                        ),
                        onClick = onStay,
                        modifier = Modifier.weight(1f)
                    )
                    QuitActionButton(
                        text = "YES, QUIT",
                        accent = PopPink,
                        fill = Brush.horizontalGradient(listOf(PopPink, Color(0xFFFF1584))),
                        onClick = onQuit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuitActionButton(
    text: String,
    accent: Color,
    fill: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(fill)
            .border(2.dp, accent, RoundedCornerShape(34.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (accent == PopPink) Color.White else PopCyan,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun HudBar(
    chart: Chart,
    session: GameSession,
    generatedFallbackNotes: Int,
    playbackSpeed: Float
) {
    val comboScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(session.combo) {
        if (session.combo > 0) {
            comboScale.snapTo(1.28f)
            comboScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xF207163D), Color(0xF21C2B61), Color(0xF207163D))
                )
            )
            .border(1.5.dp, PopCyan.copy(alpha = 0.9f), RoundedCornerShape(9.dp))
            .padding(horizontal = 17.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayTitle = displayChartTitle(chart)
        val hudTitle = if (displayTitle == "STARLIGHT AGAIN") {
            "NOW PLAYING  //  STARLIGHT\nAGAIN"
        } else {
            "NOW PLAYING  //  $displayTitle"
        }
        Column(modifier = Modifier.weight(1f).padding(end = 3.dp)) {
            Text(
                hudTitle,
                color = Ivory,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
            Text(
                text = if (generatedFallbackNotes > 0) {
                    "FULL TRACK // AUTO-MAPPED"
                } else {
                    "LIVE // SYNCED TO AUDIO"
                },
                color = if (generatedFallbackNotes > 0) SignalRed else SignalOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (playbackSpeed < 0.99f) {
                Text(
                    text = "BEGINNER // ${(playbackSpeed * 100f).roundToInt()}% TEMPO",
                    color = Brass,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        // The target HUD reads as one concert-console panel: a slim light
        // divider separates the track identity from the live stat tiles.
        Spacer(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(1.dp)
                .height(62.dp)
                .background(PopCyan.copy(alpha = 0.72f))
        )
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(55.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color(0xA8152A60))
                .border(1.5.dp, PopCyan.copy(alpha = 0.78f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SCORE", color = IvoryMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("${session.score}", color = PopCyan, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(55.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(PopPink.copy(alpha = 0.26f))
                .border(1.5.dp, PopPink.copy(alpha = 0.9f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("COMBO", color = IvoryMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${session.combo}",
                    color = PopYellow,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.scale(comboScale.value)
                )
            }
        }
    }
}

private fun Judgement.label() = when (this) {
    Judgement.PERFECT -> "Perfect"
    Judgement.GOOD -> "Good"
    Judgement.MISS -> "Miss"
}

private fun Judgement.color() = when (this) {
    Judgement.PERFECT -> JudgePerfect
    Judgement.GOOD -> JudgeGood
    Judgement.MISS -> JudgeMiss
}

private fun Float?.orZero(): Float = this ?: 0f

/** Short, hardware-tuned ticks instead of Compose's long-press buzz. */
private fun playGameplayHaptic(context: Context, event: FeedbackEvent) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return

    val effect = when (event.kind) {
        FeedbackKind.HOLD_TICK -> VibrationEffect.createOneShot(8L, 55)
        FeedbackKind.HOLD_COMPLETE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(28L, 210)
        }
        FeedbackKind.JUDGEMENT -> when (event.judgement) {
            Judgement.PERFECT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(12L, 180)
            }
            Judgement.GOOD -> VibrationEffect.createOneShot(20L, 125)
            Judgement.MISS, null -> VibrationEffect.createOneShot(26L, 60)
        }
    }
    runCatching { vibrator.vibrate(effect) }
}

@Composable
private fun PlayField(
    chart: Chart,
    session: GameSession,
    currentTimeMs: Long,
    lanePulseAlphas: List<Float>,
    heldLanes: Set<Int>,
    onPointerDown: (PointerId, Int) -> Boolean,
    onPointerUp: (PointerId, Int) -> Unit,
    onHoldPointerChanged: (PointerId, Int, Boolean) -> Unit,
    onHoldPointerMoved: (PointerId, Int, Boolean) -> Unit
) {
    val laneCount = chart.lanes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(laneCount) {
                awaitEachGesture {
                    val activeHoldPointers = mutableMapOf<PointerId, Int>()
                    var event = awaitPointerEvent(PointerEventPass.Main)
                    while (true) {
                        val laneWidth = size.width / laneCount
                        event.changes.forEach { change ->
                            val lane = (change.position.x / laneWidth).toInt().coerceIn(0, laneCount - 1)
                            if (change.pressed && !change.previousPressed) {
                                // Dispatch on DOWN (not UP) so the hit feels immediate and multi-touch is parallel.
                                if (onPointerDown(change.id, lane)) {
                                    activeHoldPointers[change.id] = lane
                                    onHoldPointerChanged(change.id, lane, true)
                                }
                                change.consume()
                            } else if (change.pressed && change.previousPressed) {
                                // A hold may drift within 65% of its lane width.
                                // Outside that band we pause its contact, with a
                                // short grace period handled by GameSession.
                                activeHoldPointers[change.id]?.let { holdLane ->
                                    val holdCenter = (holdLane + 0.5f) * laneWidth
                                    val withinTolerance = abs(change.position.x - holdCenter) <= laneWidth * 0.65f
                                    onHoldPointerMoved(change.id, holdLane, withinTolerance)
                                }
                            } else if (!change.pressed && change.previousPressed) {
                                activeHoldPointers.remove(change.id)?.let { holdLane ->
                                    onPointerUp(change.id, holdLane)
                                    onHoldPointerChanged(change.id, holdLane, false)
                                }
                                change.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) {
                            // ACTION_CANCEL can arrive without a normal UP.
                            activeHoldPointers.toList().forEach { (pointerId, holdLane) ->
                                onPointerUp(pointerId, holdLane)
                                onHoldPointerChanged(pointerId, holdLane, false)
                            }
                            activeHoldPointers.clear()
                            break
                        }
                        event = awaitPointerEvent(PointerEventPass.Main)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val laneWidth = size.width / laneCount
            val hitLineY = size.height * 0.82f

            fun yFor(timeMs: Long): Float =
                (1f - ((timeMs - currentTimeMs).toFloat() / LOOKAHEAD_MS.toFloat())) * hitLineY

            // Let the live stage show through the playfield.  The lane wash is
            // deliberately translucent so the beams and crowd remain part of
            // the gameplay read while darkening the note path just enough for
            // the colored notes to pop.
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0x1A071334), Color(0x0C070D27), Color(0x26070A20)),
                    startY = 0f,
                    endY = size.height
                )
            )

            for (i in 0 until laneCount) {
                val laneColor = LaneColors[i % LaneColors.size]
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(laneColor.copy(alpha = 0.018f), laneColor.copy(alpha = 0.11f)),
                        startY = 0f,
                        endY = hitLineY
                    ),
                    topLeft = Offset(i * laneWidth, 0f),
                    size = Size(laneWidth, hitLineY)
                )
            }

            lanePulseAlphas.forEachIndexed { lane, pulseAlpha ->
                if (pulseAlpha > 0f) {
                    drawRect(
                        color = LaneColors[lane % LaneColors.size].copy(alpha = 0.28f * pulseAlpha),
                        topLeft = Offset(lane * laneWidth, 0f),
                        size = Size(laneWidth, size.height)
                    )
                }
            }

            for (i in 0..laneCount) {
                drawLine(
                    color = when (i) {
                        0 -> PopPink.copy(alpha = 0.94f)
                        laneCount -> PopCyan.copy(alpha = 0.94f)
                        else -> Ivory.copy(alpha = 0.48f)
                    },
                    start = Offset(i * laneWidth, 0f),
                    end = Offset(i * laneWidth, size.height),
                    strokeWidth = if (i == 0 || i == laneCount) 2.5f else 1.7f
                )
            }

            // A broad receptor band makes the correct tap target unmistakable.
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(PopPink.copy(alpha = 0.26f), PopYellow.copy(alpha = 0.22f), PopCyan.copy(alpha = 0.26f))
                ),
                topLeft = Offset(0f, hitLineY - 30f),
                size = Size(size.width, 60f)
            )
            drawLine(
                color = Ivory.copy(alpha = 0.55f),
                start = Offset(0f, hitLineY - 5f),
                end = Offset(size.width, hitLineY - 5f),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = PopYellow.copy(alpha = 0.98f),
                start = Offset(0f, hitLineY),
                end = Offset(size.width, hitLineY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            for (i in 0 until laneCount) {
                val center = Offset(i * laneWidth + laneWidth / 2f, hitLineY)
                val pulseAlpha = lanePulseAlphas.getOrElse(i) { 0f }
                val holdProgress = session.holdProgressForLane(i, currentTimeMs)
                if (i in heldLanes) {
                    drawCircle(
                        color = LaneColors[i % LaneColors.size].copy(alpha = 0.32f),
                        radius = 33f,
                        center = center
                    )
                }
                if (holdProgress != null) {
                    drawArc(
                        color = LaneColors[i % LaneColors.size].copy(alpha = 0.95f),
                        startAngle = -90f,
                        sweepAngle = 360f * holdProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - 29f, center.y - 29f),
                        size = Size(58f, 58f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
                drawCircle(color = Color(0xE20B1235), radius = 24f, center = center)
                drawCircle(
                    color = LaneColors[i % LaneColors.size].copy(alpha = 0.95f),
                    radius = 18f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
                if (pulseAlpha > 0f) {
                    drawCircle(
                        color = LaneColors[i % LaneColors.size].copy(alpha = 0.92f * pulseAlpha),
                        radius = 18f + (24f * pulseAlpha),
                        center = center,
                        style = Stroke(width = 3f + (3f * pulseAlpha))
                    )
                }
                drawCircle(color = Ivory.copy(alpha = 0.9f), radius = 4.5f, center = center)
            }

            val notes = session.scheduler.visibleNotes(currentTimeMs, session.totalTimingOffsetMs)
            val activeHoldMarkerY = FloatArray(laneCount) { Float.NaN }
            val activeHoldBarStartY = FloatArray(laneCount) { Float.NaN }
            val activeHoldBarEndY = FloatArray(laneCount) { Float.NaN }
            notes.forEach { runtimeNote ->
                if (runtimeNote.state == NoteState.HOLDING &&
                    runtimeNote.note.type.equals("hold", ignoreCase = true)
                ) {
                    val targetTimeMs = session.targetTimeMs(runtimeNote.note)
                    val headY = yFor(targetTimeMs).coerceIn(0f, size.height)
                    val tailY = yFor(targetTimeMs + runtimeNote.note.durationMs)
                        .coerceIn(0f, size.height)
                    // The hold travels toward the player, so tailY can be
                    // greater than headY. Keep the real visible segment
                    // instead of collapsing negative lengths to zero.
                    val barStartY = minOf(headY, tailY)
                    val barEndY = maxOf(headY, tailY)
                    activeHoldBarStartY[runtimeNote.note.lane] = barStartY
                    activeHoldBarEndY[runtimeNote.note.lane] = barEndY
                    val shimmer = sin(currentTimeMs * 0.012f + runtimeNote.note.lane) * 0.07f
                    val lineFraction = (0.62f + shimmer).coerceIn(0.38f, 0.86f)
                    activeHoldMarkerY[runtimeNote.note.lane] =
                        (barStartY + (barEndY - barStartY) * lineFraction)
                            .coerceIn(0f, size.height)
                }
            }

            notes.forEach { runtimeNote ->
                val targetTimeMs = session.targetTimeMs(runtimeNote.note)
                val msUntilHit = targetTimeMs - currentTimeMs
                val progress = 1f - (msUntilHit.toFloat() / LOOKAHEAD_MS.toFloat())
                val y = progress * hitLineY
                val x = runtimeNote.note.lane * laneWidth
                val laneColor = LaneColors[runtimeNote.note.lane % LaneColors.size]
                // Restore the original wide horizontal note pill. The visual
                // stage pass must not change the recognizable note shape or
                // the player's lane-reading muscle memory.
                val noteHeight = 32f
                val noteWidth = laneWidth * 0.93f
                val topLeft = Offset(x + (laneWidth - noteWidth) / 2f, y - noteHeight / 2f)

                if (runtimeNote.note.type.equals("hold", ignoreCase = true)) {
                    val headY = yFor(targetTimeMs).coerceAtMost(hitLineY)
                    val tailY = yFor(targetTimeMs + runtimeNote.note.durationMs)
                        .coerceIn(0f, size.height)
                    val barWidth = laneWidth * 0.34f
                    drawLine(
                        color = laneColor.copy(alpha = if (runtimeNote.state == NoteState.HOLDING) 0.95f else 0.58f),
                        start = Offset(x + laneWidth / 2f, headY),
                        end = Offset(x + laneWidth / 2f, tailY),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.24f),
                        start = Offset(x + laneWidth / 2f, headY),
                        end = Offset(x + laneWidth / 2f, tailY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    if (runtimeNote.state == NoteState.HOLDING) {
                        val progressY = yFor(
                            targetTimeMs + (runtimeNote.note.durationMs *
                                session.holdProgressForLane(runtimeNote.note.lane, currentTimeMs).orZero()).toLong()
                        ).coerceIn(0f, size.height)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.88f),
                            radius = barWidth * 0.62f,
                            center = Offset(x + laneWidth / 2f, progressY)
                        )
                    }
                }

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(topLeft.x + 3f, topLeft.y + 5f),
                    size = Size(noteWidth, noteHeight),
                    cornerRadius = CornerRadius(noteHeight / 2f)
                )
                drawRoundRect(
                    color = laneColor,
                    topLeft = topLeft,
                    size = Size(noteWidth, noteHeight),
                    cornerRadius = CornerRadius(noteHeight / 2f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.32f),
                    topLeft = Offset(topLeft.x + 5f, topLeft.y + 5f),
                    size = Size(noteWidth - 10f, 7f),
                    cornerRadius = CornerRadius(4f)
                )
            }

            // Guitar Hero-inspired sustain stream: a hot ribbon runs down the
            // entire held bar while comets and embers peel off its edges.
            for (lane in 0 until laneCount) {
                if (!session.hasActiveHold(lane)) continue
                val barStartY = activeHoldBarStartY.getOrNull(lane) ?: continue
                val barEndY = activeHoldBarEndY.getOrNull(lane) ?: continue
                if (barStartY.isNaN() || barEndY.isNaN() || barEndY - barStartY < 6f) continue

                val centerX = lane * laneWidth + laneWidth / 2f
                val color = LaneColors[lane % LaneColors.size]
                val barLength = barEndY - barStartY

                // Layered ribbon: broad color haze, saturated body, and a
                // narrow ivory core make the sustain read as one hot stream.
                drawLine(
                    color = color.copy(alpha = 0.16f),
                    start = Offset(centerX, barStartY),
                    end = Offset(centerX, barEndY),
                    strokeWidth = laneWidth * 0.62f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color.copy(alpha = 0.42f),
                    start = Offset(centerX, barStartY),
                    end = Offset(centerX, barEndY),
                    strokeWidth = laneWidth * 0.28f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Ivory.copy(alpha = 0.48f),
                    start = Offset(centerX, barStartY),
                    end = Offset(centerX, barEndY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Four staggered comets travel along the hold, producing a
                // visible stream rather than a single radial hit burst.
                repeat(SUSTAIN_FLOW_STREAK_COUNT) { streakIndex ->
                    val phase = ((currentTimeMs + lane * 113L + streakIndex *
                        (SUSTAIN_FLOW_PERIOD_MS / SUSTAIN_FLOW_STREAK_COUNT)) %
                        SUSTAIN_FLOW_PERIOD_MS).toFloat() / SUSTAIN_FLOW_PERIOD_MS.toFloat()
                    val flowY = barStartY + phase * barLength
                    val trailLength = 24f + abs(sin(phase * TWO_PI)) * 18f
                    val cometFade = 0.5f + 0.5f * sin(phase * TWO_PI)
                    drawLine(
                        color = color.copy(alpha = 0.56f + 0.24f * cometFade),
                        start = Offset(centerX, flowY - trailLength),
                        end = Offset(centerX, flowY + 5f),
                        strokeWidth = laneWidth * 0.12f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Ivory.copy(alpha = 0.8f),
                        radius = 3.5f + 2f * cometFade,
                        center = Offset(centerX, flowY)
                    )
                    drawLine(
                        color = Ivory.copy(alpha = 0.62f),
                        start = Offset(centerX - laneWidth * 0.18f, flowY),
                        end = Offset(centerX + laneWidth * 0.18f, flowY),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round
                    )
                }

                // Side embers are distributed across the bar so the whole
                // sustain appears to burn, not just its center marker.
                repeat(SUSTAIN_EMBER_COUNT) { emberIndex ->
                    val emberPhase = ((currentTimeMs + lane * 89L + emberIndex * 53L) % 760L)
                        .toFloat() / 760f
                    val railFraction = ((emberIndex / SUSTAIN_EMBER_COUNT.toFloat()) +
                        emberPhase * 0.32f) % 1f
                    val anchorY = barStartY + railFraction * barLength
                    val side = if (emberIndex % 2 == 0) -1f else 1f
                    val rise = 7f + emberPhase * (15f + (emberIndex % 4) * 6f)
                    val drift = side * (8f + (emberIndex % 3) * 4f) +
                        sin(emberPhase * 7f + emberIndex) * 4f
                    val emberHead = Offset(
                        x = centerX + drift,
                        y = anchorY - rise
                    )
                    val emberTail = Offset(
                        x = centerX + drift * 0.24f,
                        y = anchorY + 3f
                    )
                    val emberColor = if (emberIndex % 5 == 0) Ivory else color
                    val emberFade = 1f - emberPhase
                    drawLine(
                        color = emberColor.copy(alpha = 0.8f * emberFade),
                        start = emberTail,
                        end = emberHead,
                        strokeWidth = 1.5f + (emberIndex % 3) * 0.65f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = emberColor.copy(alpha = 0.92f * emberFade),
                        radius = 1.8f + (emberIndex % 3) * 0.7f,
                        center = emberHead
                    )
                }

                // A moving four-point glint sweeps the sustain ribbon.
                val glintPhase = ((currentTimeMs + lane * 61L) % SUSTAIN_GLINT_PERIOD_MS)
                    .toFloat() / SUSTAIN_GLINT_PERIOD_MS.toFloat()
                val glintY = barStartY + glintPhase * barLength
                val glintLength = 8f + 8f * sin(glintPhase * Math.PI.toFloat())
                drawLine(
                    color = Ivory.copy(alpha = 0.88f),
                    start = Offset(centerX - glintLength, glintY),
                    end = Offset(centerX + glintLength, glintY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Ivory.copy(alpha = 0.76f),
                    start = Offset(centerX, glintY - glintLength),
                    end = Offset(centerX, glintY + glintLength),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // The progress marker remains a compact hot spot on the bar,
                // not the origin of the particle system.
                val markerY = activeHoldMarkerY.getOrNull(lane)
                if (markerY != null && !markerY.isNaN()) {
                    drawRoundRect(
                        color = color.copy(alpha = 0.42f),
                        topLeft = Offset(centerX - laneWidth * 0.22f, markerY - 10f),
                        size = Size(laneWidth * 0.44f, 20f),
                        cornerRadius = CornerRadius(10f)
                    )
                    drawRoundRect(
                        color = Ivory.copy(alpha = 0.84f),
                        topLeft = Offset(centerX - laneWidth * 0.14f, markerY - 2f),
                        size = Size(laneWidth * 0.28f, 4f),
                        cornerRadius = CornerRadius(2f)
                    )
                }
            }

            // A bright but low-alpha timing grid keeps the four lanes legible
            // against the moving concert lights.
            for (i in 0..20) {
                val y = i * (size.height / 20f)
                drawLine(
                    color = Ivory.copy(alpha = 0.34f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(laneCount) { lane ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    LaneColors[lane % LaneColors.size].copy(alpha = 0.30f),
                                    InkElevated2.copy(alpha = 0.90f)
                                )
                            )
                        )
                        .border(1.7.dp, LaneColors[lane % LaneColors.size].copy(alpha = 0.88f), RoundedCornerShape(22.dp))
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        LaneNames[lane % LaneNames.size].uppercase(),
                        color = LaneColors[lane % LaneColors.size],
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

    }
}
