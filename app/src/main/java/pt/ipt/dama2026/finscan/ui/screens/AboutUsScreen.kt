package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.ui.theme.*

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    icon: @Composable () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme()) Color(0xFF1E293B) else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                icon()
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = IndigoTechnological)
            }
            content()
        }
    }
}

@Composable
private fun AcademicRow(label: String, value: String) {
    if (value.isEmpty()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 3.dp))
    } else {
        Row(modifier = Modifier.padding(vertical = 3.dp)) {
            Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = getAdaptiveSubtext())
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun LibraryRow(name: String, description: String, source: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodySmall, color = getAdaptiveSubtext())
        Text(source, style = MaterialTheme.typography.labelSmall, color = IndigoTechnological)
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AboutUsScreen(onBack: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Top Bar with Back Button (Consistent with Language Screen)
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
                text = stringResource(R.string.settings_about_us_label),
                style = MaterialTheme.typography.headlineMedium,
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
                .verticalScroll(rememberScrollState()),
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

            // ── Academic Information ──────────────────────────────────────────
            SectionCard(
                icon = { Icon(Icons.Default.School, contentDescription = null, tint = IndigoTechnological, modifier = Modifier.size(20.dp)) },
                title = stringResource(R.string.about_us_academic_label)
            ) {
                AcademicRow(label = stringResource(R.string.about_us_label_course), value = stringResource(R.string.about_us_course))
                AcademicRow(label = stringResource(R.string.about_us_label_uc), value = stringResource(R.string.about_us_uc))
                AcademicRow(label = stringResource(R.string.about_us_label_year), value = stringResource(R.string.about_us_academic_year))
                AcademicRow(label = stringResource(R.string.about_us_label_author), value = stringResource(R.string.about_us_student_id))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Libraries & Frameworks ────────────────────────────────────────
            SectionCard(
                icon = { Icon(Icons.Default.Code, contentDescription = null, tint = IndigoTechnological, modifier = Modifier.size(20.dp)) },
                title = stringResource(R.string.about_us_libraries_label)
            ) {
                val libs = listOf(
                    Triple("Jetpack Compose",  "Android UI toolkit",             "developer.android.com/jetpack/compose"),
                    Triple("Retrofit",         "HTTP client for Android",        "square.github.io/retrofit"),
                    Triple("OkHttp",           "HTTP + WebSocket client",        "square.github.io/okhttp"),
                    Triple("Gson",             "JSON serialization",             "github.com/google/gson"),
                    Triple("Coil",             "Image loading for Compose",      "coil-kt.github.io/coil"),
                    Triple("DataStore",        "Preferences & data persistence", "developer.android.com/jetpack/androidx/releases/datastore"),
                    Triple("CameraX",          "Camera API",                     "developer.android.com/jetpack/androidx/releases/camera"),
                )
                libs.forEach { (name, desc, source) ->
                    LibraryRow(name = name, description = desc, source = source)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
