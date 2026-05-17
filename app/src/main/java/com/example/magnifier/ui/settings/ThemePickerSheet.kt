package com.example.magnifier.ui.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.magnifier.ui.theme.LocalSpacing
import com.example.magnifier.ui.theme.ThemeSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    current: ThemeSpec,
    onSelect: (ThemeSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        ) {
            Text(
                text = "選擇主題",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = spacing.sm),
            )
            Text(
                text = "從 mreminder 共用設計語言挑選",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.md),
            )
            ThemeSpec.All.forEach { spec ->
                ThemeOptionRow(
                    spec = spec,
                    selected = spec.id == current.id,
                    onClick = {
                        onSelect(spec)
                        onDismiss()
                    },
                )
            }

            AboutBlock()
        }
    }
}

@Composable
private fun AboutBlock() {
    val spacing = LocalSpacing.current
    HorizontalDivider(
        modifier = Modifier.padding(vertical = spacing.md),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
    Text(
        text = "關於",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = spacing.sm),
    )
    AboutRow(label = "版本", value = "1.0")
    AboutRow(
        label = "原始碼",
        value = "github.com/cjc305/magnifier",
        url = "https://github.com/cjc305/magnifier",
    )
    AboutRow(
        label = "隱私權政策",
        value = "查看線上版本",
        url = "https://cjc305.github.io/magnifier/privacy.html",
    )
    AboutRow(
        label = "字體",
        value = "Space Grotesk · OFL",
        url = "https://fonts.google.com/specimen/Space+Grotesk",
    )
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    url: String? = null,
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val clickModifier = if (url != null) {
        Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    } else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (url != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ThemeOptionRow(
    spec: ThemeSpec,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md)
            .semantics {
                contentDescription =
                    "${spec.displayName} ${spec.sublabel} " +
                            (if (selected) "已選擇" else "未選擇")
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        SwatchTriplet(
            bg = spec.palette.bg,
            surface = spec.palette.surfaceContainer,
            primary = spec.palette.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = spec.sublabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SwatchTriplet(
    bg: Color,
    surface: Color,
    primary: Color,
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        ColorDot(bg, borderColor)
        ColorDot(surface, borderColor)
        ColorDot(primary, borderColor)
    }
}

@Composable
private fun ColorDot(color: Color, borderColor: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = 1.dp, color = borderColor, shape = CircleShape),
    )
}
