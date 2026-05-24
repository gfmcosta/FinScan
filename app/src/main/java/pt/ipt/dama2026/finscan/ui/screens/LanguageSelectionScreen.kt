package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.datastore.SettingsManager
import pt.ipt.dama2026.finscan.ui.theme.*

@Composable
fun LanguageSelectionScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentLanguage by settingsManager.language.collectAsState(initial = "pt")
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
                tint = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.settings_language_label),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
        }

        // Language options with better spacing
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LanguageItem(
                name = stringResource(R.string.language_portuguese_label),
                code = "pt",
                flag = "🇵🇹",
                isSelected = currentLanguage == "pt",
                onClick = {
                    scope.launch {
                        settingsManager.setLanguage("pt")
                        // Small delay to ensure DataStore update is processed
                        kotlinx.coroutines.delay(100)
                    }
                }
            )
            LanguageItem(
                name = stringResource(R.string.language_english_label),
                code = "en",
                flag = "🇬🇧",
                isSelected = currentLanguage == "en",
                onClick = {
                    scope.launch {
                        settingsManager.setLanguage("en")
                        // Small delay to ensure DataStore update is processed
                        kotlinx.coroutines.delay(100)
                    }
                }
            )
        }
    }
}

@Composable
fun LanguageItem(
    name: String,
    code: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) IndigoTechnological else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Flag in circular container with gradient background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                IndigoTechnological.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = flag,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = code.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = getAdaptiveSubtext()
                    )
                }
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectionScreenPreview() {
    FinScanTheme(darkTheme = false) {
        LanguageSelectionScreen()
    }
}
