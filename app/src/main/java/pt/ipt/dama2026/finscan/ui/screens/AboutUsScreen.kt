package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.ui.theme.*

@Composable
fun AboutUsScreen(onBack: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = stringResource(R.string.settings_about_us_label),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Name
            Text(
                text = "FinScan",
                style = MaterialTheme.typography.headlineLarge,
                color = IndigoTechnological,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // App Description
            Text(
                text = stringResource(R.string.about_us_description),
                style = MaterialTheme.typography.bodyMedium,
                color = getAdaptiveSubtext(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Spacer
            Spacer(modifier = Modifier.height(20.dp))

            // Author Section
            Text(
                text = stringResource(R.string.about_us_author_label),
                style = MaterialTheme.typography.labelLarge,
                color = getAdaptiveSubtext(),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Author Avatar (Circular)
            Image(
                painter = painterResource(id = R.drawable.costa),
                contentDescription = stringResource(R.string.about_us_author_name),
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Author Name Placeholder
            Text(
                text = stringResource(R.string.about_us_author_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Institution Section
            Text(
                text = stringResource(R.string.about_us_institution_label),
                style = MaterialTheme.typography.labelLarge,
                color = getAdaptiveSubtext(),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.about_us_institution_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // IPT Logo (Circular)
            Image(
                painter = painterResource(id = R.drawable.ipt),
                contentDescription = stringResource(R.string.about_us_institution_name),
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Version Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme()) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.about_us_version_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = getAdaptiveSubtext()
                    )
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutUsScreenPreview() {
    FinScanTheme(darkTheme = false) {
        AboutUsScreen()
    }
}