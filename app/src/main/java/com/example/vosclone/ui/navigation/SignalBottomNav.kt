package com.example.vosclone.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosclone.ui.theme.Ink
import com.example.vosclone.ui.theme.InkElevated
import com.example.vosclone.ui.theme.Ivory
import com.example.vosclone.ui.theme.IvoryMuted
import com.example.vosclone.ui.theme.PopCyan
import com.example.vosclone.ui.theme.PopPink

enum class RootDestination(val label: String, val code: String) {
    // The first destination is the player's home surface; SETLIST is retained
    // as the route name so existing navigation and deep links remain intact.
    SETLIST("HOME", "01"),
    SHOP("SHOP", "02"),
    PROFILE("PROFILE", "03")
}

/** A floating concert-console navigation pill with a clear active state. */
@Composable
fun SignalBottomNav(
    selected: RootDestination,
    onSelect: (RootDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF061126))
            .navigationBarsPadding()
            .border(0.5.dp, PopCyan.copy(alpha = 0.28f), RoundedCornerShape(0.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        RootDestination.entries.forEach { destination ->
            val active = destination == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(destination) }
                    .background(if (active) PopPink.copy(alpha = 0.06f) else Color.Transparent)
                    // A generous hit target keeps the three primary destinations
                    // comfortable to tap without changing the visual pill.
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(20.dp)) {
                        val tint = if (active) PopPink else IvoryMuted
                        val stroke = Stroke(width = 1.4.dp.toPx())
                        val w = size.width
                        val h = size.height
                        when (destination) {
                            RootDestination.SETLIST -> {
                                val house = Path().apply {
                                    moveTo(w * .08f, h * .45f)
                                    lineTo(w * .5f, h * .08f)
                                    lineTo(w * .92f, h * .45f)
                                    moveTo(w * .2f, h * .35f)
                                    lineTo(w * .2f, h * .87f)
                                    lineTo(w * .8f, h * .87f)
                                    lineTo(w * .8f, h * .35f)
                                }
                                drawPath(house, tint, style = stroke)
                            }
                            RootDestination.SHOP -> {
                                val diamond = Path().apply {
                                    moveTo(w * .5f, h * .1f)
                                    lineTo(w * .75f, h * .5f)
                                    lineTo(w * .5f, h * .9f)
                                    lineTo(w * .25f, h * .5f)
                                    close()
                                }
                                drawPath(diamond, tint, style = stroke)
                            }
                            RootDestination.PROFILE -> {
                                drawCircle(tint, w * .19f, Offset(w * .5f, h * .27f), style = stroke)
                                val shoulders = Path().apply {
                                    moveTo(w * .13f, h * .89f)
                                    cubicTo(w * .13f, h * .42f, w * .87f, h * .42f, w * .87f, h * .89f)
                                    close()
                                }
                                drawPath(shoulders, tint, style = stroke)
                            }
                        }
                    }
                    Text(
                        text = destination.label,
                        color = if (active) PopPink else IvoryMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    if (active) {
                        Box(modifier = Modifier.padding(top = 3.dp).fillMaxWidth(0.56f).height(2.dp).clip(RoundedCornerShape(3.dp)).background(PopPink))
                    }
                }
            }
        }
    }
}
