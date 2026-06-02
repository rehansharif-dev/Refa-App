package com.example.transportapp.presentation.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Central responsive layout helper.
 *
 * Phone  : screenWidth < 600 dp
 * Tablet : screenWidth >= 600 dp
 *
 * Usage inside any Composable:
 *   val r = rememberResponsive()
 *   Modifier.padding(horizontal = r.hPad)
 *   Modifier.then(r.maxWidthModifier)
 */
data class Responsive(
    val isTablet: Boolean,
    val isLandscape: Boolean,
    /** Horizontal padding for screen edges */
    val hPad: Dp,
    /** Maximum content width — use with widthIn(max = r.maxContent) */
    val maxContent: Dp,
    /** Vertical spacing between cards */
    val cardGap: Dp,
    /** Standard button height */
    val btnHeight: Dp,
    /** Avatar / profile picture size */
    val avatarSize: Dp,
    /** Screen-edge card padding (inside cards) */
    val cardPad: Dp,
    /** Title font size */
    val titleSp: TextUnit,
    /** Body font size */
    val bodySp: TextUnit,
    /** Caption / label font size */
    val labelSp: TextUnit,
    /** Grid column count for list screens */
    val gridColumns: Int,
)

@Composable
@ReadOnlyComposable
fun rememberResponsive(): Responsive {
    val config = LocalConfiguration.current
    val w = config.screenWidthDp
    val landscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val tablet = w >= 600

    return Responsive(
        isTablet     = tablet,
        isLandscape  = landscape,
        hPad         = if (tablet) 40.dp   else 20.dp,
        maxContent   = if (tablet) 720.dp  else 9999.dp, // 9999 = no practical limit on phone
        cardGap      = if (tablet) 20.dp   else 14.dp,
        btnHeight    = if (tablet) 60.dp   else 52.dp,
        avatarSize   = if (tablet) 120.dp  else 96.dp,
        cardPad      = if (tablet) 22.dp   else 16.dp,
        titleSp      = if (tablet) 26.sp   else 20.sp,
        bodySp       = if (tablet) 16.sp   else 14.sp,
        labelSp      = if (tablet) 13.sp   else 11.sp,
        gridColumns  = if (tablet) 2       else 1,
    )
}
