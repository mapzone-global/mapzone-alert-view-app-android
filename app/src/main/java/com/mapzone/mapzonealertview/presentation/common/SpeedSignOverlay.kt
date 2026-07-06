package com.mapzone.mapzonealertview.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.mapzone.mapzonealertview.presentation.controllers.SignSlot

@Composable
fun SpeedSignOverlay(
    current: SignSlot,
    next: SignSlot,
    camera: SignSlot,
    toll: SignSlot,
    modifier: Modifier = Modifier,
) {
    val hasAny = current.bitmap != null || next.bitmap != null ||
            camera.bitmap != null || toll.bitmap != null
    if (!hasAny) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SignTile(current, size = 76)
        SignTile(next, size = 50)
        SignTile(camera, size = 50)
        SignTile(toll, size = 50)
    }
}

@Composable
private fun SignTile(slot: SignSlot, size: Int) {
    val bmp = slot.bitmap ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                .padding(4.dp),
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size.dp),
            )
        }
        slot.distanceMeters?.let { d ->
            Text(
                if (d >= 1000) "%.1f km".format(d / 1000.0) else "$d m",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
