package com.example.vosclone.ui.results

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosclone.engine.GameSession
import com.example.vosclone.ui.theme.Ink
import com.example.vosclone.ui.theme.InkElevated
import com.example.vosclone.ui.theme.InkElevated2
import com.example.vosclone.ui.theme.Ivory
import com.example.vosclone.ui.theme.IvoryMuted
import com.example.vosclone.ui.theme.GradeLetter as GradeLetterStyle
import com.example.vosclone.ui.theme.PopCyan
import com.example.vosclone.ui.theme.PopLavender
import com.example.vosclone.ui.theme.PopPink
import com.example.vosclone.ui.theme.PopYellow
import com.example.vosclone.ui.theme.SignalOrange

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
    val accuracy = session.accuracyPercent
    val grade = gradeFor(accuracy)

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF111E56), Color(0xFF080F30), Ink),
                    startY = 0f,
                    endY = size.height
                )
            )
            drawCircle(
                color = PopPink.copy(alpha = 0.17f),
                radius = size.width * 0.64f,
                center = Offset(size.width * 1.02f, size.height * 0.08f)
            )
            drawCircle(PopCyan.copy(alpha = 0.11f), size.width * 0.58f, Offset(size.width * -0.04f, size.height * 0.68f))
            drawCircle(PopLavender.copy(alpha = 0.08f), size.width * 0.55f, Offset(size.width * 0.86f, size.height * 0.85f))
            for (index in 0..4) {
                drawLine(
                    color = if (index % 2 == 0) PopCyan.copy(alpha = 0.16f) else PopPink.copy(alpha = 0.16f),
                    start = Offset(size.width * (index * 0.25f - 0.1f), 0f),
                    end = Offset(size.width * (0.2f + index * 0.16f), size.height * 0.66f),
                    strokeWidth = size.width * 0.028f
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SESSION COMPLETE", color = Ivory, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
                Text("SIGNAL // OFF", color = PopYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(displayChartTitle(session.chart.title), color = IvoryMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(PopPink, PopYellow)))
                    .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 30.dp, vertical = 8.dp)
            ) {
                Text(grade, style = GradeLetterStyle, color = Ink)
            }
            Spacer(modifier = Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xC91B2D68), Color(0xDD0C1740))))
                    .border(1.dp, PopCyan.copy(alpha = 0.34f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                StatRow("SCORE", "${session.score}")
                StatRow("ACCURACY", "$accuracy%")
                StatRow("MAX COMBO", "${session.maxCombo}")
                StatRow("PERFECT / GOOD / MISS", "${session.perfectCount} / ${session.goodCount} / ${session.missCount}")
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onPlayAgain,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PopPink, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("PLAY AGAIN  ✦", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBackToMenu,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PopCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("BACK TO HOME", color = PopCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = IvoryMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Ivory, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}
