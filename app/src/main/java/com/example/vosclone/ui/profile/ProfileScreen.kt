package com.example.vosclone.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosclone.R
import com.example.vosclone.engine.PlayerProgress
import com.example.vosclone.ui.components.BrightBeatActionLabel
import com.example.vosclone.ui.components.BrightBeatIcon
import com.example.vosclone.ui.components.BrightBeatScreenHeader
import com.example.vosclone.ui.menu.HomeLayoutMode
import com.example.vosclone.ui.navigation.RootDestination
import com.example.vosclone.ui.navigation.SignalBottomNav
import com.example.vosclone.ui.theme.Brass
import com.example.vosclone.ui.theme.Ink
import com.example.vosclone.ui.theme.Ivory
import com.example.vosclone.ui.theme.IvoryMuted
import com.example.vosclone.ui.theme.PopCyan
import com.example.vosclone.ui.theme.PopLavender
import com.example.vosclone.ui.theme.PopPink
import com.example.vosclone.ui.theme.PopYellow

/** Player hub for progress, entitlements, rewards, and timing calibration. */
@Composable
fun ProfileScreen(
    progress: PlayerProgress,
    homeLayoutMode: HomeLayoutMode,
    onHomeLayoutChange: (HomeLayoutMode) -> Unit,
    onNavigate: (RootDestination) -> Unit,
    onCalibrate: () -> Unit
) {
    var notice by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        ProfileBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                // Keep content clear of transparent system bars and bottom navigation.
                .padding(top = 40.dp, bottom = 128.dp)
        ) {
            BrightBeatScreenHeader(
                title = "PLAYER PROFILE",
                meta = "ID // 0001",
                metaColor = PopCyan,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            ProfileHero(progress)

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeading("HOME STYLE", PopPink)
            Spacer(modifier = Modifier.height(9.dp))
            HomeLayoutOption(
                mode = HomeLayoutMode.CLASSIC,
                selected = homeLayoutMode == HomeLayoutMode.CLASSIC,
                title = "CLASSIC BRIGHTBEAT",
                subtitle = "Profile header, poster hero, and roomy song cards",
                onSelect = { onHomeLayoutChange(HomeLayoutMode.CLASSIC) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            HomeLayoutOption(
                mode = HomeLayoutMode.COMPACT,
                selected = homeLayoutMode == HomeLayoutMode.COMPACT,
                title = "COMPACT SETLIST",
                subtitle = "Fast catalogue browsing with a larger readable type scale",
                onSelect = { onHomeLayoutChange(HomeLayoutMode.COMPACT) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeading("ACCESS", PopCyan)
            Spacer(modifier = Modifier.height(9.dp))
            StatusRow("BRIGHTBEAT PASS", "NOT ACTIVE", PopYellow)
            Spacer(modifier = Modifier.height(7.dp))
            StatusRow("SONG PACKS", "0 OWNED", IvoryMuted)

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { notice = "PLACEHOLDER — Restore purchases will connect to Google Play Billing." },
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PopPink, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(55.dp)
            ) {
                BrightBeatActionLabel(
                    icon = BrightBeatIcon.Restore,
                    label = "RESTORE PURCHASES",
                    contentColor = Color.White
                )
            }
            OutlinedButton(
                onClick = { notice = "PLACEHOLDER — Account and subscription management will open here." },
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, PopYellow.copy(alpha = 0.84f)),
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 10.dp)
            ) {
                BrightBeatActionLabel(
                    icon = BrightBeatIcon.Manage,
                    label = "MANAGE BRIGHTBEAT PASS",
                    contentColor = PopYellow
                )
            }
            OutlinedButton(
                onClick = onCalibrate,
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, PopCyan.copy(alpha = 0.86f)),
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 10.dp)
            ) {
                BrightBeatActionLabel(
                    icon = BrightBeatIcon.Calibrate,
                    label = "CALIBRATE TIMING",
                    contentColor = PopCyan
                )
            }
            notice?.let {
                Text(
                    it,
                    color = Brass,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }

        SignalBottomNav(
            selected = RootDestination.PROFILE,
            onSelect = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeLayoutOption(
    mode: HomeLayoutMode,
    selected: Boolean,
    title: String,
    subtitle: String,
    onSelect: () -> Unit
) {
    val accent = if (mode == HomeLayoutMode.CLASSIC) PopPink else PopCyan
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else Color(0xB8162450))
            .border(1.dp, accent.copy(alpha = if (selected) 0.95f else 0.55f), RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ivory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = IvoryMuted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Text(
            if (selected) "ON" else "USE",
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ProfileBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.brightbeat_stage_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.96f)
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x4A050A2A),
                        Color(0x27081738),
                        Color(0x93050A25),
                        Color(0xF707102E)
                    )
                )
            )
        )
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.44f)) {
            drawCircle(
                color = PopCyan.copy(alpha = 0.13f),
                radius = size.width * 0.74f,
                center = Offset(size.width * 0.96f, size.height * 0.24f)
            )
            drawCircle(
                color = PopPink.copy(alpha = 0.1f),
                radius = size.width * 0.62f,
                center = Offset(size.width * 0.04f, size.height * 0.48f)
            )
        }
    }
}

@Composable
private fun ProfileHero(progress: PlayerProgress) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xEE382574), Color(0xF10D1948), Color(0xF507102F))
                )
            )
            .border(1.5.dp, PopPink.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize().alpha(0.55f)) {
            drawCircle(
                color = PopPink.copy(alpha = 0.18f),
                radius = size.width * 0.46f,
                center = Offset(size.width * 0.95f, size.height * 0.2f)
            )
            drawCircle(
                color = PopCyan.copy(alpha = 0.2f),
                radius = 52.dp.toPx(),
                center = Offset(55.dp.toPx(), size.height * 0.5f)
            )
            drawLine(
                color = PopCyan.copy(alpha = 0.5f),
                start = Offset(22.dp.toPx(), size.height - 20.dp.toPx()),
                end = Offset(size.width - 20.dp.toPx(), 22.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerBadge()
                Column(modifier = Modifier.padding(start = 15.dp)) {
                    Text(
                        "FREE LISTENER  //  LIVE PROFILE",
                        color = PopYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 0.35.sp
                    )
                    Text(
                        "${progress.songs.values.count { it.mastery >= 85 }} SONGS MASTERED",
                        color = Ivory,
                        fontSize = 23.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "BRIGHTBEAT TOKENS  •  000",
                    color = PopCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("LV ${progress.level.toString().padStart(2, '0')}", color = PopLavender, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun PlayerBadge() {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(Color(0xD90B1237))
            .border(2.dp, PopCyan, RoundedCornerShape(38.dp))
            .border(1.dp, PopPink.copy(alpha = 0.9f), RoundedCornerShape(38.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            drawCircle(color = PopLavender.copy(alpha = 0.14f), radius = size.minDimension * 0.47f)
            drawLine(
                color = PopYellow,
                start = Offset(size.width * 0.5f, 2.dp.toPx()),
                end = Offset(size.width * 0.5f, size.height - 2.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = PopYellow,
                start = Offset(2.dp.toPx(), size.height * 0.5f),
                end = Offset(size.width - 2.dp.toPx(), size.height * 0.5f),
                strokeWidth = 2.dp.toPx()
            )
        }
        Text("✦", color = PopYellow, fontSize = 31.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SectionHeading(label: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.7.sp
        )
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .height(1.dp)
                .weight(1f)
                .background(accent.copy(alpha = 0.78f))
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xC51A2B60))
            .border(1.dp, valueColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Ivory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
