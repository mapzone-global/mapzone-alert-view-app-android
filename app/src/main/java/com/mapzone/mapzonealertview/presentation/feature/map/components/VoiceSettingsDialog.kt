package com.mapzone.mapzonealertview.presentation.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vietmap.alert_view_sdk.VoiceAlertType

private fun VoiceAlertType.label(): String = when (this) {
    VoiceAlertType.SPEED_CAMERA -> "Camera tốc độ"
    VoiceAlertType.TOLL -> "Trạm thu phí"
    VoiceAlertType.TRAFFIC_ENFORCEMENT_CAMERA -> "Camera xử phạt giao thông"
    VoiceAlertType.RED_LIGHT_CAMERA -> "Camera đèn đỏ"
    VoiceAlertType.AI_CAMERA -> "Camera AI"
    VoiceAlertType.NO_LEFT_TURN -> "Cấm rẽ trái"
    VoiceAlertType.NO_RIGHT_TURN -> "Cấm rẽ phải"
    VoiceAlertType.NO_UTURN -> "Cấm quay đầu"
    VoiceAlertType.NO_OVERTAKING -> "Cấm vượt"
    VoiceAlertType.NO_OVERTAKING_END -> "Hết cấm vượt"
    VoiceAlertType.NO_PARKING -> "Cấm đỗ xe"
    VoiceAlertType.NO_STRAIGHT -> "Cấm đi thẳng"
    VoiceAlertType.BUILDUP_AREA_START -> "Khu đông dân cư"
    VoiceAlertType.BUILDUP_AREA_END -> "Hết khu đông dân cư"
    VoiceAlertType.REST_STATION -> "Trạm dừng nghỉ"
}

@Composable
fun VoiceSettingsDialog(
    voiceEnabled: Boolean,
    mutedTypes: Set<VoiceAlertType>,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onMutedTypesChange: (Set<VoiceAlertType>) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Cảnh báo giọng nói",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bật giọng nói", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Tắt để im lặng toàn bộ (kể cả cảnh báo tốc độ)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = voiceEnabled, onCheckedChange = onVoiceEnabledChange)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Chọn loại cảnh báo muốn nghe",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                VoiceAlertType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            type.label(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (voiceEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = type !in mutedTypes,
                            enabled = voiceEnabled,
                            onCheckedChange = { on ->
                                val next = mutedTypes.toMutableSet()
                                if (on) next.remove(type) else next.add(type)
                                onMutedTypesChange(next)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Xong", fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
