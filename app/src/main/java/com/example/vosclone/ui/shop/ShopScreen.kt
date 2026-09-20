package com.example.vosclone.ui.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import com.example.vosclone.ui.components.BrightBeatActionLabel
import com.example.vosclone.ui.components.BrightBeatIcon
import com.example.vosclone.ui.components.BrightBeatScreenHeader
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

/** Commerce hub for passes, song packs, and cosmetic rewards. */
@Composable
fun ShopScreen(onNavigate: (RootDestination) -> Unit) {
    var notice by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        ConcertBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                // Keep content below the status bar and above the touch-friendly nav.
                .padding(top = 40.dp, bottom = 128.dp)
        ) {
            BrightBeatScreenHeader(
                title = "BRIGHTBEAT SHOP",
                meta = "FREE LISTENER",
                metaColor = PopYellow,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            OfferCard(
                title = "BRIGHTBEAT PASS",
                price = "$4.99 / MONTH",
                detail = "All current songs while active • ad-free play • subscriber cosmetics",
                accent = PopPink,
                cta = "PREVIEW PASS",
                actionIcon = BrightBeatIcon.Pass,
                showEqualizer = true
            ) { notice = "PLACEHOLDER — Play Billing will connect to BrightBeat Pass here." }

            Spacer(modifier = Modifier.height(15.dp))
            SectionHeading("SONG PACKS // KEEP FOREVER", PopCyan)
            Spacer(modifier = Modifier.height(9.dp))

            OfferCard(
                title = "NIGHT DRIVE // PACK 01",
                price = "$2.99 ONE-TIME",
                detail = "5 songs • permanent ownership • works without a subscription",
                accent = PopCyan,
                cta = "PREVIEW PACK",
                actionIcon = BrightBeatIcon.Pack,
                imageRes = R.drawable.night_drive_pack_cover,
                showEqualizer = true
            ) { notice = "PLACEHOLDER — Pack entitlement will connect to this offer." }

            Spacer(modifier = Modifier.height(14.dp))
            OfferCard(
                title = "STARTER BUNDLE",
                price = "$1.99 ONE-TIME",
                detail = "3 songs + a first-clear cosmetic badge for new listeners",
                accent = PopYellow,
                cta = "PREVIEW STARTER",
                actionIcon = BrightBeatIcon.Spark,
                badge = "✦"
            ) { notice = "PLACEHOLDER — Starter pack purchase will connect here." }

            Spacer(modifier = Modifier.height(18.dp))
            RewardPanel(onViewRewards = { notice = "PLACEHOLDER — Reward missions will appear after the first clear." })
            notice?.let {
                Text(
                    it,
                    color = PopYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }

        SignalBottomNav(
            selected = RootDestination.SHOP,
            onSelect = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ConcertBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.brightbeat_stage_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.98f)
        )
        // A soft navy veil keeps the panels readable without flattening the stage lights.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x44050A2A),
                        Color(0x20081738),
                        Color(0x8E050A25),
                        Color(0xF507102E)
                    )
                )
            )
        )
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.46f)) {
            drawCircle(
                color = PopPink.copy(alpha = 0.16f),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.04f, size.height * 0.25f)
            )
            drawCircle(
                color = PopCyan.copy(alpha = 0.11f),
                radius = size.width * 0.66f,
                center = Offset(size.width * 1.02f, size.height * 0.33f)
            )
        }
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
            letterSpacing = 0.55.sp
        )
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .height(1.dp)
                .weight(1f)
                .background(accent.copy(alpha = 0.82f))
        )
    }
}

@Composable
private fun OfferCard(
    title: String,
    price: String,
    detail: String,
    accent: Color,
    cta: String,
    actionIcon: BrightBeatIcon,
    imageRes: Int? = null,
    badge: String? = null,
    showEqualizer: Boolean = false,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.34f),
                        Color(0xEF0B1744),
                        Color(0xF508102F)
                    )
                )
            )
            .border(1.5.dp, accent.copy(alpha = 0.96f), RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize().alpha(0.44f)) {
            if (showEqualizer) {
                val barWidth = 6.dp.toPx()
                val gap = 4.dp.toPx()
                val heights = listOf(0.24f, 0.44f, 0.3f, 0.72f, 0.5f, 0.9f, 0.4f, 0.65f)
                val startX = size.width - (barWidth + gap) * heights.size - 16.dp.toPx()
                heights.forEachIndexed { index, fraction ->
                    val h = size.height * fraction * 0.52f
                    drawRoundRect(
                        color = PopCyan.copy(alpha = 0.55f),
                        topLeft = Offset(startX + index * (barWidth + gap), size.height - h - 84.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
            }
            badge?.let {
                drawCircle(accent.copy(alpha = 0.25f), radius = 48.dp.toPx(), center = Offset(46.dp.toPx(), 45.dp.toPx()))
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                imageRes?.let { resource ->
                    Image(
                        painter = painterResource(resource),
                        contentDescription = "$title cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, accent, RoundedCornerShape(16.dp))
                    )
                }
                badge?.let {
                    Text(
                        it,
                        color = accent,
                        fontSize = 42.sp,
                        lineHeight = 42.sp,
                        modifier = Modifier.padding(end = 10.dp, top = 4.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (imageRes != null) 13.dp else 0.dp)
                ) {
                    Text(
                        title,
                        color = Ivory,
                        fontSize = 19.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 0.1.sp
                    )
                    Text(
                        detail,
                        color = IvoryMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
                Text(
                    price,
                    color = accent,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 13.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
            ) {
                BrightBeatActionLabel(
                    icon = actionIcon,
                    label = cta,
                    contentColor = Ink
                )
            }
        }
    }
}

@Composable
private fun RewardPanel(onViewRewards: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xEA25205F), Color(0xF00A1238))))
            .border(BorderStroke(1.dp, PopLavender.copy(alpha = 0.72f)), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "BRIGHTBEAT TOKENS",
                color = Ivory,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
            Text("000", color = PopYellow, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            "Cosmetic rewards only — earn them from first clears, daily missions, and streaks.",
            color = IvoryMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        OutlinedButton(
            onClick = onViewRewards,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PopYellow.copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            BrightBeatActionLabel(
                icon = BrightBeatIcon.Rewards,
                label = "VIEW REWARD TRACK",
                contentColor = PopYellow
            )
        }
    }
}
