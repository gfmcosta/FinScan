package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.ExpenseStats
import pt.ipt.dama2026.finscan.data.api.services.ReceiptApiService
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import pt.ipt.dama2026.finscan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class to define the bottom navbar routes, icons and text.
 * @param route Defined route to a specific nav item
 * @param icon Defined icon to a specific nav item
 * @param labelRes: Defined text label to a specific nav item
 */
sealed class BottomNavItem(val route: String, val icon: ImageVector, val labelRes: Int) {
    object Home : BottomNavItem("home", Icons.Default.Home, R.string.home_label)
    object Receipts : BottomNavItem("receipts", Icons.Default.Receipt, R.string.receipts_label)
    object Scan : BottomNavItem("scan", Icons.Default.QrCodeScanner, R.string.scan_label)
    object Reports : BottomNavItem("reports", Icons.Default.Timeline, R.string.reports_label)
    object Settings : BottomNavItem("settings", Icons.Default.Settings, R.string.settings_label)
}


@Composable
fun MainScreen() {
    // object to control what item is selected (nav item)
    var selectedItem by remember { mutableIntStateOf(0) }
    
    // Remember items to avoid recreating list on every recomposition
    val items = remember {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Receipts,
            BottomNavItem.Scan,
            BottomNavItem.Reports,
            BottomNavItem.Settings,
        )
    }

    Scaffold(
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                ) {
                    items.forEachIndexed { index, item ->
                    val label = stringResource(id = item.labelRes)
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = selectedItem == index,
                        onClick = { 
                            if (selectedItem != index) {
                                selectedItem = index 
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoTechnological,
                            selectedTextColor = IndigoTechnological,
                            unselectedIconColor = HomeNavBarGrey,
                            unselectedTextColor = HomeNavBarGrey,
                            indicatorColor = IndigoTechnological.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // ScanScreen uses rememberLauncherForActivityResult which needs
            // LocalActivityResultRegistryOwner — keep it outside AnimatedContent
            // to avoid the sub-composition scope losing the owner.
            if (selectedItem == 2) {
                ScanScreen()
            } else {
                AnimatedContent(
                    targetState = selectedItem,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> HomeScreen(onNavigateToSettings = { selectedItem = 4 })
                        1 -> ReceiptsScreen()
                        3 -> ReportsScreen()
                        4 -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen() {
    PlaceholderScreen("Reports Screen")
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val userName by authManager.name.collectAsState(initial = "")
    
    // Obter apenas o primeiro nome
    val firstName = remember(userName) {
        userName?.trim()?.split("\\s+".toRegex())?.firstOrNull() ?: ""
    }

    val currentDate = remember { 
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) 
    }

    val api = remember { ApiClient.getRetrofit().create(ReceiptApiService::class.java) }
    var stats by remember { mutableStateOf<ExpenseStats?>(null) }
    var isLoadingStats by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val startOfMonth = startFmt.format(cal.time)
            cal.add(Calendar.MONTH, 1)
            val startOfNextMonth = startFmt.format(cal.time)
            val resp = api.getStats(startDate = startOfMonth, endDate = startOfNextMonth)
            if (resp.isSuccessful) {
                stats = resp.body()
            }
        } catch (_: Exception) {}
        isLoadingStats = false
    }

    val monthlyTotal = remember(stats) {
        stats?.byCategory?.sumOf { it.total } ?: 0.0
    }
    val monthlySpent = remember(monthlyTotal) {
        String.format("%.2f", monthlyTotal).replace(".", ",")
    }

    val topCategories = remember(stats) {
        stats?.byCategory?.sortedByDescending { it.total }?.take(5) ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_hi_title) + firstName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = getAdaptiveSubtext()
                )
            }
            // User Avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SettingsProfilePlaceholderColor)
                    .clickable { onNavigateToSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = SettingsIconTintColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Monthly Spent Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = IndigoTechnological)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(IndigoTechnological, HomeMonthlyCardGradientEnd)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_monthly_spent_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$monthlySpent€",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Chart Section
        Text(
            text = stringResource(R.string.home_category_spent_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (topCategories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = getAdaptiveSubtext()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.home_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = getAdaptiveSubtext()
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val chartColors = listOf(
                        EmeraldGreen,
                        IndigoTechnological,
                        AmberAlert,
                        Color(0xFFEC4899),
                        LightBlue
                    )

                    val maxTotal = remember(topCategories) {
                        topCategories.maxOfOrNull { it.total } ?: 1.0
                    }
                    val chartData = remember(topCategories, maxTotal) {
                        topCategories.mapIndexed { index, cat ->
                            (cat.total / maxTotal).toFloat() to chartColors[index % chartColors.size]
                        }
                    }

                    BarChartPlaceholder(
                        data = chartData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topCategories.forEachIndexed { index, cat ->
                            val amount = String.format("%.2f", cat.total).replace(".", ",")
                            CategoryLegendItem(cat.key, amount, chartColors[index % chartColors.size])
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FinScanTheme(darkTheme = false) {
        MainScreen()
    }
}

@Composable
fun BarChartPlaceholder(
    data: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier
) {
    val labels = listOf("100%", "75%", "50%", "25%", "0%")

    Row(modifier = modifier) {
        // Y-Axis Labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = getAdaptiveSubtext()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Bars
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            val barCount = data.size
            val barWidth = width / (barCount * 1.5f)
            val spacing = if (barCount > 1) (width - (barCount * barWidth)) / (barCount - 1) else 0f

            data.forEachIndexed { index, (value, color) ->
                val x = index * (barWidth + spacing)
                val barHeight = value * height * 0.9f // Padding top

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.7f))
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(x, height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun CategoryLegendItem(label: String, amount: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ($amount€)",
            style = MaterialTheme.typography.bodySmall,
            color = getAdaptiveSubtext(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
