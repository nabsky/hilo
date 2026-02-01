package com.zorindisplays.display.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage

@Composable
fun CardSvg(
    assetFileName: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = "file:///android_asset/cards/$assetFileName",
        contentDescription = null,
        modifier = modifier
    )
}
