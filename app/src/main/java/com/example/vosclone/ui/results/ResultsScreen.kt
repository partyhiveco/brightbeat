package com.example.vosclone.ui.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosclone.R
import com.example.vosclone.engine.GameSession
import com.example.vosclone.ui.components.BrightBeatLogo
import com.example.vosclone.ui.components.BrightBeatLogoVariant
import com.example.vosclone.ui.theme.Ink
import com.example.vosclone.ui.theme.Ivory
import com.example.vosclone.ui.theme.PopCyan
import com.example.vosclone.ui.theme.PopPink
import com.example.vosclone.ui.theme.PopYellow

/** Letter-grade thresholds off weighted accuracy. */
private fun gradeFor(accuracy: Int): String = when {
    accuracy >= 95 -> "S"
    accuracy >= 85 -> "A"
    accuracy >= 70 -> "B"
    accuracy >= 50 -> "C"
    else -> "D"
}

private fun displayChartTitle(title: String): String =
    if (title.equals("Demo Track 1", ignoreCase = true)) "STARLIGHT AGAIN" else title.uppercase()

@Composable
fun ResultsScreen(
    session: GameSession,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        Image(
            painter = painterResource(R.drawable.brightbeat_stage_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        // Preserve the concert through the system bars while keeping every
        // live control and result inside the usable window.
        BoxWithConstraints(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            val unit = maxWidth.value / 428f
            val pageHeight = maxHeight
            val margin = maxWidth * 0.064f
            val neonBorder = Brush.horizontalGradient(listOf(PopCyan, Color(0xFF797BFF), Color(0xFFFF46F4)))

            BrightBeatLogo(
                variant = BrightBeatLogoVariant.Brand,
                contentDescription = "BrightBeat",
                modifier = Modifier.align(Alignment.TopCenter)
                    .padding(top = pageHeight * 0.008f).width(maxWidth * 0.68f)
            )

            Row(
                modifier = Modifier.fillMaxWidth().offset(y = pageHeight * 0.114f)
                    .padding(horizontal = maxWidth * 0.11f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp * unit)
            ) {
                NeonDash(Modifier.weight(1f).height(16.dp * unit))
                Text(
                    "SESSION COMPLETE", color = Ivory, fontSize = (24f * unit).sp,
                    fontWeight = FontWeight.Black, maxLines = 1,
                    style = TextStyle(shadow = Shadow(PopPink, Offset.Zero, 15f))
                )
                NeonDash(Modifier.weight(1f).height(16.dp * unit))
            }

            Text(
                displayChartTitle(session.chart.title),
                modifier = Modifier.fillMaxWidth().offset(y = pageHeight * 0.186f)
                    .padding(horizontal = margin),
                color = Color(0xFFCDD9FF), fontSize = (18f * unit).sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier.align(Alignment.TopCenter)
                    .offset(y = pageHeight * 0.229f).size(maxWidth * 0.365f)
                    .background(
                        Brush.horizontalGradient(listOf(PopPink, Color(0xFFFF795A), PopYellow)),
                        RoundedCornerShape(24.dp * unit)
                    )
                    .border(2.dp, Color(0xFFFFD5E5), RoundedCornerShape(24.dp * unit)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    gradeFor(session.accuracyPercent), color = Color(0xFF050D29),
                    fontSize = (116f * unit).sp, lineHeight = (124f * unit).sp,
                    fontWeight = FontWeight.Black
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().offset(y = pageHeight * 0.445f)
                    .padding(horizontal = margin)
                    .background(
                        Brush.linearGradient(listOf(Color(0xEE102A75), Color(0xF009123C), Color(0xED26105D))),
                        RoundedCornerShape(23.dp * unit)
                    )
                    .border(1.7.dp, neonBorder, RoundedCornerShape(23.dp * unit))
                    .padding(horizontal = 22.dp * unit, vertical = 11.dp * unit)
            ) {
                ResultStat("SCORE", "${session.score}", unit)
                ResultDivider()
                ResultStat("ACCURACY", "${session.accuracyPercent}%", unit)
                ResultDivider()
                ResultStat("MAX COMBO", "${session.maxCombo}", unit)
                ResultDivider()
                ResultStat("PERFECT / GOOD / MISS", "${session.perfectCount} / ${session.goodCount} / ${session.missCount}", unit)
            }

            Row(
                modifier = Modifier.align(Alignment.TopCenter).offset(y = pageHeight * 0.69f)
                    .width(maxWidth * 0.60f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(17.dp * unit)
            ) {
                NeonDash(Modifier.weight(1f).height(20.dp * unit))
                Text("♪", color = PopCyan, fontSize = (37f * unit).sp,
                    style = TextStyle(shadow = Shadow(PopPink, Offset(2f, -2f), 14f)))
                NeonDash(Modifier.weight(1f).height(20.dp * unit))
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(horizontal = margin).padding(bottom = pageHeight * 0.062f),
                verticalArrangement = Arrangement.spacedBy(12.dp * unit)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.5.dp, Color(0xFFFF72D8)),
                    colors = ButtonDefaults.buttonColors(containerColor = PopPink, contentColor = Color.White)
                ) {
                    Text("PLAY AGAIN ✦", fontSize = (20f * unit).sp, fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = onBackToMenu,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xB3061238)),
                    border = BorderStroke(1.4.dp, PopCyan)
                ) {
                    Text("BACK TO HOME", color = PopCyan, fontSize = (19f * unit).sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String, scale: Float) {
    Row(
        Modifier.fillMaxWidth().height(41.dp * scale),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFCFBEFF), fontSize = (14f * scale).sp,
            fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = Ivory, fontSize = (21f * scale).sp,
            fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun ResultDivider() {
    Box(Modifier.fillMaxWidth().height(0.7.dp).background(Color(0xFF555484)))
}

@Composable
private fun NeonDash(modifier: Modifier) {
    Canvas(modifier) {
        val lines = listOf(
            Triple(PopPink, Offset(0f, size.height * 0.27f), Offset(size.width, size.height * 0.27f)),
            Triple(PopCyan, Offset(size.width * 0.26f, size.height * 0.72f), Offset(size.width, size.height * 0.72f))
        )
        lines.forEach { (color, start, end) ->
            drawLine(color.copy(alpha = 0.18f), start, end, 7.dp.toPx(), StrokeCap.Round)
            drawLine(color, start, end, 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
