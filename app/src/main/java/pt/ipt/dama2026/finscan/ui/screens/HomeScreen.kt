package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipt.dama2026.finscan.R
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
        // Use AnimatedContent for smoother transitions between screens
        Box(modifier = Modifier.padding(paddingValues)) {
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
                    2 -> ScanScreen()
                    3 -> ReportsScreen()
                    4 -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun ReceiptsScreen() {
    PlaceholderScreen("Receipts Screen")
}

@Composable
fun ScanScreen() {
    PlaceholderScreen("Scan Screen")
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit = {}) {
    val userName = "Costa" // TODO: Change in PROD
    val currentDate = remember { 
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) 
    }
    val monthlySpent = "450,25" //TODO: Change in PROD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    text = stringResource(R.string.home_hi_title) + userName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = currentDate,
                    fontSize = 14.sp,
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
                Column {
                    Text(
                        text = stringResource(R.string.home_monthly_spent_title),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$monthlySpent€",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Chart Section
        Text(
            text = stringResource(R.string.home_category_spent_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Simple Bar Chart using Canvas
                val chartData = listOf(
                    0.6f to EmeraldGreen,
                    0.4f to IndigoTechnological,
                    0.8f to AmberAlert,
                    0.3f to Color.Magenta,
                    0.5f to LightBlue
                )
                BarChartPlaceholder(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Top 5 most spent categories
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryLegendItem("Entertainment", EmeraldGreen)
                    CategoryLegendItem("Restaurant", IndigoTechnological)
                    CategoryLegendItem("University", AmberAlert)
                    CategoryLegendItem("Car - Gasoline", Color.Magenta)
                    CategoryLegendItem("Car - Tolls", LightBlue)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FinScanTheme {
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
                    fontSize = 10.sp,
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
fun CategoryLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = getAdaptiveSubtext(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
