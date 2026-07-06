package com.mapzone.mapzonealertview.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import com.mapzone.mapzonealertview.domain.model.VehicleProfile
import com.mapzone.mapzonealertview.domain.model.VehicleType

@Composable
fun VehicleProfileDialog(
    current: VehicleProfile,
    onDismiss: () -> Unit,
    onConfirm: (VehicleProfile) -> Unit,
) {
    var selectedType by remember { mutableStateOf(current.type) }
    var seats by remember { mutableStateOf(current.seats.toString()) }
    var weight by remember { mutableStateOf(current.weightKg.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Column {
                Text(
                    "Cấu hình phương tiện",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "Dùng để tính route và cảnh báo tốc độ theo loại xe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionLabel("Loại xe")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VehicleType.entries.forEach { type ->
                        VehicleCard(
                            type = type,
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                SectionLabel("Thông số")
                OutlinedTextField(
                    value = seats,
                    onValueChange = { seats = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Số chỗ ngồi") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Trọng tải (kg)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        VehicleProfile(
                            type = selectedType,
                            seats = seats.toIntOrNull() ?: current.seats,
                            weightKg = weight.toIntOrNull() ?: current.weightKg,
                        )
                    )
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Lưu", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Huỷ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun VehicleCard(
    type: VehicleType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector = when (type) {
        VehicleType.CAR -> Icons.Default.DirectionsCar
        VehicleType.MOTORCYCLE -> Icons.Default.TwoWheeler
        VehicleType.TRUCK -> Icons.Default.LocalShipping
        VehicleType.BIKE -> Icons.AutoMirrored.Filled.DirectionsBike
    }
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
        Text(
            type.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
