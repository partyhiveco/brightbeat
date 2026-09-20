package com.example.vosclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vosclone.R
import com.example.vosclone.ui.theme.Ivory

/**
 * Small, local-drawn action marks keep commerce/profile controls in the same
 * angular neon language as the BrightBeat logo without adding an icon library.
 */
enum class BrightBeatIcon {
    Pass,
    Pack,
    Spark,
    Rewards,
    Restore,
    Manage,
    Calibrate
}

enum class BrightBeatLogoVariant {
    Brand,
    Shop,
    Profile
}

/**
 * Renders a BrightBeat lockup at the available width while preserving the
 * source artwork's proportions. Keeping this in one component prevents the
 * single-line Home/Profile marks and the two-line Shop mark from drifting in
 * scale as each screen evolves independently.
 */
@Composable
fun BrightBeatLogo(
    variant: BrightBeatLogoVariant,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val (asset, aspectRatio) = when (variant) {
        BrightBeatLogoVariant.Brand -> R.drawable.brightbeat_logo_v2 to 3.0f
        BrightBeatLogoVariant.Shop -> R.drawable.brightbeat_shop_lockup_v2 to (1937f / 812f)
        BrightBeatLogoVariant.Profile -> R.drawable.brightbeat_profile_lockup to 3.0f
    }
    // Shop's raster has a tighter transparent margin than the single-line
    // lockups. A small width correction makes the visible neon artwork match
    // Profile's displayed footprint while keeping the source proportions.
    val visibleWidthFraction = if (variant == BrightBeatLogoVariant.Shop) 0.97f else 1f
    Image(
        painter = painterResource(asset),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth(visibleWidthFraction)
            .aspectRatio(aspectRatio)
    )
}

@Composable
fun BrightBeatScreenHeader(
    title: String,
    meta: String,
    metaColor: Color,
    modifier: Modifier = Modifier
) {
    val logoVariant = if (title.endsWith("SHOP")) {
        BrightBeatLogoVariant.Shop
    } else {
        BrightBeatLogoVariant.Profile
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            BrightBeatLogo(
                variant = logoVariant,
                contentDescription = title
            )
            Text(
                text = meta,
                color = metaColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xA8050A2A))
                    .border(1.dp, metaColor.copy(alpha = 0.82f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun BrightBeatActionLabel(
    icon: BrightBeatIcon,
    label: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrightBeatActionIcon(icon = icon, tint = contentColor, modifier = Modifier.size(28.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.35.sp,
                maxLines = 1,
                softWrap = false
            )
        }
        BrightBeatChevron(contentColor)
    }
}

@Composable
fun BrightBeatActionIcon(
    icon: BrightBeatIcon,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(22.dp)) {
        drawBrightBeatIcon(icon = icon, tint = tint)
    }
}

private fun DrawScope.drawBrightBeatIcon(icon: BrightBeatIcon, tint: Color) {
    val stroke = 2.1.dp.toPx()
    val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val center = Offset(size.width / 2f, size.height / 2f)

    when (icon) {
        BrightBeatIcon.Pass -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(2.dp.toPx(), 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 14.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = outline
            )
            drawLine(tint, Offset(6.dp.toPx(), 8.dp.toPx()), Offset(16.dp.toPx(), 8.dp.toPx()), stroke)
            drawLine(tint, Offset(6.dp.toPx(), 12.dp.toPx()), Offset(12.dp.toPx(), 12.dp.toPx()), stroke)
        }

        BrightBeatIcon.Pack -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 16.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = outline
            )
            drawLine(tint, Offset(3.dp.toPx(), 8.dp.toPx()), Offset(19.dp.toPx(), 8.dp.toPx()), stroke)
            drawLine(tint, Offset(8.dp.toPx(), 3.dp.toPx()), Offset(8.dp.toPx(), 19.dp.toPx()), stroke)
        }

        BrightBeatIcon.Spark -> {
            val spark = Path().apply {
                moveTo(center.x, 1.5.dp.toPx())
                lineTo(center.x + 3.dp.toPx(), center.y - 3.dp.toPx())
                lineTo(size.width - 1.5.dp.toPx(), center.y)
                lineTo(center.x + 3.dp.toPx(), center.y + 3.dp.toPx())
                lineTo(center.x, size.height - 1.5.dp.toPx())
                lineTo(center.x - 3.dp.toPx(), center.y + 3.dp.toPx())
                lineTo(1.5.dp.toPx(), center.y)
                lineTo(center.x - 3.dp.toPx(), center.y - 3.dp.toPx())
                close()
            }
            drawPath(spark, tint, style = outline)
            drawCircle(tint, radius = 1.3.dp.toPx(), center = center)
        }

        BrightBeatIcon.Rewards -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(4.dp.toPx(), 3.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 11.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = outline
            )
            drawLine(tint, Offset(8.dp.toPx(), 14.dp.toPx()), Offset(8.dp.toPx(), 19.dp.toPx()), stroke)
            drawLine(tint, Offset(14.dp.toPx(), 14.dp.toPx()), Offset(14.dp.toPx(), 19.dp.toPx()), stroke)
            drawLine(tint, Offset(6.dp.toPx(), 19.dp.toPx()), Offset(16.dp.toPx(), 19.dp.toPx()), stroke)
            drawLine(tint, Offset(11.dp.toPx(), 5.dp.toPx()), Offset(11.dp.toPx(), 12.dp.toPx()), stroke)
        }

        BrightBeatIcon.Restore -> {
            drawArc(
                color = tint,
                startAngle = 38f,
                sweepAngle = 286f,
                useCenter = false,
                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 16.dp.toPx()),
                style = outline
            )
            val arrow = Path().apply {
                moveTo(4.dp.toPx(), 4.dp.toPx())
                lineTo(4.dp.toPx(), 10.dp.toPx())
                lineTo(10.dp.toPx(), 7.dp.toPx())
            }
            drawPath(arrow, tint, style = outline)
        }

        BrightBeatIcon.Manage -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(2.dp.toPx(), 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 14.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = outline
            )
            drawLine(tint, Offset(2.dp.toPx(), 8.dp.toPx()), Offset(20.dp.toPx(), 8.dp.toPx()), stroke)
            drawCircle(tint, radius = 1.3.dp.toPx(), center = Offset(6.dp.toPx(), 13.dp.toPx()))
            drawCircle(tint, radius = 1.3.dp.toPx(), center = Offset(11.dp.toPx(), 13.dp.toPx()))
        }

        BrightBeatIcon.Calibrate -> {
            drawLine(tint, Offset(3.dp.toPx(), 18.dp.toPx()), Offset(11.dp.toPx(), 3.dp.toPx()), stroke)
            drawLine(tint, Offset(19.dp.toPx(), 18.dp.toPx()), Offset(11.dp.toPx(), 3.dp.toPx()), stroke)
            drawLine(tint, Offset(6.dp.toPx(), 14.dp.toPx()), Offset(16.dp.toPx(), 14.dp.toPx()), stroke)
            drawLine(tint, Offset(11.dp.toPx(), 3.dp.toPx()), Offset(11.dp.toPx(), 10.dp.toPx()), stroke)
            drawCircle(tint, radius = 1.2.dp.toPx(), center = Offset(11.dp.toPx(), 3.dp.toPx()))
        }
    }
}

@Composable
private fun BrightBeatChevron(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(5.dp.toPx(), 2.dp.toPx())
            lineTo(13.dp.toPx(), size.height / 2f)
            lineTo(5.dp.toPx(), size.height - 2.dp.toPx())
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
