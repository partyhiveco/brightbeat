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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.example.vosclone.chart.ChartDifficulty
import com.example.vosclone.engine.PlayerProgress
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
    progress: PlayerProgress,
    layoutMode: HomeLayoutMode,
    onToggleFavorite: (String) -> Unit,
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
    var activeFilter by remember { mutableStateOf(CatalogueFilter.ALL) }
    val filteredCharts = charts.filter { chart ->
        when (activeFilter) {
            CatalogueFilter.ALL -> true
            CatalogueFilter.FAVORITES -> chart.audioFile in progress.favorites
            CatalogueFilter.RECENT -> chart.audioFile == progress.recentSong
            CatalogueFilter.OWNED -> chart.owned
            CatalogueFilter.PACKS -> !chart.owned
            CatalogueFilter.EASY -> chart.difficulty == ChartDifficulty.EASY
            CatalogueFilter.MEDIUM -> chart.difficulty == ChartDifficulty.NORMAL
            CatalogueFilter.HARD -> chart.difficulty == ChartDifficulty.HARD
        }
    }
    val selectedBaseChart = filteredCharts.firstOrNull { it.audioFile == selectedAudioFile }
        ?: filteredCharts.firstOrNull()
    val selectedChart = selectedBaseChart?.withAudioMetadata(audioMetadata[selectedBaseChart.audioFile])

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        StageBackdrop(quietCatalogue = layoutMode == HomeLayoutMode.COMPACT)

        val onFilterSelect: (CatalogueFilter) -> Unit = { activeFilter = it }
        if (layoutMode == HomeLayoutMode.CLASSIC) {
            ClassicHomeContent(
                charts = charts,
                progress = progress,
                filteredCharts = filteredCharts,
                selectedChart = selectedChart,
                selectedAudioFile = selectedAudioFile,
                audioMetadata = audioMetadata,
                activeFilter = activeFilter,
                onFilterSelect = onFilterSelect,
                onSelectAudioFile = { selectedAudioFile = it },
                onToggleFavorite = onToggleFavorite,
                onSelectChart = onSelectChart,
                onNavigate = onNavigate
            )
        } else {
            CompactHomeContent(
                filteredCharts = filteredCharts,
                selectedChart = selectedChart,
                selectedAudioFile = selectedAudioFile,
                progress = progress,
                audioMetadata = audioMetadata,
                activeFilter = activeFilter,
                onFilterSelect = onFilterSelect,
                onSelectAudioFile = { selectedAudioFile = it },
                onToggleFavorite = onToggleFavorite,
                onSelectChart = onSelectChart,
                onNavigate = onNavigate
            )
        }

        SignalBottomNav(
            selected = RootDestination.SETLIST,
            onSelect = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ClassicHomeContent(
    charts: List<Chart>,
    progress: PlayerProgress,
    filteredCharts: List<Chart>,
    selectedChart: Chart?,
    selectedAudioFile: String?,
    audioMetadata: Map<String, AudioMetadata>,
    activeFilter: CatalogueFilter,
    onFilterSelect: (CatalogueFilter) -> Unit,
    onSelectAudioFile: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectChart: (Chart) -> Unit,
    onNavigate: (RootDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(bottom = 48.dp)
    ) {
        Spacer(modifier = Modifier.height(5.dp))
        ProfileHeader(progress)
        BrandLockup(onSettings = { onNavigate(RootDestination.PROFILE) })

        CatalogueFilters(active = activeFilter, onSelect = onFilterSelect)
        SetlistHeader(label = "${filteredCharts.size} / ${charts.size} SONGS")

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(top = 5.dp, bottom = 80.dp)
        ) {
            if (filteredCharts.isEmpty()) item { EmptyCatalogueState() }
            selectedChart?.let { chart ->
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        FeaturedSongCard(
                            chart = chart,
                            metadata = audioMetadata[chart.audioFile],
                            onSelect = { onSelectAudioFile(chart.audioFile) },
                            favorite = chart.audioFile in progress.favorites,
                            onFavorite = { onToggleFavorite(chart.audioFile) },
                            onPlay = { if (chart.owned) onSelectChart(chart) }
                        )
                    }
                }
            }
            filteredCharts.filterNot { it.audioFile == selectedChart?.audioFile }.forEachIndexed { index, chart ->
                val displayChart = chart.withAudioMetadata(audioMetadata[chart.audioFile])
                item(key = chart.audioFile) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        PreviewSongRow(
                            chart = displayChart,
                            metadata = audioMetadata[chart.audioFile],
                            accent = listOf(PopCyan, PopPink, PopYellow, PopLavender)[index % 4],
                            selected = chart.audioFile == selectedAudioFile,
                            favorite = chart.audioFile in progress.favorites,
                            onSelect = { onSelectAudioFile(chart.audioFile) },
                            onFavorite = { onToggleFavorite(chart.audioFile) },
                            onPlay = { if (chart.owned) onSelectChart(displayChart) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactHomeContent(
    filteredCharts: List<Chart>,
    selectedChart: Chart?,
    selectedAudioFile: String?,
    progress: PlayerProgress,
    audioMetadata: Map<String, AudioMetadata>,
    activeFilter: CatalogueFilter,
    onFilterSelect: (CatalogueFilter) -> Unit,
    onSelectAudioFile: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectChart: (Chart) -> Unit,
    onNavigate: (RootDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 48.dp)
    ) {
        HomeBrandHeader(onSettings = { onNavigate(RootDestination.PROFILE) })
        selectedChart?.let { chart ->
            FeaturedSongCard(
                chart = chart,
                metadata = audioMetadata[chart.audioFile],
                largeText = true,
                onSelect = { onSelectAudioFile(chart.audioFile) },
                favorite = chart.audioFile in progress.favorites,
                onFavorite = { onToggleFavorite(chart.audioFile) },
                onPlay = { if (chart.owned) onSelectChart(chart) }
            )
        }
        CatalogueFilters(active = activeFilter, onSelect = onFilterSelect)
        SetlistHeader(label = "${filteredCharts.size} SONGS")
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 64.dp)
        ) {
            if (filteredCharts.isEmpty()) item { EmptyCatalogueState() }
            filteredCharts.forEachIndexed { index, chart ->
                val displayChart = chart.withAudioMetadata(audioMetadata[chart.audioFile])
                item(key = chart.audioFile) {
                    CompactSongRow(
                        chart = displayChart,
                        metadata = audioMetadata[chart.audioFile],
                        accent = listOf(PopCyan, PopPink, PopYellow, PopLavender)[index % 4],
                        selected = chart.audioFile == selectedAudioFile,
                        favorite = chart.audioFile in progress.favorites,
                        onSelect = { onSelectAudioFile(chart.audioFile) },
                        onFavorite = { onToggleFavorite(chart.audioFile) },
                        onPlay = { if (chart.owned) onSelectChart(displayChart) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetlistHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("SETLIST", color = Ivory, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Text(label, color = IvoryMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
    }
}

@Composable
private fun EmptyCatalogueState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 26.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xD90A1538))
            .border(1.dp, PopCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("NO SONGS MATCH THIS FILTER", color = Ivory, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun StageBackdrop(quietCatalogue: Boolean) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.20f to Color(0x4400061A),
                        0.34f to Color(0xD9041028),
                        1f to Color(0xF7020B1D)
                    )
                )
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
        if (quietCatalogue) {
            // The compact setlist needs a quiet reading surface; Classic keeps
            // the brighter poster treatment from the original Home screen.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Transparent,
                0.12f to Color.Transparent,
                0.25f to Color(0xED020D20),
                0.42f to Color(0xFF020D20),
                1f to Color(0xFF020B1C)
            )))
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
private fun ProfileHeader(progress: PlayerProgress) {
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
                Text("LV. ${progress.level}", color = PopPink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("NEXT ${1_000 - progress.levelXp} EXP", color = Ivory, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Box(modifier = Modifier.padding(top = 5.dp).width(83.dp).height(5.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF26325C))) {
                    Box(modifier = Modifier.fillMaxWidth(progress.levelXp / 1_000f).fillMaxSize().background(PopCyan))
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
private fun BrandLockup(onSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        BrightBeatLogo(
            variant = BrightBeatLogoVariant.Brand,
            contentDescription = "BRIGHTBEAT",
            modifier = Modifier.width(278.dp)
        )
        Text(
            text = "⚙",
            color = Ivory,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSettings)
                .padding(8.dp)
        )
    }
}

@Composable
private fun HomeBrandHeader(onSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        BrightBeatLogo(
            variant = BrightBeatLogoVariant.Brand,
            contentDescription = "BRIGHTBEAT",
            modifier = Modifier.fillMaxWidth(0.79f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp)
                .size(25.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Transparent)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", color = Ivory, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeaturedSongCard(
    chart: Chart,
    metadata: AudioMetadata?,
    largeText: Boolean = false,
    onSelect: () -> Unit,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onPlay: () -> Unit
) {
    val cardHeight = if (largeText) 112.dp else 88.dp
    val artworkSize = if (largeText) 96.dp else 76.dp
    val hotFont = if (largeText) 12.sp else 8.sp
    val titleFont = if (largeText) 19.5.sp else 13.sp
    val titleLineHeight = if (largeText) 21.sp else 15.sp
    val artistFont = if (largeText) 13.5.sp else 9.sp
    val metadataFont = if (largeText) 12.sp else 8.sp
    val starFont = if (largeText) 13.5.sp else 9.sp
    val playFont = if (largeText) 18.sp else 14.sp
    val playSize = if (largeText) 45.dp else 37.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .background(Color(0xF205142B))
            .border(1.dp, Brush.horizontalGradient(listOf(PopCyan, PopLavender, PopPink)), RoundedCornerShape(10.dp))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(5.dp))) {
            CoverArtwork(chart = chart, metadata = metadata)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 9.dp)) {
            Text(
                text = if (chart.owned) "HOT 100" else chart.packId.uppercase(),
                color = Ink,
                fontSize = hotFont,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(PopYellow).padding(horizontal = 6.dp, vertical = 1.dp)
            )
            Text(chart.title, color = Ivory, fontSize = titleFont, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = titleLineHeight, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            Text(chart.artist.ifBlank { "Unknown artist" }, color = IvoryMuted, fontSize = artistFont, maxLines = 1)
            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("BPM ${chart.bpm}", color = IvoryMuted, fontSize = metadataFont, fontWeight = FontWeight.Bold)
                Text("   |   ${formatDuration(metadata?.durationMs) ?: songDuration(chart, metadata)}", color = IvoryMuted, fontSize = metadataFont)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                starsFor(chart),
                color = if (favorite) PopYellow else PopPink,
                fontSize = starFont,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable(onClick = onFavorite)
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 2.dp)
                    .size(playSize)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, if (chart.owned) PopCyan else IvoryMuted, RoundedCornerShape(50))
                    .clickable(enabled = chart.owned, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Text(if (chart.owned) "▶" else "◆", color = if (chart.owned) PopCyan else PopYellow, fontSize = playFont)
            }
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
    favorite: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onPlay: () -> Unit
) {
    val rowAccent = if (selected) PopCyan else Color(0xFF31506B)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onSelect)
            .background(if (selected) Color(0xFF081A30) else Color(0xFF051326))
            .border(0.6.dp, rowAccent.copy(alpha = if (selected) 0.45f else 0.42f), RoundedCornerShape(7.dp))
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(5.dp))) {
            CoverArtwork(chart = chart, metadata = metadata)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 7.dp)) {
            Text(
                chart.title,
                color = if (chart.owned) Ivory else IvoryMuted,
                style = SongTitle,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chart.artist.ifBlank { "Unknown" },
                color = IvoryMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        Text(formatDuration(metadata?.durationMs) ?: songDuration(chart, metadata), color = IvoryMuted, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 5.dp))
        Text(
            starsFor(chart),
            color = if (favorite) PopYellow else if (chart.owned) PopPink else PopPink.copy(alpha = 0.64f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(49.dp).clickable(onClick = onFavorite)
        )
        if (chart.owned) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.5.dp, PopCyan, RoundedCornerShape(50))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = PopCyan, fontSize = 11.sp)
            }
        } else {
            Row(
                modifier = Modifier.width(68.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("▣", color = IvoryMuted, fontSize = 12.sp)
                Text(chart.packId, color = IvoryMuted, fontSize = 7.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun CompactSongRow(
    chart: Chart,
    metadata: AudioMetadata?,
    accent: Color,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onPlay: () -> Unit
) {
    val rowAccent = if (selected) PopCyan else accent.copy(alpha = 0.70f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onSelect)
            .background(if (selected) Color(0xFF0A1E3A) else Color(0xF205142B))
            .border(1.dp, rowAccent.copy(alpha = if (selected) 0.82f else 0.52f), RoundedCornerShape(9.dp))
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))) {
            CoverArtwork(chart = chart, metadata = metadata)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
            Text(
                chart.title,
                color = if (chart.owned) Ivory else IvoryMuted,
                style = SongTitle,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chart.artist.ifBlank { "Unknown" },
                color = IvoryMuted,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        Text(
            formatDuration(metadata?.durationMs) ?: songDuration(chart, metadata),
            color = IvoryMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Text(
            starsFor(chart),
            color = if (favorite) PopYellow else if (chart.owned) PopPink else PopPink.copy(alpha = 0.64f),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(66.dp).clickable(onClick = onFavorite)
        )
        if (chart.owned) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.8.dp, PopCyan, RoundedCornerShape(50))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = PopCyan, fontSize = 16.sp)
            }
        } else {
            Row(
                modifier = Modifier.width(80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("▣", color = IvoryMuted, fontSize = 15.sp)
                Text(chart.packId, color = IvoryMuted, fontSize = 10.sp, maxLines = 2)
            }
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

private fun starsFor(chart: Chart): String = "★".repeat(chart.stars.coerceIn(1, 5)) +
    "☆".repeat((5 - chart.stars).coerceAtLeast(0))

private enum class CatalogueFilter(val label: String) {
    ALL("ALL"), FAVORITES("FAVORITES"), RECENT("RECENT"), OWNED("OWNED"), PACKS("PACKS"),
    EASY("EASY"), MEDIUM("MEDIUM"), HARD("HARD")
}

@Composable
private fun CatalogueFilters(
    active: CatalogueFilter,
    onSelect: (CatalogueFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CatalogueFilter.entries.forEach { filter ->
            val selected = filter == active
            val minWidth = when (filter) {
                CatalogueFilter.ALL -> 58.dp
                CatalogueFilter.FAVORITES -> 108.dp
                CatalogueFilter.RECENT -> 82.dp
                CatalogueFilter.OWNED -> 78.dp
                CatalogueFilter.PACKS -> 72.dp
                CatalogueFilter.EASY -> 68.dp
                CatalogueFilter.MEDIUM -> 86.dp
                CatalogueFilter.HARD -> 70.dp
            }
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = minWidth)
                    .height(34.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) PopCyan else Color(0xE8071735))
                    .border(1.dp, if (selected) PopCyan else IvoryMuted.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    color = if (selected) Ink else Ivory,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun SongAccessStrip(chart: Chart, dark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${chart.difficulty.label.uppercase()}  •  LV ${chart.level}",
            color = if (dark) PopCyan else Color(0xFF394B83),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            if (chart.owned) starsFor(chart) else "◆ ${chart.packId.uppercase()}",
            color = if (chart.owned) PopPink else PopYellow,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}
