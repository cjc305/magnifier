package com.example.magnifier.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.magnifier.ui.theme.LocalSpacing

private const val GITHUB_URL  = "https://github.com/cjc305/magnifier"
private const val PRIVACY_URL = "https://cjc305.github.io/magnifier/privacy.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val spacing = LocalSpacing.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val versionName = remember(context) {
        try {
            @Suppress("DEPRECATION")
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "—"
        } catch (_: PackageManager.NameNotFoundException) {
            "—"
        }
    }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = "關於 數位放大鏡",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = spacing.sm),
            )
            Text(
                text = "Magnifier · 版本 $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "把手機鏡頭變成放大鏡。完全離線運作,不收集個資,程式碼公開。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = spacing.xs),
            )

            Spacer(Modifier.height(spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LinkRow(
                label = "原始碼 / Source code",
                url = GITHUB_URL,
                onClick = ::openUrl,
            )
            LinkRow(
                label = "隱私權政策 / Privacy policy",
                url = PRIVACY_URL,
                onClick = ::openUrl,
            )

            Spacer(Modifier.height(spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "開源元件 / Open Source",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = spacing.sm),
            )
            Attribution("Space Grotesk", "SIL Open Font License 1.1")
            Attribution("Jetpack Compose / Material 3", "Apache License 2.0")
            Attribution("CameraX", "Apache License 2.0")
            Attribution("Coil", "Apache License 2.0")
            Attribution("Kotlin Coroutines", "Apache License 2.0")

            Spacer(Modifier.height(spacing.md))
        }
    }
}

@Composable
private fun LinkRow(
    label: String,
    url: String,
    onClick: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(url) }
            .padding(vertical = spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Attribution(name: String, license: String) {
    val spacing = LocalSpacing.current
    Column(modifier = Modifier.padding(vertical = spacing.xs)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = license,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
