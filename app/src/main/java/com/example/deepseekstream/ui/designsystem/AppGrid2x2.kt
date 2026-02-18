package com.example.deepseekstream.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

@Composable
fun AppGrid2x2(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val normalized = (items + List((4 - items.size).coerceAtLeast(0)) { "" to "" }).take(4)
    var gridHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val spacingPx = with(density) { 8.dp.roundToPx() }
    val cellHeight = with(density) {
        if (gridHeightPx > 0) {
            ((gridHeightPx - spacingPx) / 2).coerceAtLeast(1).toDp()
        } else {
            180.dp
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { gridHeightPx = it.height },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(normalized) { item ->
            AppCard(modifier = Modifier.height(cellHeight)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppSectionTitle(item.first)
                    OutputScrollableText(text = item.second)
                }
            }
        }
    }
}

@Composable
private fun OutputScrollableText(text: String) {
    val scrollState = rememberScrollState()

    LaunchedEffect(text) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 2.dp)
            .heightIn(min = 64.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = text)
    }
}
