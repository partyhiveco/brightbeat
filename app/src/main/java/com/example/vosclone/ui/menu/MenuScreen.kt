package com.example.vosclone.ui.menu

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import com.example.vosclone.audio.AudioMetadata
import com.example.vosclone.audio.AudioMetadataLoader
import com.example.vosclone.chart.Chart
import com.example.vosclone.R
import com.example.vosclone.ui.components.BrightBeatLogo
import com.example.vosclone.ui.components.BrightBeatLogoVariant
import com.example.vosclone.ui.navigation.RootDestination
import com.example.vosclone.ui.navigation.SignalBottomNav
import com.example.vosclone.ui.theme.Ink
import com.example.vosclone.ui.theme.Ivory
import com.example.vosclone.ui.theme.IvoryMuted
import com.example.vosclone.ui.theme.PopCyan
import com.example.vosclone.ui.theme.PopLavender
import com.example.vosclone.ui.theme.PopPink
import com.example.vosclone.ui.theme.PopYellow
import com.example.vosclone.ui.theme.SongTitle

/**
 * The pop/beat home surface. The concert artwork is a full-screen raster stage
 * while the play action remains backed by the real chart/session flow.
 */
@Composable
fun MenuScreen(
    charts: List<Chart>,
    onSelectChart: (Chart) -> Unit,
    onNavigate: (RootDestination) -> Unit
) {
    val context = LocalContext.current
    val audioFiles = remember(charts) { charts.map { it.audioFile } }
    val audioMetadata by produceState<Map<String, AudioMetadata>>(
        initialValue = emptyMap(),
        context,
        audioFiles
    ) {
        value = AudioMetadataLoader.loadAll(context, audioFiles)
    }
    var selectedAudioFile by remember(charts) { mutableStateOf(charts.firstOrNull()?.audioFile) }
    val selectedBaseChart = charts.firstOrNull { it.audioFile == selectedAudioFile } ?: charts.firstOrNull()
    val selectedChart = selectedBaseChart?.withAudioMetadata(audioMetadata[selectedBaseChart.audioFile])

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        StageBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Keep the transparent Android status bar, but move the foreground
                // header below its live inset. The stage artwork remains edge-to-edge.
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                // Let the setlist run almost to the floating navigation pill. The
                // pill remains clear of the final row while the lower song text can
                // reach the same visual vanishing point as the reference.
                // Keep the last song rows visible almost to the floating nav;
                // the nav itself remains layered above the scroll surface.
                .padding(bottom = 48.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            ProfileHeader()
            BrandLockup()

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                // Leave enough scroll tail for the final song pill to clear the
                // floating nav overlay instead of ending underneath it.
                contentPadding = PaddingValues(top = 5.dp, bottom = 80.dp)
            ) {
                selectedChart?.let { chart ->
                    item {
                        // The hero is intentionally narrower than the setlist: the
                        // target composition gives the artwork room to breathe and
                        // leaves the supporting songs visible in the first viewport.
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FeaturedSongCard(
                                chart = chart,
                                metadata = audioMetadata[chart.audioFile],
                                onSelect = { selectedAudioFile = chart.audioFile },
                                onPlay = { onSelectChart(chart) }
                            )
                        }
                    }
                }
                charts.drop(1).forEachIndexed { index, chart ->
                    val displayChart = chart.withAudioMetadata(audioMetadata[chart.audioFile])
                    item(key = chart.audioFile) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            PreviewSongRow(
                                chart = displayChart,
                                metadata = audioMetadata[chart.audioFile],
                                accent = listOf(PopCyan, PopPink, PopYellow, PopLavender)[index % 4],
                                selected = chart.audioFile == selectedAudioFile,
                                onSelect = { selectedAudioFile = chart.audioFile },
                                onPlay = { onSelectChart(displayChart) }
                            )
                        }
                    }
                }
            }
        }

        SignalBottomNav(
            selected = RootDestination.SETLIST,
            onSelect = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun StageBackdrop() {
    // Keep the generated stage as the actual full-screen surface. It carries
    // the truss, haze, crowd lights, word-art, and side posters as one
    // perspective-locked composition, so Compose content only needs to sit on
    // top of the negative space and never recreate those details.
    val stageMotion = rememberInfiniteTransition(label = "stage-motion")
    val sweepPhase by stageMotion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5600, easing = LinearEasing)),
        label = "light-sweep"
    )
    val pulse by stageMotion.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(tween(2200), repeatMode = RepeatMode.Reverse),
        label = "stage-pulse"
    )
    val wind by stageMotion.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), repeatMode = RepeatMode.Reverse),
        label = "confetti-wind"
    )
    val confettiMotion by stageMotion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6800, easing = LinearEasing)),
        label = "confetti-fall"
    )
    val confetti = androidx.compose.runtime.remember {
        val random = Random(3107)
        List(34) {
            FallingConfetti(
                x = random.nextFloat(),
                size = 3.5f + random.nextFloat() * 5.5f,
                phase = random.nextFloat(),
                drift = 8f + random.nextFloat() * 24f,
                spin = 0.7f + random.nextFloat() * 1.8f,
                color = listOf(PopPink, PopCyan, PopYellow, PopLavender)[random.nextInt(4)]
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.brightbeat_stage_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            // The travel value is derived from a full sine cycle, so the
            // final frame and first frame share the exact same light positions.
            val sweepAngle = sweepPhase * 6.2831855f
            val travel = (sin(sweepAngle.toDouble()).toFloat() + 1f) * 0.5f
            // Purple starts at the top-right, glides to the top-left, and
            // returns to the exact starting point through one cosine cycle.
            val purpleTravel = (cos(sweepAngle.toDouble()).toFloat() + 1f) * 0.5f
            val purpleSourceX = size.width * (0.12f + purpleTravel * 0.78f)
            val sweepX = size.width * travel
            val beamBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    PopCyan.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.22f),
                    PopPink.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                start = Offset(sweepX, 0f),
                end = Offset(sweepX + size.width * 0.30f, size.height)
            )
            drawRect(beamBrush)
            drawCircle(
                color = PopCyan.copy(alpha = pulse),
                radius = size.width * 0.34f,
                center = Offset(size.width * (0.12f + travel * 0.30f), size.height * 0.18f)
            )
            drawCircle(
                color = PopPink.copy(alpha = pulse * 0.9f),
                radius = size.width * 0.30f,
                center = Offset(size.width * (0.88f - travel * 0.26f), size.height * 0.24f)
            )
            drawCircle(
                color = PopLavender.copy(alpha = pulse * 0.82f),
                radius = size.width * 0.18f,
                center = Offset(purpleSourceX, size.height * 0.07f)
            )

            // Wide, translucent spotlight cones make the generated stage feel
            // alive. Every beam is anchored at the top edge, like a stage rig.
            val cyanBeamX = size.width * (0.10f + travel * 0.84f)
            val pinkBeamX = size.width * (0.84f - travel * 0.66f)
            drawLine(
                color = PopCyan.copy(alpha = 0.13f),
                start = Offset(cyanBeamX, 0f),
                end = Offset(cyanBeamX - size.width * 0.34f, size.height * 0.86f),
                strokeWidth = size.width * 0.16f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = PopPink.copy(alpha = 0.11f),
                start = Offset(pinkBeamX, 0f),
                end = Offset(pinkBeamX + size.width * 0.30f, size.height * 0.78f),
                strokeWidth = size.width * 0.13f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = PopLavender.copy(alpha = 0.17f),
                start = Offset(purpleSourceX, -size.height * 0.03f),
                end = Offset(
                    size.width * (0.42f + purpleTravel * 0.16f),
                    size.height * 0.84f
                ),
                strokeWidth = size.width * 0.11f,
                cap = StrokeCap.Round
            )
            confetti.forEach { bit ->
                // All particles share the same periodic phase; their unique
                // offsets, drift, and spin keep the field lively without a
                // discontinuity when the global loop wraps.
                val fall = (confettiMotion + bit.phase) % 1f
                val wave = (fall + bit.phase) * 6.283f
                val x = bit.x * size.width +
                    sin(wave.toDouble()).toFloat() * bit.drift +
                    wind * bit.drift * 0.55f
                val y = fall * (size.height + 44f) - 22f
                val spinAngle = wave * bit.spin
                val tail = bit.size * (1.7f + sin(spinAngle.toDouble()).toFloat() * 0.45f)

                // Three linked segments give each streamer a soft, fluid trail
                // instead of a single identical falling dash.
                var previous = Offset(x, y)
                for (trailIndex in 1..3) {
                    val trailFall = (fall - trailIndex * 0.035f + 1f) % 1f
                    val trailWave = (trailFall + bit.phase) * 6.283f
                    val trailX = bit.x * size.width +
                        sin(trailWave.toDouble()).toFloat() * bit.drift +
                        wind * bit.drift * 0.55f
                    val trailY = trailFall * (size.height + 44f) - 22f
                    val alpha = 0.68f * (1f - trailIndex / 4f)
                    drawLine(
                        color = bit.color.copy(alpha = alpha),
                        start = Offset(trailX, trailY),
                        end = previous,
                        strokeWidth = bit.size * (1.05f - trailIndex * 0.12f),
                        cap = StrokeCap.Round
                    )
                    previous = Offset(trailX, trailY)
                }
                drawLine(
                    color = bit.color.copy(alpha = 0.92f),
                    start = Offset(x, y),
                    end = Offset(
                        x + cos(spinAngle.toDouble()).toFloat() * tail,
                        y + sin(spinAngle.toDouble()).toFloat() * tail
                    ),
                    strokeWidth = bit.size,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private data class FallingConfetti(
    val x: Float,
    val size: Float,
    val phase: Float,
    val drift: Float,
    val spin: Float,
    val color: Color
)

@Composable
private fun ProfileHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(57.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(PopCyan.copy(alpha = 0.95f), radius = size.minDimension / 2f)
                    drawCircle(Color(0xFF071033), radius = size.minDimension / 2f - 4f)
                    drawCircle(PopPink.copy(alpha = 0.45f), radius = size.minDimension / 2f - 8f)
                    drawCircle(Color(0xFF17194B), radius = size.minDimension / 2f - 11f)
                    drawLine(PopCyan, Offset(size.width * 0.28f, size.height * 0.65f), Offset(size.width * 0.72f, size.height * 0.36f), 3f, cap = StrokeCap.Round)
                }
                Text("✦", color = PopYellow, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.padding(start = 9.dp)) {
                Text("LV. 32", color = PopPink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("NEXT 1,240 EXP", color = Ivory, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Box(modifier = Modifier.padding(top = 5.dp).width(83.dp).height(5.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF26325C))) {
                    Box(modifier = Modifier.fillMaxWidth(0.64f).fillMaxSize().background(PopCyan))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.End) {
            CurrencyChip("♥", "10/10", PopPink)
            CurrencyChip("◇", "1,350", PopCyan)
        }
    }
}

@Composable
private fun CurrencyChip(icon: String, amount: String, accent: Color) {
    Row(
        modifier = Modifier
            .width(103.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0D1740))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(amount, color = Ivory, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 7.dp))
        Text("+", color = Ivory, fontSize = 18.sp, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun BrandLockup() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrightBeatLogo(
            variant = BrightBeatLogoVariant.Brand,
            contentDescription = "BRIGHTBEAT"
        )
    }
}

@Composable
private fun FeaturedSongCard(
    chart: Chart,
    metadata: AudioMetadata?,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val cardMotion = rememberInfiniteTransition(label = "featured-card-motion")
    val bob by cardMotion.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "card-bob"
    )
    val glow by cardMotion.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(1400), repeatMode = RepeatMode.Reverse),
        label = "card-glow"
    )
    val shimmer by cardMotion.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "card-shimmer"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = bob }
            .clip(RoundedCornerShape(25.dp))
            .clickable(onClick = onSelect)
            .background(Ivory)
            .border(2.dp, PopPink.copy(alpha = 0.75f + glow * 0.25f), RoundedCornerShape(25.dp))
    ) {
        Column {
            // Keep the cover's native 584:510 ratio instead of stretching it into
            // a wide banner; this is the compact poster proportion in the target.
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(584f / 510f)) {
                CoverArtwork(chart = chart, metadata = metadata)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val shineX = size.width * shimmer
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            start = Offset(shineX - size.width * 0.24f, 0f),
                            end = Offset(shineX + size.width * 0.24f, size.height)
                        )
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        chart.title,
                        color = Color(0xFF111937),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        chart.artist.ifBlank { "Unknown artist" },
                        color = Color(0xFF59638C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 7.dp)
                        .width(62.dp)
                        .border(1.dp, Color(0xFFB7BEDA), RoundedCornerShape(0.dp))
                        .padding(start = 7.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("BPM ${chart.bpm}", color = Color(0xFF29345E), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        formatDuration(metadata?.durationMs) ?: songDuration(chart, metadata),
                        color = Color(0xFF29345E),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Text("★★★★☆", color = PopPink, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 1.dp))
                }
                Text("♡", color = Color(0xFF29345E), fontSize = 23.sp, modifier = Modifier.padding(start = 3.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Brush.horizontalGradient(listOf(PopPink, Color(0xFFFF1684), PopPink.copy(alpha = 0.9f))))
                    .clickable(onClick = onPlay)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dotColor = Color.White.copy(alpha = 0.12f)
                    var x = 10f
                    while (x < size.width) {
                        var y = 6f
                        while (y < size.height) {
                            drawCircle(dotColor, radius = 1.3f, center = Offset(x, y))
                            y += 8f
                        }
                        x += 8f
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("▶", color = Color.White, fontSize = 20.sp)
                    Text("PLAY NOW  ››", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CoverArtwork(chart: Chart, metadata: AudioMetadata?) {
    val embeddedCover: ImageBitmap? = remember(metadata?.embeddedCover) {
        metadata?.embeddedCover?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    when {
        embeddedCover != null -> Image(
            bitmap = embeddedCover,
            contentDescription = "${chart.title} cover",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        fallbackCoverResource(chart.audioFile) != null -> Image(
            painter = painterResource(fallbackCoverResource(chart.audioFile)!!),
            contentDescription = "${chart.title} cover",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        else -> FallbackCoverArtwork(chart)
    }
}

@Composable
private fun FallbackCoverArtwork(chart: Chart) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val number = chart.audioFile.filter { it.isDigit() }.toIntOrNull()?.minus(1)
        val palette = fallbackCoverPalettes[
            (number?.takeIf { it >= 0 } ?: (chart.audioFile.hashCode() and Int.MAX_VALUE)) %
                fallbackCoverPalettes.size
        ]
        drawRect(brush = Brush.verticalGradient(listOf(palette.top, palette.bottom)))
        drawCircle(
            color = palette.accent.copy(alpha = 0.90f),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.72f, size.height * 0.26f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.24f),
            radius = size.minDimension * 0.10f,
            center = Offset(size.width * 0.30f, size.height * 0.30f)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.72f),
            start = Offset(size.width * 0.06f, size.height * 0.78f),
            end = Offset(size.width * 0.94f, size.height * 0.40f),
            strokeWidth = size.minDimension * 0.07f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = palette.accent.copy(alpha = 0.70f),
            start = Offset(size.width * 0.14f, size.height * 0.16f),
            end = Offset(size.width * 0.86f, size.height * 0.86f),
            strokeWidth = size.minDimension * 0.025f,
            cap = StrokeCap.Round
        )
    }
}

private fun fallbackCoverResource(audioFile: String): Int? = when (audioFile) {
    "demo1.mp3" -> R.drawable.vos_starlight_cover
    "demo2.mp3" -> R.drawable.thumb_blue_tomorrow
    "demo3.mp3" -> R.drawable.thumb_neon_parade
    "demo4.mp3" -> R.drawable.thumb_blooming_signal
    "demo5.mp3" -> R.drawable.night_drive_pack_cover
    else -> null
}

private data class ArtworkPalette(val top: Color, val bottom: Color, val accent: Color)

private val fallbackCoverPalettes = listOf(
    ArtworkPalette(Color(0xFF0B1F58), Color(0xFF4A0D73), PopCyan),
    ArtworkPalette(Color(0xFF172A67), Color(0xFF6B174F), PopPink),
    ArtworkPalette(Color(0xFF3E1D68), Color(0xFF101947), PopYellow),
    ArtworkPalette(Color(0xFF123B66), Color(0xFF27124F), PopLavender),
    ArtworkPalette(Color(0xFF3B214F), Color(0xFF0A2A55), PopCyan),
    ArtworkPalette(Color(0xFF53204B), Color(0xFF15205A), PopPink),
    ArtworkPalette(Color(0xFF173E5A), Color(0xFF351957), PopYellow),
    ArtworkPalette(Color(0xFF2A275D), Color(0xFF0A3552), PopLavender)
)

@Composable
private fun PreviewSongRow(
    chart: Chart,
    metadata: AudioMetadata?,
    accent: Color,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val rowAccent = if (selected) PopPink else accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onSelect)
            .background(if (selected) Color(0xEE27275F) else Color(0xD9142550))
            .border(if (selected) 2.dp else 1.dp, rowAccent.copy(alpha = 0.84f), RoundedCornerShape(17.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(11.dp))) {
            CoverArtwork(chart = chart, metadata = metadata)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(
                chart.title,
                color = Ivory,
                style = SongTitle,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chart.artist.ifBlank { "Unknown" },
                color = IvoryMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(modifier = Modifier.width(1.dp).height(42.dp).background(accent.copy(alpha = 0.42f)).padding(end = 8.dp))
        Column(modifier = Modifier.padding(start = 10.dp, end = 8.dp)) {
            Text("BPM  ${chart.bpm}", color = Ivory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(formatDuration(metadata?.durationMs) ?: songDuration(chart, metadata), color = IvoryMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
            Text(songRating(chart), color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 1.dp))
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .border(1.5.dp, Ivory, RoundedCornerShape(50))
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Ivory, fontSize = 13.sp)
        }
    }
}

private fun Chart.withAudioMetadata(metadata: AudioMetadata?): Chart = metadata?.let {
    copy(
        title = it.title ?: title,
        artist = it.artist ?: artist
    )
} ?: this

private fun formatDuration(durationMs: Long?): String? {
    val totalSeconds = durationMs?.let { ((it + 500L) / 1000L).toInt() } ?: return null
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

private fun songDuration(chart: Chart, metadata: AudioMetadata?): String = formatDuration(metadata?.durationMs) ?: when (chart.audioFile) {
    "demo2.mp3" -> "2:36"
    "demo3.mp3" -> "2:10"
    "demo4.mp3" -> "2:27"
    else -> "2:18"
}

private fun songRating(chart: Chart): String = when {
    chart.title.equals("Neon Parade", ignoreCase = true) -> "★★★★★"
    chart.title.equals("Blooming Signal", ignoreCase = true) -> "★★★★☆"
    else -> "★★★★☆"
}
