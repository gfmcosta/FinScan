package pt.ipt.dama2026.finscan.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.models.ReceiptResponse
import pt.ipt.dama2026.finscan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receipt: ReceiptResponse,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var locationLabel by remember { mutableStateOf<String?>(null) }

    val hasLocation = receipt.latitude != null && receipt.longitude != null

    LaunchedEffect(receipt.id) {
        if (hasLocation) {
            val lat = receipt.latitude!!
            val lng = receipt.longitude!!
            val fallback = String.format("%.4f, %.4f", lat, lng)
            try {
                withContext(Dispatchers.IO) {
                    if (Geocoder.isPresent()) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            val parts = listOfNotNull(addr.locality, addr.countryName)
                            locationLabel = if (parts.isNotEmpty()) parts.joinToString(", ") else fallback
                        } else {
                            locationLabel = fallback
                        }
                    } else {
                        locationLabel = fallback
                    }
                }
            } catch (_: Exception) {
                locationLabel = fallback
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.receipt_detail_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DetailRow(
                        icon = Icons.Default.Store,
                        label = stringResource(R.string.receipt_detail_store),
                        value = receipt.store
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                    DetailRow(
                        icon = Icons.Default.Category,
                        label = stringResource(R.string.receipt_detail_category),
                        value = receipt.categoryName
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                    DetailRow(
                        icon = Icons.Default.Euro,
                        label = stringResource(R.string.receipt_detail_total),
                        value = String.format("%.2f\u00A0\u20AC", receipt.total)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = stringResource(R.string.receipt_detail_date),
                        value = formatDetailDate(receipt.purchaseDate)
                    )

                    if (hasLocation) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))

                        DetailRow(
                            icon = Icons.Default.LocationOn,
                            label = stringResource(R.string.receipt_detail_location),
                            value = locationLabel ?: "…"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(IndigoTechnological.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IndigoTechnological,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = getAdaptiveSubtext()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDetailDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateStr.substringBefore("+").substringBefore("."))
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr.take(10)
    }
}
