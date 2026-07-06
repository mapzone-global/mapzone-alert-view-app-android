package com.mapzone.mapzonealertview.presentation.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BottomActionBar(
    isNavigating: Boolean,
    statusLine: String,
    routeSummary: String?,
    isReady: Boolean,
    statusText: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isNavigating) {
        IdleStartButton(onStart = onStart, modifier = modifier)
    } else {
        NavStatusBar(
            statusLine = statusLine,
            routeSummary = routeSummary,
            isReady = isReady,
            statusText = statusText,
            onStop = onStop,
            modifier = modifier,
        )
    }
}

@Composable
private fun IdleStartButton(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onStart,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Box(modifier = Modifier.size(8.dp))
        Text(
            "Bắt đầu navigation",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun NavStatusBar(
    statusLine: String,
    routeSummary: String?,
    isReady: Boolean,
    statusText: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    routeSummary ?: statusLine,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(text = statusText, ok = isReady)
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFC62828), CircleShape),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Kết thúc navigation",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, ok: Boolean) {
    val dot = if (ok) Color(0xFF2E7D32) else Color(0xFFEF6C00)
    Row(
        modifier = Modifier
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dot, CircleShape),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
