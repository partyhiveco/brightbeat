package com.example.vosclone.ui.navigation

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
            .background(InkElevated)
            .navigationBarsPadding()
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(29.dp))
            .border(1.dp, PopCyan.copy(alpha = 0.26f), RoundedCornerShape(29.dp)),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        RootDestination.entries.forEach { destination ->
            val active = destination == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(destination) }
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (active) PopPink.copy(alpha = 0.18f) else Color.Transparent)
                    // A generous hit target keeps the three primary destinations
                    // comfortable to tap without changing the visual pill.
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (destination) {
                            RootDestination.SETLIST -> "⌂"
                            RootDestination.SHOP -> "♢"
                            RootDestination.PROFILE -> "●"
                        },
                        color = if (active) PopPink else IvoryMuted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp
                    )
                    Text(
                        text = destination.label,
                        color = if (active) Ivory else IvoryMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    if (active) {
                        Box(modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.62f).height(3.dp).clip(RoundedCornerShape(3.dp)).background(PopPink))
                    }
                }
            }
        }
    }
}
